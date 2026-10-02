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
import { recoverPromotionEvidence } from "../recover-promotion-evidence.mjs";

const fixtures = join(import.meta.dirname, "fixtures");
const lock = loadFrozenRelease();
const stageEvidence = JSON.parse(readFileSync(join(fixtures, "run-8-stage-evidence.json"), "utf8"));
const promoteRuntime = JSON.parse(readFileSync(join(fixtures, "run-9-promote-runtime.json"), "utf8"));
const promoteHosting = JSON.parse(readFileSync(join(fixtures, "run-9-promote-hosting.json"), "utf8"));
const runtimeSnapshot = JSON.parse(readFileSync(join(fixtures, "run-9-runtime-snapshot.json"), "utf8"));
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
