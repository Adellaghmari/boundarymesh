import assert from "node:assert/strict";
import { mkdtempSync, readFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import test from "node:test";
import { assertEvidenceLookupAbsent } from "../artifacts.mjs";
import { canonicalJson, evidenceVersion, loadFrozenRelease, validateDeploymentEvidence } from "../contract.mjs";
import {
    assertPromotedHostingObservation,
    assertPromotedRuntimeObservation,
    assertPromotionRecoveryPreconditions,
    buildRecoveredPromotionEvidence,
    observePromotion,
} from "../evidence.mjs";
import {
    buildHostingRecoveryHandoff,
    buildVerifiedRecoveryProof,
    FAILED_RECOVERY_RUN_1,
    isRecoveryRun1HostingQuotaDenial,
    recoverPromotionEvidence,
    sealHostingRecoveryHandoff,
    verifyHostingRecoveryHandoff,
    verifyRecoveredPromotionEvidence,
} from "../recover-promotion-evidence.mjs";

const fixtures = join(import.meta.dirname, "fixtures");
const lock = loadFrozenRelease();
const stageEvidence = JSON.parse(readFileSync(join(fixtures, "run-8-stage-evidence.json"), "utf8"));
const promoteRuntime = JSON.parse(readFileSync(join(fixtures, "run-9-promote-runtime.json"), "utf8"));
const promoteHosting = JSON.parse(readFileSync(join(fixtures, "run-9-promote-hosting.json"), "utf8"));
const runtimeSnapshot = JSON.parse(readFileSync(join(fixtures, "run-9-runtime-snapshot.json"), "utf8"));
const promotionRun = JSON.parse(readFileSync(join(fixtures, "run-9-promotion-run.json"), "utf8"));
const promotionJobs = JSON.parse(readFileSync(join(fixtures, "run-9-promotion-jobs.json"), "utf8"));
const stageRun = JSON.parse(readFileSync(join(fixtures, "run-8-stage-run.json"), "utf8"));
const health = {
    apiHealth: { status: "UP" },
    webHealth: { status: "UP" },
    firebaseStatus: 200,
    workerReady: true,
};
const serviceAccounts = {
    api: "boundarymesh-api@boundarymesh-dev.iam.gserviceaccount.com",
    web: "boundarymesh-web@boundarymesh-dev.iam.gserviceaccount.com",
    worker: "boundarymesh-worker@boundarymesh-dev.iam.gserviceaccount.com",
};

function legacyExactTransition(actual, expected) {
    if (canonicalJson(actual) !== canonicalJson(expected)) {
        throw new Error("Post-promotion runtime observation does not match the referenced prior evidence.");
    }
}

function validProof(overrides = {}) {
    return {
        runId: "36945783284",
        originalPromotionRunId: "36945783284",
        attempt: 1,
        workflowSha: "2326b488d722222de0966321f165215c30dcebe9",
        sha: "2326b488d722222de0966321f165215c30dcebe9",
        frozenSourceSha: lock.sourceSha,
        manifestSha256: lock.releaseManifestSha256,
        stageRunId: "36941014905",
        requiredStageRunId: "36941014905",
        requiredPromotionRunId: "36945783284",
        jobs: {
            "validate-release": "success",
            "load-stage-evidence": "success",
            "promotion-hosting-preflight": "success",
            "promote-runtime": "success",
            "promote-hosting": "success",
            "record-promote-evidence": "failure",
            "rollback-hosting": "skipped",
            "rollback-runtime": "skipped",
            "record-rollback-evidence": "skipped",
            "stage-runtime": "skipped",
            "stage-hosting": "skipped",
            "record-stage-evidence": "skipped",
        },
        promotionEvidenceExists: false,
        rollbackExecuted: false,
        productionMatchesPromotedTargets: true,
        rollbackTargetsExist: true,
        runtimeServiceAccounts: { ...serviceAccounts },
        startedAt: "2026-10-02T00:25:00.000Z",
        completedAt: "2026-10-02T00:28:59.000Z",
        ...overrides,
    };
}

function observeRun9(overrides = {}) {
    return observePromotion({
        priorEvidence: stageEvidence,
        snapshot: runtimeSnapshot,
        runtimeJobResult: "success",
        hostingJobResult: "success",
        runtimeValue: promoteRuntime,
        hostingValue: promoteHosting,
        ...overrides,
    });
}

test("run #9 parent-vs-resolved image comparison fails the old exactTransition and passes the repaired observer", () => {
    assert.throws(
        () => legacyExactTransition(promoteRuntime.result, runtimeSnapshot.state),
        /Post-promotion runtime observation does not match the referenced prior evidence/,
    );
    assert.equal(promoteRuntime.result.api.image.includes("bcfd9b01"), true);
    assert.equal(runtimeSnapshot.state.api.image.includes("41e70a6f"), true);
    assert.doesNotThrow(() => assertPromotedRuntimeObservation({
        promoteResult: promoteRuntime.result,
        observedState: runtimeSnapshot.state,
        stageEvidence,
        runtimeServiceAccounts: serviceAccounts,
    }));
    const observed = observeRun9();
    assert.equal(observed.outcome, "success");
    assert.equal(observed.runtime.result.api.image, stageEvidence.runtime.result.candidates.api.image);
    assert.equal(observed.runtime.result.api.revision, "boundarymesh-api-c-36941014905");
});

test("promotion identity continuity remains exact for revision, digest, SA, run, and frozen release", () => {
    const cases = [
        ["api revision", (value) => { value.promoteResult.api.revision = "boundarymesh-api-00004-9hw"; }, /revision/],
        ["web revision", (value) => { value.observedState.web.revision = "boundarymesh-web-00004-k48"; }, /revision/],
        ["worker revision", (value) => { value.promoteResult.worker.revision = "boundarymesh-worker-00002-v6j"; }, /revision/],
        ["parent digest", (value) => { value.promoteResult.api.image = value.observedState.api.image; }, /frozen parent digest/],
        ["resolved digest", (value) => { value.observedState.web.image = value.promoteResult.web.image; }, /resolved linux\/amd64 digest/],
        ["runtime SA", (value) => { value.runtimeServiceAccounts.api = "other@boundarymesh-dev.iam.gserviceaccount.com"; }, /service account/],
    ];
    for (const [label, mutate, pattern] of cases) {
        const value = {
            promoteResult: structuredClone(promoteRuntime.result),
            observedState: structuredClone(runtimeSnapshot.state),
            stageEvidence,
            runtimeServiceAccounts: { ...serviceAccounts },
        };
        mutate(value);
        assert.throws(() => assertPromotedRuntimeObservation(value), pattern, label);
    }
    assert.throws(() => assertPromotedHostingObservation({
        hostingResult: { ...promoteHosting.result, version: "sites/boundarymesh-dev/versions/1bce16129fb1a0d4" },
        stageEvidence,
    }), /baseline version|exact staged version/);
    assert.throws(() => validateDeploymentEvidence({ ...stageEvidence, workflow: { ...stageEvidence.workflow, runId: "1" } }, { operation: "stage", runId: "36941014905" }), /workflow run/);
    assert.throws(() => assertPromotionRecoveryPreconditions(validProof({ frozenSourceSha: "0".repeat(40) })), /frozen release identity/);
    assert.throws(() => assertPromotionRecoveryPreconditions(validProof({ manifestSha256: "0".repeat(64) })), /frozen release identity/);
    assert.throws(() => assertPromotionRecoveryPreconditions(validProof({ requiredPromotionRunId: "1" })), /promotion run/);
    assert.throws(() => assertPromotionRecoveryPreconditions(validProof({ stageRunId: "1" })), /stage run/);
});

test("promotion allocation transition requires staged zero to promoted 100 and exact Firebase version", () => {
    const stillZero = structuredClone(promoteRuntime.result);
    stillZero.api.traffic[0].percent = 0;
    assert.throws(() => assertPromotedRuntimeObservation({
        promoteResult: stillZero,
        observedState: runtimeSnapshot.state,
        stageEvidence,
    }), /100 percent/);
    const partial = structuredClone(runtimeSnapshot.state);
    partial.web.traffic = [
        { ...partial.web.traffic[0], percent: 50 },
        { type: "TRAFFIC_TARGET_ALLOCATION_TYPE_REVISION", revision: "boundarymesh-web-00004-k48", percent: 50, tag: null },
    ];
    assert.throws(() => assertPromotedRuntimeObservation({
        promoteResult: promoteRuntime.result,
        observedState: partial,
        stageEvidence,
    }), /100 percent/);
    const historical = structuredClone(promoteRuntime.result);
    historical.worker.revision = "boundarymesh-worker-00002-v6j";
    historical.worker.split[0].revision = "boundarymesh-worker-00002-v6j";
    assert.throws(() => assertPromotedRuntimeObservation({
        promoteResult: historical,
        observedState: runtimeSnapshot.state,
        stageEvidence,
    }), /revision|staged candidate/);
    assert.throws(() => assertPromotedHostingObservation({
        hostingResult: { ...promoteHosting.result, version: "sites/boundarymesh-dev/versions/otherversion" },
        stageEvidence,
    }), /exact staged version/);
});

test("representation differences do not loosen identity or allocation matching", () => {
    const swapped = structuredClone(promoteRuntime.result);
    swapped.api.image = runtimeSnapshot.state.api.image;
    assert.throws(() => assertPromotedRuntimeObservation({
        promoteResult: swapped,
        observedState: runtimeSnapshot.state,
        stageEvidence,
    }), /frozen parent digest/);
    assert.notEqual(canonicalJson(promoteRuntime.result), canonicalJson(runtimeSnapshot.state));
    assert.doesNotThrow(() => assertPromotedRuntimeObservation({
        promoteResult: promoteRuntime.result,
        observedState: runtimeSnapshot.state,
        stageEvidence,
    }));
});

test("evidence-only recovery binds the original promotion run and cannot reach mutation adapters", () => {
    const recoverSource = readFileSync(join(import.meta.dirname, "..", "recover-promotion-evidence.mjs"), "utf8");
    assert.doesNotMatch(recoverSource, /cloud-runtime\.mjs|cloud-hosting\.mjs/);
    assert.doesNotMatch(recoverSource, /promoteRuntime|promoteHosting|rollbackRuntime|rollbackHosting/);
    const directory = mkdtempSync(join(tmpdir(), "bm-recover-"));
    const recovered = recoverPromotionEvidence({
        stageEvidence,
        runtimeSnapshot,
        hostingSnapshot: promoteHosting.result,
        proof: validProof(),
        outputDir: directory,
        publish: false,
    });
    assert.equal(recovered.published, null);
    assert.equal(recovered.version, `${lock.sourceSha}-promote-run-36945783284`);
    assert.equal(recovered.evidence.status, "PROMOTED");
    assert.equal(recovered.evidence.workflow.runId, "36945783284");
    assert.equal(recovered.evidence.references.stageRunId, "36941014905");
    assert.equal(recovered.evidence.references.promotionRunId, null);
    assert.deepEqual(recovered.evidence.rollback, stageEvidence.rollback);
    assert.equal(recovered.evidence.runtime.result.api.revision, "boundarymesh-api-c-36941014905");
    assert.equal(recovered.evidence.hosting.result.version, "sites/boundarymesh-dev/versions/c9def09b61688b16");
});

test("evidence-only recovery refuses unsafe preconditions and collisions before publication", () => {
    const directory = mkdtempSync(join(tmpdir(), "bm-recover-fail-"));
    const failures = [
        [{ attempt: 2 }, /attempt 1/],
        [{ workflowSha: "1".repeat(40) }, /source SHA/],
        [{ jobs: { ...validProof().jobs, "promote-runtime": "failure" } }, /promote-runtime/],
        [{ jobs: { ...validProof().jobs, "rollback-runtime": "success" }, rollbackExecuted: true }, /rollback/],
        [{ promotionEvidenceExists: true }, /already exists/],
        [{ productionMatchesPromotedTargets: false }, /production does not match/],
        [{ rollbackTargetsExist: false }, /rollback targets/],
        [{ requiredStageRunId: "1" }, /stage run/],
        [{ runtimeServiceAccounts: { ...serviceAccounts, web: "wrong@boundarymesh-dev.iam.gserviceaccount.com" } }, /service account/],
    ];
    for (const [overrides, pattern] of failures) {
        assert.throws(() => recoverPromotionEvidence({
            stageEvidence,
            runtimeSnapshot,
            hostingSnapshot: promoteHosting.result,
            proof: validProof(overrides),
            outputDir: directory,
        }), pattern);
    }
    const wrongStage = structuredClone(stageEvidence);
    wrongStage.runtime.result.candidates.api.revision = "boundarymesh-api-c-1";
    assert.throws(() => buildRecoveredPromotionEvidence({
        stageEvidence: wrongStage,
        runtimeSnapshot,
        hostingSnapshot: promoteHosting.result,
        promotionWorkflow: { runId: "36945783284", runAttempt: "1", sha: "2326b488d722222de0966321f165215c30dcebe9", ref: "refs/heads/main" },
        references: { stageRunId: "36941014905" },
        startedAt: "2026-10-02T00:25:00.000Z",
        completedAt: "2026-10-02T00:28:59.000Z",
    }), /schema|staged candidate|workflow run|revision|zero production allocation/);
    let published = false;
    assert.throws(() => recoverPromotionEvidence({
        stageEvidence,
        runtimeSnapshot,
        hostingSnapshot: promoteHosting.result,
        proof: validProof(),
        outputDir: mkdtempSync(join(tmpdir(), "bm-recover-collision-")),
        publish: true,
        collisionRunner: () => ({ status: 0, stdout: "", stderr: "" }),
        publisher: () => { published = true; },
    }), /already exists/);
    assert.equal(published, false);
    assert.throws(() => assertEvidenceLookupAbsent(
        { status: 0, stdout: "", stderr: "" },
        { project: lock.project, location: lock.region, repository: lock.deploymentEvidence.repository, packageName: lock.deploymentEvidence.package, version: evidenceVersion("promote", "36945783284") },
        { project: lock.project, location: lock.region, repository: lock.deploymentEvidence.repository, packageName: lock.deploymentEvidence.package, version: evidenceVersion("promote", "36945783284") },
    ), /already exists/);
});

function verifiedProof(overrides = {}) {
    return buildVerifiedRecoveryProof({
        run: promotionRun,
        jobs: promotionJobs,
        stageRun,
        stageEvidence,
        runtimeSnapshot,
        hostingSnapshot: promoteHosting.result,
        runtimeServiceAccounts: serviceAccounts,
        health,
        productionMatchesPromotedTargets: true,
        rollbackTargetsExist: true,
        promotionEvidenceExists: false,
        requiredStageRunId: "36941014905",
        requiredPromotionRunId: "36945783284",
        ...overrides,
    });
}

test("recovery proof reconstruction binds original runs and refuses every unsafe precondition before publication", () => {
    const proof = verifiedProof();
    assert.equal(proof.runId, "36945783284");
    assert.equal(proof.stageRunId, "36941014905");
    assert.equal(proof.jobs["record-promote-evidence"], "failure");
    assert.equal(proof.rollbackExecuted, false);
    const recovered = recoverPromotionEvidence({
        stageEvidence,
        runtimeSnapshot,
        hostingSnapshot: promoteHosting.result,
        proof,
        outputDir: mkdtempSync(join(tmpdir(), "bm-recover-proof-")),
        publish: false,
    });
    assert.doesNotThrow(() => verifyRecoveredPromotionEvidence(recovered.evidence, {
        stageRunId: "36941014905",
        promotionRunId: "36945783284",
    }));
    const failures = [
        [{ run: { ...promotionRun, id: 1 } }, /promotion run/],
        [{ stageRun: { ...stageRun, id: 1 } }, /stage run/],
        [{ run: { ...promotionRun, run_attempt: 2 } }, /attempt 1/],
        [{ jobs: { jobs: promotionJobs.jobs.map((job) => job.name === "promote-runtime" ? { ...job, conclusion: "failure" } : job) } }, /promote-runtime/],
        [{ jobs: { jobs: promotionJobs.jobs.map((job) => job.name === "promote-hosting" ? { ...job, conclusion: "failure" } : job) } }, /promote-hosting/],
        [{ jobs: { jobs: promotionJobs.jobs.map((job) => job.name === "record-promote-evidence" ? { ...job, conclusion: "success" } : job) } }, /record-promote-evidence/],
        [{ jobs: { jobs: promotionJobs.jobs.map((job) => job.name === "rollback-runtime" ? { ...job, conclusion: "success" } : job) } }, /rollback/],
        [{ productionMatchesPromotedTargets: false }, /production does not match/],
        [{ promotionEvidenceExists: true }, /already exists/],
        [{ rollbackTargetsExist: false }, /rollback targets/],
        [{ runtimeSnapshot: { ...runtimeSnapshot, state: { ...runtimeSnapshot.state, api: { ...runtimeSnapshot.state.api, revision: "boundarymesh-api-00004-9hw" } } } }, /revision|production/],
        [{ stageEvidence: { ...stageEvidence, release: { ...stageEvidence.release, sourceSha: "0".repeat(40) } } }, /frozen release identity|source/],
        [{ health: { ...health, firebaseStatus: 503 } }, /Firebase/],
    ];
    for (const [overrides, pattern] of failures) {
        assert.throws(() => verifiedProof(overrides), pattern);
    }
});

test("recovery run #1 fails closed before evidence publication on Hosting USER_PROJECT_DENIED", () => {
    const diagnostic = {
        httpStatus: 403,
        error: {
            code: 403,
            status: "PERMISSION_DENIED",
            message: "Caller does not have required permission to use project boundarymesh-dev. Grant the caller the roles/serviceusage.serviceUsageConsumer role, or a custom role with the serviceusage.services.use permission, by visiting https://console.developers.google.com/iam-admin/iam?project=boundarymesh-dev and then retry. Propagation of the new permission may take a few minutes.",
        },
        errorInfo: [{
            reason: "USER_PROJECT_DENIED",
            domain: "googleapis.com",
            metadata: { service: "firebasehosting.googleapis.com", consumer: "projects/boundarymesh-dev" },
        }],
    };
    const error = new Error(`Firebase Hosting request failed: ${JSON.stringify(diagnostic)}`);
    error.name = "FirebaseHostingRequestError";
    error.diagnostic = diagnostic;
    assert.equal(isRecoveryRun1HostingQuotaDenial(error), true);
    let published = false;
    assert.throws(() => {
        if (isRecoveryRun1HostingQuotaDenial(error)) throw error;
        recoverPromotionEvidence({
            stageEvidence,
            runtimeSnapshot,
            hostingSnapshot: promoteHosting.result,
            proof: validProof(),
            outputDir: mkdtempSync(join(tmpdir(), "bm-recover-run1-")),
            publish: true,
            publisher: () => { published = true; },
        });
    }, /USER_PROJECT_DENIED|serviceusage\.services\.use|Firebase Hosting request failed/);
    assert.equal(published, false);
    assert.throws(() => recoverPromotionEvidence({
        stageEvidence,
        runtimeSnapshot,
        hostingSnapshot: promoteHosting.result,
        proof: validProof({ runId: FAILED_RECOVERY_RUN_1, requiredPromotionRunId: FAILED_RECOVERY_RUN_1, originalPromotionRunId: FAILED_RECOVERY_RUN_1 }),
        outputDir: mkdtempSync(join(tmpdir(), "bm-recover-failed-run-")),
        publish: true,
        publisher: () => { published = true; },
    }), /37003377107|promotion run/);
    assert.equal(published, false);
});

test("dual-principal Hosting handoff is hashed, run-scoped, and fail-closed", () => {
    const snapshot = promoteHosting.result;
    const sealed = sealHostingRecoveryHandoff(buildHostingRecoveryHandoff({
        snapshot,
        firebaseStatus: 200,
        recoveryRunId: "99900000001",
    }));
    const verified = verifyHostingRecoveryHandoff({
        payloadB64: sealed.payloadB64,
        sha256: sealed.sha256,
        byteLength: sealed.byteLength,
        recoveryRunId: "99900000001",
        stageEvidence,
    });
    assert.equal(verified.principal, "boundarymesh-hosting-deploy@boundarymesh-dev.iam.gserviceaccount.com");
    assert.equal(verified.snapshot.version, "sites/boundarymesh-dev/versions/c9def09b61688b16");
    assert.throws(() => verifyHostingRecoveryHandoff({
        payloadB64: sealed.payloadB64,
        sha256: "0".repeat(64),
        byteLength: sealed.byteLength,
        recoveryRunId: "99900000001",
        stageEvidence,
    }), /SHA-256/);
    assert.throws(() => verifyHostingRecoveryHandoff({
        payloadB64: sealed.payloadB64,
        sha256: sealed.sha256,
        byteLength: String(sealed.byteLength - 1),
        recoveryRunId: "99900000001",
        stageEvidence,
    }), /truncated|padded|byte length/);
    assert.throws(() => verifyHostingRecoveryHandoff({
        payloadB64: sealed.payloadB64,
        sha256: sealed.sha256,
        byteLength: sealed.byteLength,
        recoveryRunId: "99900000002",
        stageEvidence,
    }), /this recovery run/);
    assert.throws(() => verifyHostingRecoveryHandoff({
        payloadB64: sealed.payloadB64,
        sha256: sealed.sha256,
        byteLength: sealed.byteLength,
        recoveryRunId: FAILED_RECOVERY_RUN_1,
        stageEvidence,
    }), /37003377107|this recovery run/);
    const wrongPrincipal = sealHostingRecoveryHandoff({
        ...buildHostingRecoveryHandoff({ snapshot, firebaseStatus: 200, recoveryRunId: "99900000001" }),
        principal: "boundarymesh-deploy@boundarymesh-dev.iam.gserviceaccount.com",
    });
    assert.throws(() => verifyHostingRecoveryHandoff({
        payloadB64: wrongPrincipal.payloadB64,
        sha256: wrongPrincipal.sha256,
        byteLength: wrongPrincipal.byteLength,
        recoveryRunId: "99900000001",
        stageEvidence,
    }), /Hosting deploy identity/);
    const wrongVersion = sealHostingRecoveryHandoff(buildHostingRecoveryHandoff({
        snapshot: { ...snapshot, version: "sites/boundarymesh-dev/versions/1bce16129fb1a0d4" },
        firebaseStatus: 200,
        recoveryRunId: "99900000001",
    }));
    assert.throws(() => verifyHostingRecoveryHandoff({
        payloadB64: wrongVersion.payloadB64,
        sha256: wrongVersion.sha256,
        byteLength: wrongVersion.byteLength,
        recoveryRunId: "99900000001",
        stageEvidence,
    }), /baseline version|exact staged version/);
    assert.throws(() => buildHostingRecoveryHandoff({
        snapshot,
        firebaseStatus: 200,
        recoveryRunId: FAILED_RECOVERY_RUN_1,
    }), /37003377107/);
    const driftedRuntime = {
        ...runtimeSnapshot,
        state: {
            ...runtimeSnapshot.state,
            api: { ...runtimeSnapshot.state.api, revision: "boundarymesh-api-00004-9hw" },
        },
    };
    assert.throws(() => verifiedProof({ runtimeSnapshot: driftedRuntime }), /revision|production/);
});
