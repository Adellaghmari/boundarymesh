import { readFileSync, writeFileSync } from "node:fs";
import { resolve } from "node:path";
import { fileURLToPath } from "node:url";
import {
    canonicalJson,
    loadFrozenRelease,
    validateDeploymentEvidence,
    verifyEvidenceFiles,
    writeDeploymentEvidence,
} from "./contract.mjs";

function readJson(path) {
    return JSON.parse(readFileSync(path, "utf8"));
}

function exactTransition(actual, expected, label) {
    if (canonicalJson(actual) !== canonicalJson(expected)) throw new Error(`${label} does not match the referenced prior evidence.`);
}

function frozenImageReference(subject, lock) {
    const names = { api: "boundarymesh-api", web: "boundarymesh-web-edge", worker: "boundarymesh-worker" };
    return `${lock.region}-docker.pkg.dev/${lock.project}/${lock.dockerRepository}/${names[subject]}@${lock.images[subject]}`;
}

function resolvedImageReference(subject, runtimeDigest, lock) {
    const names = { api: "boundarymesh-api", web: "boundarymesh-web-edge", worker: "boundarymesh-worker" };
    return `${lock.region}-docker.pkg.dev/${lock.project}/${lock.dockerRepository}/${names[subject]}@${runtimeDigest}`;
}

function allocationField(subject) {
    return subject === "worker" ? "split" : "traffic";
}

const EXPECTED_RUNTIME_SERVICE_ACCOUNTS = Object.freeze({
    api: "boundarymesh-api@boundarymesh-dev.iam.gserviceaccount.com",
    web: "boundarymesh-web@boundarymesh-dev.iam.gserviceaccount.com",
    worker: "boundarymesh-worker@boundarymesh-dev.iam.gserviceaccount.com",
});

export function assertPromotedRuntimeObservation({ promoteResult, observedState, stageEvidence, lock = loadFrozenRelease(), runtimeServiceAccounts = null }) {
    if (!promoteResult || !observedState || !stageEvidence?.runtime?.result?.candidates) {
        throw new Error("Promotion observation is missing the staged candidate, promote result, or live snapshot.");
    }
    for (const subject of ["api", "web", "worker"]) {
        const candidate = stageEvidence.runtime.result.candidates[subject];
        const promoted = promoteResult[subject];
        const observed = observedState[subject];
        const field = allocationField(subject);
        const frozenImage = frozenImageReference(subject, lock);
        const resolvedImage = resolvedImageReference(subject, candidate.imageResolution.runtimeDigest, lock);
        if (promoted?.revision !== candidate.revision || observed?.revision !== candidate.revision) {
            throw new Error(`Promotion ${subject} revision is not the exact staged candidate.`);
        }
        if (promoted?.image !== candidate.image || promoted?.image !== frozenImage) {
            throw new Error(`Promotion ${subject} result is not bound to the frozen parent digest.`);
        }
        if (observed?.image !== resolvedImage) {
            throw new Error(`Promotion ${subject} live observation is not the exact resolved linux/amd64 digest.`);
        }
        if (candidate.imageResolution.parentDigest !== lock.images[subject] || candidate.imageResolution.runtimeDigest !== resolvedImage.split("@")[1]) {
            throw new Error(`Promotion ${subject} OCI parent/child digest contract is not the frozen pair.`);
        }
        assertFullAllocation(promoted, field, `Promotion ${subject} result`);
        assertFullAllocation(observed, field, `Promotion ${subject} observation`);
        if (promoted[field][0].revision !== candidate.revision || observed[field][0].revision !== candidate.revision) {
            throw new Error(`Promotion ${subject} allocation does not point at the staged candidate.`);
        }
        const stagedCandidateEntry = candidate[field].find((entry) => entry.revision === candidate.revision);
        if (!stagedCandidateEntry || stagedCandidateEntry.percent !== 0) {
            throw new Error(`Promotion ${subject} staged candidate was not recorded at zero production allocation.`);
        }
        if (promoted[field][0].percent !== 100 || observed[field][0].percent !== 100) {
            throw new Error(`Promotion ${subject} allocation did not transition to 100 percent.`);
        }
        if (runtimeServiceAccounts && runtimeServiceAccounts[subject] !== EXPECTED_RUNTIME_SERVICE_ACCOUNTS[subject]) {
            throw new Error(`Promotion ${subject} runtime service account is not the exact deployed identity.`);
        }
    }
    return true;
}

export function assertPromotedHostingObservation({ hostingResult, stageEvidence }) {
    if (hostingResult?.version !== stageEvidence.hosting.result.version || hostingResult?.site !== stageEvidence.hosting.before.site) {
        throw new Error("Promotion Hosting observation is not the exact staged version.");
    }
    if (hostingResult?.version === stageEvidence.hosting.before.version) {
        throw new Error("Promotion Hosting observation is still the pre-promotion baseline version.");
    }
    return true;
}

export function assertPromotionRecoveryPreconditions(proof) {
    const requiredJobs = {
        "validate-release": "success",
        "load-stage-evidence": "success",
        "promotion-hosting-preflight": "success",
        "promote-runtime": "success",
        "promote-hosting": "success",
        "record-promote-evidence": "failure",
    };
    const forbiddenJobs = ["rollback-hosting", "rollback-runtime", "record-rollback-evidence", "stage-runtime", "stage-hosting", "record-stage-evidence"];
    if (!proof || proof.attempt !== 1 || !/^[1-9][0-9]*$/.test(String(proof.runId))) {
        throw new Error("Promotion recovery requires the original promotion run at attempt 1.");
    }
    if (typeof proof.workflowSha !== "string" || !/^[0-9a-f]{40}$/.test(proof.workflowSha)) {
        throw new Error("Promotion recovery requires the exact original promotion source SHA.");
    }
    if (proof.sha && proof.sha !== proof.workflowSha) {
        throw new Error("Promotion recovery source SHA does not match the original promotion workflow SHA.");
    }
    if (proof.frozenSourceSha !== loadFrozenRelease().sourceSha || proof.manifestSha256 !== loadFrozenRelease().releaseManifestSha256) {
        throw new Error("Promotion recovery frozen release identity does not match the lock.");
    }
    if (!proof.requiredStageRunId || String(proof.stageRunId) !== String(proof.requiredStageRunId)) {
        throw new Error("Promotion recovery stage run is not the required original stage run.");
    }
    if (!proof.requiredPromotionRunId || String(proof.runId) !== String(proof.requiredPromotionRunId)) {
        throw new Error("Promotion recovery promotion run is not the required original promotion run.");
    }
    if (proof.productionMatchesPromotedTargets !== true) {
        throw new Error("Promotion recovery refused: current production does not match the promoted target set.");
    }
    if (proof.rollbackTargetsExist !== true) {
        throw new Error("Promotion recovery refused: original rollback targets are not proven to exist.");
    }
    if (!proof.runtimeServiceAccounts) {
        throw new Error("Promotion recovery refused: runtime service account identities were not proven.");
    }
    for (const subject of ["api", "web", "worker"]) {
        if (proof.runtimeServiceAccounts[subject] !== EXPECTED_RUNTIME_SERVICE_ACCOUNTS[subject]) {
            throw new Error(`Promotion recovery refused: ${subject} runtime service account is not the exact deployed identity.`);
        }
    }
    for (const [name, expected] of Object.entries(requiredJobs)) {
        if (proof.jobs?.[name] !== expected) throw new Error(`Promotion recovery refused: ${name} is not ${expected}.`);
    }
    for (const name of forbiddenJobs) {
        if (proof.jobs?.[name] && proof.jobs[name] !== "skipped") throw new Error(`Promotion recovery refused: ${name} executed.`);
    }
    if (proof.promotionEvidenceExists === true) throw new Error("Promotion recovery refused: promotion evidence already exists.");
    if (proof.rollbackExecuted === true) throw new Error("Promotion recovery refused: a rollback job executed.");
    return true;
}

export function observePromotion({
    priorEvidence,
    snapshot,
    runtimeJobResult,
    hostingJobResult,
    runtimeValue,
    hostingValue,
    lock = loadFrozenRelease(),
}) {
    validateDeploymentEvidence(priorEvidence, { operation: "stage", lock });
    let runtime = { before: priorEvidence.runtime.before, result: snapshot.state, checks: { promotionCompleted: false, observedAfterJobs: true } };
    if (runtimeJobResult === "success" && runtimeValue) {
        runtime = typeof runtimeValue === "string" ? decodeJson(runtimeValue) : structuredClone(runtimeValue);
        assertPromotedRuntimeObservation({
            promoteResult: runtime.result,
            observedState: snapshot.state,
            stageEvidence: priorEvidence,
            lock,
            runtimeServiceAccounts: snapshot.serviceAccounts ?? null,
        });
        runtime.checks = { ...runtime.checks, promotionCompleted: true, observedAfterJobs: true, identityContinuous: true, allocationTransition: true };
    }
    const hosting = hostingValue
        ? (typeof hostingValue === "string" ? decodeJson(hostingValue) : structuredClone(hostingValue))
        : { before: priorEvidence.hosting.before, result: priorEvidence.hosting.before, checks: { promotionCompleted: false, notAttempted: true } };
    if (hostingJobResult === "success") {
        assertPromotedHostingObservation({ hostingResult: hosting.result, stageEvidence: priorEvidence });
        exactTransition(hosting.before, priorEvidence.hosting.before, "Promotion Hosting baseline");
        hosting.checks = { ...hosting.checks, promotionCompleted: true, exactStagedVersion: true };
    }
    return {
        runtime,
        hosting,
        outcome: runtimeJobResult === "success" && hostingJobResult === "success" ? "success" : "failed",
    };
}

export function buildRecoveredPromotionEvidence({
    stageEvidence,
    runtimeSnapshot,
    hostingSnapshot,
    promotionWorkflow,
    references,
    startedAt,
    completedAt,
    lock = loadFrozenRelease(),
}) {
    validateDeploymentEvidence(stageEvidence, { operation: "stage", runId: references.stageRunId, lock });
    const promoteResult = Object.fromEntries(["api", "web", "worker"].map((subject) => {
        const candidate = stageEvidence.runtime.result.candidates[subject];
        const field = allocationField(subject);
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
        runtimeServiceAccounts: runtimeSnapshot.serviceAccounts ?? null,
    });
    const hostingResult = hostingSnapshot.version ? hostingSnapshot : hostingSnapshot.result;
    assertPromotedHostingObservation({ hostingResult, stageEvidence });
    return buildEvidence({
        operation: "promote",
        outcome: "success",
        runtime: {
            before: stageEvidence.runtime.before,
            result: promoteResult,
            checks: { recoveredAfterSuccessfulPromotion: true, identityContinuous: true, allocationTransition: true },
        },
        hosting: {
            before: stageEvidence.hosting.before,
            result: hostingResult,
            checks: { exactStagedVersion: true, recoveredAfterSuccessfulPromotion: true },
        },
        priorEvidence: stageEvidence,
        workflow: promotionWorkflow,
        references: { stageRunId: references.stageRunId, promotionRunId: null },
        startedAt,
        completedAt,
        lock,
    });
}

function assertFullAllocation(state, field, label) {
    const entries = state[field];
    if (!Array.isArray(entries) || entries.length !== 1 || entries[0].percent !== 100 || entries[0].revision !== state.revision) {
        throw new Error(`${label} must assign exactly 100 percent to its recorded revision.`);
    }
}

function matchesState(actual, expected) {
    return canonicalJson(actual) === canonicalJson(expected);
}

function verifyPriorTransition(operation, status, runtime, hosting, references, priorEvidence) {
    if (operation === "stage") return;
    if (!priorEvidence) throw new Error(`${operation} requires validated prior deployment evidence.`);
    if (operation === "promote") {
        if (priorEvidence.operation !== "stage" || references.stageRunId !== priorEvidence.workflow.runId || references.promotionRunId) throw new Error("Promotion does not reference the exact stage evidence run.");
        exactTransition(runtime.before, priorEvidence.runtime.before, "Promotion runtime baseline");
        exactTransition(hosting.before, priorEvidence.hosting.before, "Promotion Hosting baseline");
        for (const subject of ["api", "web", "worker"]) {
            const candidate = priorEvidence.runtime.result.candidates[subject];
            const baseline = priorEvidence.runtime.before[subject];
            const result = runtime.result[subject];
            const isCandidate = result?.revision === candidate.revision && result?.image === candidate.image;
            const isBaseline = matchesState(result, baseline);
            if (status === "PROMOTED" ? !isCandidate : (!isCandidate && !isBaseline)) throw new Error(`Promotion ${subject} result differs from the staged candidate/baseline set.`);
            assertFullAllocation(result, subject === "worker" ? "split" : "traffic", `Promotion ${subject}`);
        }
        const hostingIsCandidate = hosting.result?.site === priorEvidence.hosting.before.site && hosting.result?.version === priorEvidence.hosting.result.version;
        const hostingIsBaseline = matchesState(hosting.result, priorEvidence.hosting.before);
        if (status === "PROMOTED" ? !hostingIsCandidate : (!hostingIsCandidate && !hostingIsBaseline)) throw new Error("Promotion Hosting result differs from the staged version/baseline set.");
        return;
    }
    if (priorEvidence.operation !== "promote" || references.stageRunId !== priorEvidence.references.stageRunId || references.promotionRunId !== priorEvidence.workflow.runId) throw new Error("Rollback does not reference the exact promotion evidence run.");
    exactTransition(runtime.before, priorEvidence.runtime.result, "Rollback runtime baseline");
    exactTransition(hosting.before, priorEvidence.hosting.result, "Rollback Hosting baseline");
    exactTransition(runtime.result, priorEvidence.runtime.before, "Rollback runtime result");
    if (hosting.result?.site !== priorEvidence.hosting.before.site || hosting.result?.version !== priorEvidence.hosting.before.version) {
        throw new Error("Rollback Hosting result does not restore the retained site/version identity.");
    }
}

export function buildEvidence({ operation, outcome = "success", runtime, hosting, workflow, references, priorEvidence = null, startedAt, completedAt = new Date().toISOString(), lock = loadFrozenRelease() }) {
    if (!["success", "failed"].includes(outcome) || (operation !== "promote" && outcome !== "success")) throw new Error("Invalid deployment operation outcome.");
    const status = operation === "promote" && outcome === "failed" ? "PROMOTION_FAILED" : { stage: "STAGED", promote: "PROMOTED", rollback: "ROLLED_BACK" }[operation];
    if (!status) throw new Error("Unknown deployment evidence operation.");
    if (runtime.operation && runtime.operation !== operation) throw new Error("Runtime result operation mismatch.");
    if (hosting.operation && hosting.operation !== operation) throw new Error("Hosting result operation mismatch.");
    verifyPriorTransition(operation, status, runtime, hosting, references, priorEvidence);
    let rollback;
    if (operation === "stage") {
        rollback = {
            apiRevision: runtime.before.api.revision,
            webRevision: runtime.before.web.revision,
            workerRevision: runtime.before.worker.revision,
            firebaseVersion: hosting.before.version,
        };
    } else {
        rollback = structuredClone(priorEvidence.rollback);
    }
    const evidence = {
        schemaVersion: "boundarymesh.deployment/v1",
        operation,
        status,
        release: { sourceSha: lock.sourceSha, manifestSha256: lock.releaseManifestSha256, publicationRunId: lock.publicationRunId },
        workflow: {
            runId: String(workflow.runId),
            runAttempt: String(workflow.runAttempt),
            sha: workflow.sha,
            ref: workflow.ref,
            path: ".github/workflows/deploy-release.yml",
        },
        environments: { runtime: "boundarymesh-deploy", hosting: "boundarymesh-hosting-deploy" },
        references: { stageRunId: references.stageRunId ?? null, promotionRunId: references.promotionRunId ?? null },
        runtime: { operation, before: runtime.before, result: runtime.result ?? { candidates: runtime.candidates }, checks: runtime.checks },
        hosting: { operation, before: hosting.before, result: hosting.result, checks: hosting.checks },
        checks: {
            frozenReleaseVerified: true,
            splitAuthorityPreserved: true,
            noApplicationBuild: true,
            noAutomaticRollback: true,
            operationSucceeded: outcome === "success",
        },
        timestamps: { startedAt, completedAt },
        rollback,
    };
    return validateDeploymentEvidence(evidence, { operation, runId: workflow.runId, lock });
}

export function encodeFile(path) {
    return readFileSync(path).toString("base64");
}

export function decodeEvidence(value) {
    const parsed = JSON.parse(Buffer.from(value, "base64").toString("utf8"));
    return validateDeploymentEvidence(parsed);
}

export function decodeJson(value) {
    return JSON.parse(Buffer.from(value, "base64").toString("utf8"));
}

function option(name) {
    const index = process.argv.indexOf(name);
    return index >= 0 ? process.argv[index + 1] : undefined;
}

function main() {
    const command = process.argv[2];
    if (command === "build") {
        const operation = option("--operation");
        const prior = option("--prior-evidence") ? readJson(option("--prior-evidence")) : null;
        const evidence = buildEvidence({
            operation,
            outcome: option("--outcome") ?? "success",
            runtime: readJson(option("--runtime")),
            hosting: readJson(option("--hosting")),
            priorEvidence: prior,
            workflow: { runId: option("--run-id"), runAttempt: option("--run-attempt"), sha: option("--sha"), ref: option("--ref") },
            references: { stageRunId: option("--stage-run-id") || null, promotionRunId: option("--promotion-run-id") || null },
            startedAt: option("--started-at"),
        });
        const paths = writeDeploymentEvidence(evidence, resolve(option("--output-dir")), { operation, runId: option("--run-id") });
        process.stdout.write(`${JSON.stringify(paths)}\n`);
    } else if (command === "verify") {
        verifyEvidenceFiles(resolve(option("--document")), resolve(option("--sidecar")), { operation: option("--operation"), runId: option("--run-id") });
        process.stdout.write("deployment evidence verified\n");
    } else if (command === "encode") {
        process.stdout.write(`${encodeFile(resolve(option("--file")))}\n`);
    } else if (command === "decode") {
        process.stdout.write(canonicalJson(decodeEvidence(option("--value"))));
    } else if (command === "decode-json") {
        process.stdout.write(canonicalJson(decodeJson(option("--value"))));
    } else if (command === "observe-promotion") {
        const observed = observePromotion({
            priorEvidence: readJson(option("--prior-evidence")),
            snapshot: readJson(option("--runtime-snapshot")),
            runtimeJobResult: option("--runtime-result"),
            hostingJobResult: option("--hosting-result"),
            runtimeValue: option("--runtime-value"),
            hostingValue: option("--hosting-value"),
        });
        writeFileSync(resolve(option("--runtime-output")), canonicalJson(observed.runtime), { flag: "wx" });
        writeFileSync(resolve(option("--hosting-output")), canonicalJson(observed.hosting), { flag: "wx" });
        process.stdout.write(observed.outcome);
    } else if (command === "assert-reference") {
        const evidence = readJson(option("--document"));
        validateDeploymentEvidence(evidence, { operation: option("--operation"), runId: option("--run-id") });
        if (option("--stage-run-id") && evidence.references.stageRunId !== option("--stage-run-id")) throw new Error("Deployment evidence stage_run_id does not match the explicit input.");
        process.stdout.write("deployment evidence reference verified\n");
    } else {
        throw new Error(`Unknown deployment evidence command: ${command}`);
    }
}

if (resolve(process.argv[1] ?? "") === resolve(fileURLToPath(import.meta.url))) main();
