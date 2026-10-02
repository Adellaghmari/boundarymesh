import { appendFileSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { assertEvidenceVersionAbsent, publishEvidence } from "./artifacts.mjs";
import {
    canonicalJson,
    evidenceVersion,
    loadFrozenRelease,
    RUN_ID,
    sha256Bytes,
    validateDeploymentEvidence,
    validateRecoveryDispatchInputs,
    verifyEvidenceFiles,
    writeDeploymentEvidence,
} from "./contract.mjs";
import {
    assertPromotedHostingObservation,
    assertPromotedRuntimeObservation,
    assertPromotionRecoveryPreconditions,
    buildRecoveredPromotionEvidence,
} from "./evidence.mjs";

function readJson(path) {
    return JSON.parse(readFileSync(path, "utf8"));
}

function option(name) {
    const index = process.argv.indexOf(name);
    return index >= 0 ? process.argv[index + 1] : undefined;
}

function flagBoolean(name) {
    const raw = option(name);
    if (raw === "true") return true;
    if (raw === "false") return false;
    throw new Error(`${name} must be exactly true or false.`);
}

export const HOSTING_HANDOFF_SCHEMA = "boundarymesh.recovery-hosting-handoff/v1";
export const HOSTING_RECOVERY_PRINCIPAL = "boundarymesh-hosting-deploy@boundarymesh-dev.iam.gserviceaccount.com";
export const HOSTING_HANDOFF_WORKFLOW_PATH = ".github/workflows/recover-promotion-evidence.yml";
export const MAX_HOSTING_HANDOFF_BYTES = 16 * 1024;
export const FAILED_RECOVERY_RUN_1 = "37003377107";

export function isRecoveryRun1HostingQuotaDenial(error) {
    const diagnostic = error?.diagnostic ?? error;
    const quotaMessage = diagnostic?.error?.message ?? diagnostic?.message ?? "";
    const reasons = Array.isArray(diagnostic?.errorInfo) ? diagnostic.errorInfo : [];
    return error?.name === "FirebaseHostingRequestError"
        && Number(diagnostic?.httpStatus) === 403
        && /serviceusage\.services\.use/.test(quotaMessage)
        && reasons.some((entry) => (
            entry?.reason === "USER_PROJECT_DENIED"
            && entry?.metadata?.service === "firebasehosting.googleapis.com"
            && entry?.metadata?.consumer === "projects/boundarymesh-dev"
        ));
}

export function buildHostingRecoveryHandoff({ snapshot, firebaseStatus, recoveryRunId }) {
    if (!RUN_ID.test(String(recoveryRunId))) throw new Error("Hosting recovery handoff requires this recovery run ID.");
    if (String(recoveryRunId) === FAILED_RECOVERY_RUN_1) {
        throw new Error("Failed recovery run 37003377107 cannot seal a Hosting handoff.");
    }
    if (Number(firebaseStatus) !== 200) throw new Error("Hosting recovery handoff requires Firebase HTTP 200.");
    if (!snapshot?.site || !snapshot?.version || !snapshot?.release) {
        throw new Error("Hosting recovery handoff is missing the live snapshot identity.");
    }
    return {
        schemaVersion: HOSTING_HANDOFF_SCHEMA,
        recoveryRunId: String(recoveryRunId),
        workflowPath: HOSTING_HANDOFF_WORKFLOW_PATH,
        principal: HOSTING_RECOVERY_PRINCIPAL,
        snapshot: {
            site: snapshot.site,
            release: snapshot.release,
            version: snapshot.version,
            releaseTime: snapshot.releaseTime,
        },
        health: { firebaseStatus: 200 },
    };
}

export function sealHostingRecoveryHandoff(handoff) {
    const canonical = canonicalJson(handoff);
    const bytes = Buffer.from(canonical, "utf8");
    if (bytes.length > MAX_HOSTING_HANDOFF_BYTES) throw new Error("Hosting recovery handoff exceeded the bounded job-output size.");
    return {
        canonical,
        sha256: sha256Bytes(bytes),
        byteLength: bytes.length,
        payloadB64: bytes.toString("base64"),
    };
}

export function verifyHostingRecoveryHandoff({
    payloadB64,
    sha256,
    byteLength,
    recoveryRunId,
    stageEvidence,
    lock = loadFrozenRelease(),
}) {
    if (!/^[1-9][0-9]*$/.test(String(byteLength))) throw new Error("Hosting recovery handoff byte length is malformed.");
    const expectedLength = Number(byteLength);
    if (!Number.isSafeInteger(expectedLength) || expectedLength > MAX_HOSTING_HANDOFF_BYTES) {
        throw new Error("Hosting recovery handoff byte length is not bounded.");
    }
    const compact = String(payloadB64 ?? "").replace(/\s+/g, "");
    if (!/^[A-Za-z0-9+/]+=*$/.test(compact)) throw new Error("Hosting recovery handoff payload is not Base64.");
    const bytes = Buffer.from(compact, "base64");
    if (bytes.length !== expectedLength) throw new Error("Hosting recovery handoff was truncated or padded.");
    if (sha256Bytes(bytes) !== String(sha256)) throw new Error("Hosting recovery handoff SHA-256 does not match the canonical bytes.");
    const text = bytes.toString("utf8");
    let parsed;
    try { parsed = JSON.parse(text); } catch { throw new Error("Hosting recovery handoff is not JSON."); }
    if (canonicalJson(parsed) !== text) throw new Error("Hosting recovery handoff is not canonical.");
    if (parsed.schemaVersion !== HOSTING_HANDOFF_SCHEMA) throw new Error("Hosting recovery handoff schema is not v1.");
    if (parsed.workflowPath !== HOSTING_HANDOFF_WORKFLOW_PATH) throw new Error("Hosting recovery handoff is not from the recovery workflow.");
    if (parsed.principal !== HOSTING_RECOVERY_PRINCIPAL) throw new Error("Hosting recovery handoff was not sealed by the Hosting deploy identity.");
    if (String(parsed.recoveryRunId) !== String(recoveryRunId)) throw new Error("Hosting recovery handoff does not belong to this recovery run.");
    if (String(parsed.recoveryRunId) === FAILED_RECOVERY_RUN_1) {
        throw new Error("Failed recovery run 37003377107 cannot supply a Hosting handoff.");
    }
    if (Number(parsed.health?.firebaseStatus) !== 200) throw new Error("Hosting recovery handoff did not prove Firebase HTTP 200.");
    validateDeploymentEvidence(stageEvidence, { operation: "stage", lock });
    assertPromotedHostingObservation({ hostingResult: parsed.snapshot, stageEvidence });
    const liveReleasePrefix = `sites/${parsed.snapshot.site}/channels/live/releases/`;
    if (!String(parsed.snapshot.release).startsWith(liveReleasePrefix)) {
        throw new Error("Hosting recovery handoff live release is not the Firebase live channel.");
    }
    return parsed;
}

export function jobConclusions(jobsPayload) {
    const jobs = Array.isArray(jobsPayload) ? jobsPayload : jobsPayload?.jobs;
    if (!Array.isArray(jobs)) throw new Error("Promotion recovery requires the original promotion job list.");
    return Object.fromEntries(jobs.map((job) => [job.name, job.conclusion]));
}

export function inspectOriginalStageRun({ run, stageRunId }) {
    if (!RUN_ID.test(String(stageRunId))) throw new Error("Evidence recovery requires an explicit numeric stage_run_id.");
    if (String(run?.id) !== String(stageRunId)) throw new Error("Inspected GitHub run is not the required original stage run.");
    if (run.path !== ".github/workflows/deploy-release.yml") throw new Error("Original stage run is not the reviewed deploy-release workflow.");
    if (run.event !== "workflow_dispatch") throw new Error("Original stage run is not a manual workflow_dispatch.");
    if (typeof run.head_sha !== "string" || !/^[0-9a-f]{40}$/.test(run.head_sha)) {
        throw new Error("Original stage run is missing an exact source SHA.");
    }
    return { runId: String(stageRunId), workflowSha: run.head_sha };
}

export function inspectOriginalPromotionRun({ run, jobs, stageRunId, promotionRunId }) {
    const refs = validateRecoveryDispatchInputs({ stageRunId, promotionRunId });
    if (String(run?.id) !== refs.promotionRunId) throw new Error("Inspected GitHub run is not the required original promotion run.");
    if (Number(run.run_attempt) !== 1) throw new Error("Promotion recovery requires the original promotion run at attempt 1.");
    if (run.path !== ".github/workflows/deploy-release.yml") throw new Error("Original promotion run is not the reviewed deploy-release workflow.");
    if (run.event !== "workflow_dispatch") throw new Error("Original promotion run is not a manual workflow_dispatch.");
    if (typeof run.head_sha !== "string" || !/^[0-9a-f]{40}$/.test(run.head_sha)) {
        throw new Error("Original promotion run is missing an exact source SHA.");
    }
    if (run.inputs) {
        if (run.inputs.operation !== "promote") throw new Error("Original promotion run was not a promote dispatch.");
        if (String(run.inputs.stage_run_id ?? "") !== refs.stageRunId) {
            throw new Error("Original promotion run did not lock the required stage_run_id.");
        }
    }
    const conclusions = jobConclusions(jobs);
    return {
        conclusions,
        workflowSha: run.head_sha,
        startedAt: run.run_started_at ?? run.created_at,
        completedAt: run.updated_at ?? run.created_at,
        runId: refs.promotionRunId,
        stageRunId: refs.stageRunId,
        attempt: 1,
    };
}

export function assertLiveMatchesStagedPromotion({
    stageEvidence,
    runtimeSnapshot,
    hostingSnapshot,
    lock = loadFrozenRelease(),
    runtimeServiceAccounts = null,
}) {
    validateDeploymentEvidence(stageEvidence, { operation: "stage", lock });
    const promoteResult = Object.fromEntries(["api", "web", "worker"].map((subject) => {
        const candidate = stageEvidence.runtime.result.candidates[subject];
        const field = subject === "worker" ? "split" : "traffic";
        const observed = runtimeSnapshot.state[subject];
        return [subject, {
            revision: candidate.revision,
            image: candidate.image,
            [field]: observed[field],
        }];
    }));
    assertPromotedRuntimeObservation({
        promoteResult,
        observedState: runtimeSnapshot.state,
        stageEvidence,
        lock,
        runtimeServiceAccounts,
    });
    const hostingResult = hostingSnapshot.version ? hostingSnapshot : hostingSnapshot.result;
    assertPromotedHostingObservation({ hostingResult, stageEvidence });
    return { promoteResult, hostingResult };
}

export function assertRecoveryHealth({ apiHealth, webHealth, firebaseStatus, workerReady }) {
    const apiOk = apiHealth?.status === "UP" || apiHealth?.status === 200 || apiHealth?.status === "200";
    if (!apiOk) throw new Error("Recovery refused: API production health is not UP.");
    const webOk = webHealth?.status === "UP" || webHealth?.status === 200 || webHealth?.status === "200";
    if (!webOk) throw new Error("Recovery refused: Web production health is not UP.");
    if (Number(firebaseStatus) !== 200) throw new Error("Recovery refused: Firebase production is not serving HTTP 200.");
    if (workerReady !== true && workerReady !== "True" && workerReady !== "CONDITION_SUCCEEDED") {
        throw new Error("Recovery refused: Worker production is not Ready.");
    }
    return true;
}

export function buildVerifiedRecoveryProof({
    run,
    jobs,
    stageRun,
    stageEvidence,
    runtimeSnapshot,
    hostingSnapshot,
    lock = loadFrozenRelease(),
    productionMatchesPromotedTargets,
    rollbackTargetsExist,
    promotionEvidenceExists,
    runtimeServiceAccounts,
    health,
    requiredStageRunId,
    requiredPromotionRunId,
}) {
    const refs = validateRecoveryDispatchInputs({
        stageRunId: requiredStageRunId ?? stageEvidence.workflow.runId,
        promotionRunId: requiredPromotionRunId ?? String(run.id),
    });
    if (String(stageEvidence.workflow.runId) !== refs.stageRunId) {
        throw new Error("Stage evidence is not the required original stage run.");
    }
    if (stageRun) inspectOriginalStageRun({ run: stageRun, stageRunId: refs.stageRunId });
    const inspected = inspectOriginalPromotionRun({
        run,
        jobs,
        stageRunId: refs.stageRunId,
        promotionRunId: refs.promotionRunId,
    });
    validateDeploymentEvidence(stageEvidence, { operation: "stage", runId: inspected.stageRunId, lock });
    if (stageEvidence.status !== "STAGED") throw new Error("Stage evidence is not STAGED.");
    if (stageEvidence.release.sourceSha !== lock.sourceSha || stageEvidence.release.manifestSha256 !== lock.releaseManifestSha256) {
        throw new Error("Stage evidence frozen release identity does not match the lock.");
    }
    if (stageEvidence.workflow.sha !== inspected.workflowSha && stageRun && stageRun.head_sha !== stageEvidence.workflow.sha) {
        throw new Error("Stage evidence source SHA does not match the original stage run.");
    }
    const accounts = runtimeServiceAccounts ?? runtimeSnapshot.serviceAccounts ?? null;
    assertLiveMatchesStagedPromotion({
        stageEvidence,
        runtimeSnapshot,
        hostingSnapshot,
        lock,
        runtimeServiceAccounts: accounts,
    });
    if (health) assertRecoveryHealth(health);
    if (productionMatchesPromotedTargets !== true) {
        throw new Error("Promotion recovery refused: current production does not match the promoted target set.");
    }
    const proof = {
        runId: inspected.runId,
        originalPromotionRunId: inspected.runId,
        requiredPromotionRunId: inspected.runId,
        attempt: inspected.attempt,
        workflowSha: inspected.workflowSha,
        sha: inspected.workflowSha,
        frozenSourceSha: lock.sourceSha,
        manifestSha256: lock.releaseManifestSha256,
        stageRunId: inspected.stageRunId,
        requiredStageRunId: inspected.stageRunId,
        jobs: inspected.conclusions,
        promotionEvidenceExists: promotionEvidenceExists === true,
        rollbackExecuted: ["rollback-hosting", "rollback-runtime", "record-rollback-evidence"].some((name) => inspected.conclusions[name] && inspected.conclusions[name] !== "skipped"),
        productionMatchesPromotedTargets: true,
        rollbackTargetsExist: rollbackTargetsExist === true,
        runtimeServiceAccounts: accounts,
        startedAt: inspected.startedAt,
        completedAt: inspected.completedAt,
    };
    assertPromotionRecoveryPreconditions(proof);
    return proof;
}

export function verifyRecoveredPromotionEvidence(evidence, { stageRunId, promotionRunId, lock = loadFrozenRelease() }) {
    validateDeploymentEvidence(evidence, { operation: "promote", runId: promotionRunId, lock });
    if (evidence.schemaVersion !== "boundarymesh.deployment/v1") throw new Error("Recovered promotion evidence schema is not v1.");
    if (evidence.operation !== "promote") throw new Error("Recovered promotion evidence operation is not promote.");
    if (evidence.status !== "PROMOTED") throw new Error("Recovered promotion evidence status is not PROMOTED.");
    if (String(evidence.workflow.runId) !== String(promotionRunId)) throw new Error("Recovered promotion evidence substituted a different workflow run identity.");
    if (String(evidence.workflow.runId) === FAILED_RECOVERY_RUN_1) throw new Error("Recovered promotion evidence must not use failed recovery run 37003377107.");
    if (String(evidence.references.stageRunId) !== String(stageRunId)) throw new Error("Recovered promotion evidence stage run is not the original stage run.");
    if (evidence.runtime?.checks?.recoveredAfterSuccessfulPromotion !== true) {
        throw new Error("Recovered promotion evidence is missing recoveredAfterSuccessfulPromotion.");
    }
    if (evidence.hosting?.checks?.recoveredAfterSuccessfulPromotion !== true) {
        throw new Error("Recovered promotion evidence is missing recoveredAfterSuccessfulPromotion.");
    }
    return true;
}

export function recoverPromotionEvidence({
    stageEvidence,
    runtimeSnapshot,
    hostingSnapshot,
    proof,
    outputDir,
    publish = false,
    lock = loadFrozenRelease(),
    collisionRunner,
    publisher,
}) {
    assertPromotionRecoveryPreconditions(proof);
    if (String(proof.runId) !== String(proof.originalPromotionRunId ?? proof.runId)) {
        throw new Error("Promotion recovery must bind the original promotion run as evidence identity.");
    }
    if (String(proof.runId) === FAILED_RECOVERY_RUN_1) {
        throw new Error("Promotion recovery must not bind failed recovery run 37003377107 as evidence identity.");
    }
    const evidence = buildRecoveredPromotionEvidence({
        stageEvidence,
        runtimeSnapshot: { ...runtimeSnapshot, serviceAccounts: runtimeSnapshot.serviceAccounts ?? proof.runtimeServiceAccounts },
        hostingSnapshot,
        promotionWorkflow: {
            runId: String(proof.runId),
            runAttempt: String(proof.attempt),
            sha: proof.workflowSha,
            ref: "refs/heads/main",
        },
        references: { stageRunId: String(proof.stageRunId) },
        startedAt: proof.startedAt,
        completedAt: proof.completedAt,
        lock,
    });
    const version = evidenceVersion("promote", proof.runId, lock);
    if (version !== `${lock.sourceSha}-promote-run-${proof.runId}`) {
        throw new Error("Recovered promotion evidence identity does not use the original promote-run pattern.");
    }
    const paths = writeDeploymentEvidence(evidence, outputDir, { operation: "promote", runId: proof.runId, lock });
    if (publish) {
        assertEvidenceVersionAbsent({ operation: "promote", runId: proof.runId, lock, runner: collisionRunner });
        const published = publisher
            ? publisher({ operation: "promote", runId: proof.runId, directory: outputDir, lock })
            : publishEvidence({ operation: "promote", runId: proof.runId, directory: outputDir, lock });
        return { evidence, paths, version, published };
    }
    return { evidence, paths, version, published: null };
}

function refuseMutationFlags() {
    if (process.argv.includes("--promote") || process.argv.includes("--rollback") || process.argv.includes("--stage")) {
        throw new Error("Evidence recovery cannot invoke a deployment mutation command.");
    }
}

function main() {
    refuseMutationFlags();
    const command = process.argv[2];
    if (command === "validate-inputs") {
        process.stdout.write(`${JSON.stringify(validateRecoveryDispatchInputs({
            stageRunId: option("--stage-run-id") ?? "",
            promotionRunId: option("--promotion-run-id") ?? "",
        }))}\n`);
        return;
    }
    if (command === "seal-hosting-handoff") {
        const snapshot = readJson(resolve(option("--snapshot")));
        const handoff = buildHostingRecoveryHandoff({
            snapshot,
            firebaseStatus: option("--firebase-status"),
            recoveryRunId: option("--recovery-run-id") ?? "",
        });
        const sealed = sealHostingRecoveryHandoff(handoff);
        const githubOutput = option("--github-output");
        if (githubOutput) {
            appendFileSync(githubOutput, `payload_b64=${sealed.payloadB64}\nsha256=${sealed.sha256}\nbyte_length=${sealed.byteLength}\n`);
        }
        process.stdout.write(`${JSON.stringify({ sha256: sealed.sha256, byteLength: sealed.byteLength })}\n`);
        return;
    }
    if (command === "verify-hosting-handoff") {
        const parsed = verifyHostingRecoveryHandoff({
            payloadB64: option("--payload-b64") ?? "",
            sha256: option("--sha256") ?? "",
            byteLength: option("--byte-length") ?? "",
            recoveryRunId: option("--recovery-run-id") ?? "",
            stageEvidence: readJson(resolve(option("--stage-evidence"))),
        });
        writeFileSync(resolve(option("--output")), canonicalJson(parsed.snapshot), { flag: "wx" });
        process.stdout.write("hosting recovery handoff verified\n");
        return;
    }
    if (command === "prepare") {
        const outputDir = resolve(option("--output-dir"));
        mkdirSync(outputDir, { recursive: true });
        const stageEvidence = readJson(resolve(option("--stage-evidence")));
        const requiredStageRunId = option("--stage-run-id");
        const requiredPromotionRunId = option("--promotion-run-id");
        if (process.argv.includes("--require-absent")) {
            assertEvidenceVersionAbsent({ operation: "promote", runId: requiredPromotionRunId });
        }
        const proof = buildVerifiedRecoveryProof({
            run: readJson(resolve(option("--promotion-run"))),
            jobs: readJson(resolve(option("--promotion-jobs"))),
            stageRun: option("--stage-run") ? readJson(resolve(option("--stage-run"))) : null,
            stageEvidence,
            runtimeSnapshot: readJson(resolve(option("--runtime-snapshot"))),
            hostingSnapshot: readJson(resolve(option("--hosting-snapshot"))),
            runtimeServiceAccounts: option("--runtime-service-accounts") ? readJson(resolve(option("--runtime-service-accounts"))) : null,
            health: option("--health") ? readJson(resolve(option("--health"))) : null,
            productionMatchesPromotedTargets: true,
            rollbackTargetsExist: flagBoolean("--rollback-targets-exist"),
            promotionEvidenceExists: flagBoolean("--promotion-evidence-exists"),
            requiredStageRunId: option("--stage-run-id"),
            requiredPromotionRunId: option("--promotion-run-id"),
        });
        const proofPath = resolve(outputDir, "proof.json");
        writeFileSync(proofPath, canonicalJson(proof), { flag: "wx" });
        process.stdout.write(`${JSON.stringify({ proofPath, runId: proof.runId, stageRunId: proof.stageRunId })}\n`);
        return;
    }
    if (command === "verify") {
        const document = resolve(option("--document"));
        const sidecar = resolve(option("--sidecar"));
        const stageRunId = option("--stage-run-id");
        const promotionRunId = option("--promotion-run-id");
        const evidence = verifyEvidenceFiles(document, sidecar, { operation: "promote", runId: promotionRunId });
        verifyRecoveredPromotionEvidence(evidence, { stageRunId, promotionRunId });
        process.stdout.write("recovered promotion evidence verified\n");
        return;
    }
    if (command !== "recover") throw new Error("Evidence recovery mutation command is recover only.");
    const result = recoverPromotionEvidence({
        stageEvidence: readJson(resolve(option("--stage-evidence"))),
        runtimeSnapshot: readJson(resolve(option("--runtime-snapshot"))),
        hostingSnapshot: readJson(resolve(option("--hosting-snapshot"))),
        proof: readJson(resolve(option("--proof"))),
        outputDir: resolve(option("--output-dir")),
        publish: process.argv.includes("--publish"),
    });
    process.stdout.write(`${JSON.stringify({ version: result.version, published: Boolean(result.published) })}\n`);
}

if (resolve(process.argv[1] ?? "") === resolve(fileURLToPath(import.meta.url))) main();
