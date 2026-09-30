import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import { existsSync, mkdtempSync, readFileSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { gzipSync } from "node:zlib";
import test from "node:test";
import {
    assertEtag,
    assertInvariantUnchanged,
    assertStageBaseline,
    canonicalJson,
    evidenceVersion,
    extractFrontendArchive,
    inspectFrontendArchive,
    loadFrozenRelease,
    materializeFrozenFirebaseConfig,
    runtimeInvariant,
    sha256Bytes,
    validateDispatchInputs,
    validateDeploymentEvidence,
    verifyEvidenceFiles,
    verifyFrozenReleaseBytes,
    verifyFrozenReleaseFile,
    writeDeploymentEvidence,
} from "../contract.mjs";
import { buildEvidence } from "../evidence.mjs";
import { assertEvidenceLookupAbsent, assertExactOciLookup } from "../artifacts.mjs";
import { assertHostingCommand, assertPreviewChannelLookupAbsent } from "../cloud-hosting.mjs";
import { activeProductionRevision, assertRuntimePreflightMatches } from "../cloud-runtime.mjs";

const fixture = join(import.meta.dirname, "fixtures", "release-manifest.json");
const lock = loadFrozenRelease();
const image = (name, digest) => `europe-west4-docker.pkg.dev/boundarymesh-dev/boundarymesh/${name}@${digest}`;
const traffic = (revision, percent = 100, tag = null) => [{ type: "TRAFFIC_TARGET_ALLOCATION_TYPE_REVISION", revision, percent, tag }];
const split = (revision, percent = 100) => [{ type: "INSTANCE_SPLIT_ALLOCATION_TYPE_REVISION", revision, percent, tag: null }];

function stageParts() {
    const apiImage = image("boundarymesh-api", lock.images.api);
    const webImage = image("boundarymesh-web-edge", lock.images.web);
    const workerImage = image("boundarymesh-worker", lock.images.worker);
    return {
        runtime: {
            before: {
                api: { revision: "boundarymesh-api-00004-9hw", image: apiImage, traffic: traffic("boundarymesh-api-00004-9hw") },
                web: { revision: "boundarymesh-web-00004-k48", image: webImage, traffic: traffic("boundarymesh-web-00004-k48") },
                worker: { revision: "boundarymesh-worker-00002-v6j", image: workerImage, split: split("boundarymesh-worker-00002-v6j") },
            },
            candidates: {
                api: { revision: "boundarymesh-api-c-100", image: apiImage, traffic: [...traffic("boundarymesh-api-00004-9hw"), ...traffic("boundarymesh-api-c-100", 0, "candidate-100")], health: { path: "/api/v1/health", status: 200 } },
                web: { revision: "boundarymesh-web-c-100", image: webImage, traffic: [...traffic("boundarymesh-web-00004-k48"), ...traffic("boundarymesh-web-c-100", 0, "candidate-100")], health: { path: "/actuator/health", status: 200 } },
                worker: { revision: "boundarymesh-worker-c-100", image: workerImage, split: [...split("boundarymesh-worker-00002-v6j"), ...split("boundarymesh-worker-c-100", 0)], ready: true },
            },
            checks: { iamUnchanged: true },
        },
        hosting: {
            before: { site: "boundarymesh-dev", release: "sites/boundarymesh-dev/releases/1", version: "sites/boundarymesh-dev/versions/1bce16129fb1a0d4", releaseTime: "2026-09-22T00:00:00Z" },
            result: { channel: "bm-100", release: "sites/boundarymesh-dev/channels/bm-100/releases/1", version: "sites/boundarymesh-dev/versions/preview100", url: "https://boundarymesh-dev--bm-100-abc.web.app" },
            checks: { liveUnchanged: true },
        },
    };
}

function validStageEvidence(runId = "100") {
    const parts = stageParts();
    return buildEvidence({
        operation: "stage",
        ...parts,
        workflow: { runId, runAttempt: "1", sha: "1".repeat(40), ref: "refs/heads/main" },
        references: { stageRunId: null, promotionRunId: null },
        startedAt: "2026-09-30T00:00:00Z",
        completedAt: "2026-09-30T00:01:00Z",
    });
}

function validPromotionEvidence(runId = "200") {
    const stage = validStageEvidence("100");
    return buildEvidence({
        operation: "promote",
        runtime: {
            before: stage.runtime.before,
            result: {
                api: { revision: stage.runtime.result.candidates.api.revision, image: stage.runtime.result.candidates.api.image, traffic: traffic(stage.runtime.result.candidates.api.revision) },
                web: { revision: stage.runtime.result.candidates.web.revision, image: stage.runtime.result.candidates.web.image, traffic: traffic(stage.runtime.result.candidates.web.revision) },
                worker: { revision: stage.runtime.result.candidates.worker.revision, image: stage.runtime.result.candidates.worker.image, split: split(stage.runtime.result.candidates.worker.revision) },
            },
            checks: { orderedPromotion: true },
        },
        hosting: {
            before: stage.hosting.before,
            result: { site: "boundarymesh-dev", release: "sites/boundarymesh-dev/releases/2", version: stage.hosting.result.version, releaseTime: "2026-09-30T00:02:00Z" },
            checks: { exactStagedVersion: true },
        },
        priorEvidence: stage,
        workflow: { runId, runAttempt: "1", sha: "2".repeat(40), ref: "refs/heads/main" },
        references: { stageRunId: "100", promotionRunId: null },
        startedAt: "2026-09-30T00:01:00Z",
        completedAt: "2026-09-30T00:02:00Z",
    });
}

test("actual frozen release manifest matches the exact immutable lock", () => {
    const manifest = verifyFrozenReleaseFile(fixture);
    assert.equal(manifest.source.commitSha, lock.sourceSha);
    assert.equal(manifest.publication.runId, lock.publicationRunId);
    assert.equal(manifest.artifacts.api.digest, lock.images.api);
    assert.equal(manifest.artifacts.worker.digest, lock.images.worker);
    assert.equal(manifest.artifacts.web.digest, lock.images.web);
    assert.equal(manifest.artifacts.frontend.sha256, lock.frontend.archiveSha256);
});

test("wrong manifest bytes fail the exact hash gate", () => {
    assert.throws(() => verifyFrozenReleaseBytes(Buffer.concat([readFileSync(fixture), Buffer.from(" ")])), /SHA-256/);
});

test("wrong source SHA fails after byte integrity is independently established", () => {
    const manifest = JSON.parse(readFileSync(fixture, "utf8"));
    manifest.source.commitSha = "f".repeat(40);
    const bytes = Buffer.from(canonicalJson(manifest));
    assert.throws(() => verifyFrozenReleaseBytes(bytes, { ...lock, releaseManifestSha256: sha256Bytes(bytes) }), /source SHA/);
});

test("mutable image tags and malformed digest identities are rejected", () => {
    const manifest = JSON.parse(readFileSync(fixture, "utf8"));
    manifest.artifacts.api.digest = "main";
    const bytes = Buffer.from(canonicalJson(manifest));
    assert.throws(() => verifyFrozenReleaseBytes(bytes, { ...lock, releaseManifestSha256: sha256Bytes(bytes), images: { ...lock.images, api: "main" } }), /schema|digest|field/i);
});

test("dispatch inputs are exact and never select release identity", () => {
    assert.deepEqual(validateDispatchInputs({ operation: "stage" }), { operation: "stage", stageRunId: null, promotionRunId: null });
    assert.deepEqual(validateDispatchInputs({ operation: "promote", stageRunId: "123" }), { operation: "promote", stageRunId: "123", promotionRunId: null });
    assert.deepEqual(validateDispatchInputs({ operation: "rollback", stageRunId: "123", promotionRunId: "456" }), { operation: "rollback", stageRunId: "123", promotionRunId: "456" });
    assert.throws(() => validateDispatchInputs({ operation: "promote" }), /stage_run_id/);
    assert.throws(() => validateDispatchInputs({ operation: "rollback", stageRunId: "123", promotionRunId: "latest" }), /explicit numeric/);
    assert.throws(() => validateDispatchInputs({ operation: "deploy" }), /exactly/);
});

test("runtime invariant comparison rejects unexpected configuration drift", () => {
    const base = { name: "service", ingress: "INGRESS_TRAFFIC_ALL", template: { serviceAccount: "sa", timeout: "30s", containers: [{ image: "old", env: [{ name: "A", value: "B" }], resources: { limits: { cpu: "1", memory: "512Mi" } } }] } };
    const imageOnly = structuredClone(base);
    imageOnly.template.containers[0].image = "new";
    assert.doesNotThrow(() => assertInvariantUnchanged(runtimeInvariant(base), runtimeInvariant(imageOnly)));
    const drift = structuredClone(imageOnly);
    drift.template.containers[0].env[0].value = "changed";
    assert.throws(() => assertInvariantUnchanged(runtimeInvariant(base), runtimeInvariant(drift)), /outside the deployment allowlist/);
});

test("Cloud Run production allocation normalizes full revision names and rejects ambiguous splits", () => {
    assert.equal(activeProductionRevision(traffic("projects/p/locations/r/services/boundarymesh-api/revisions/boundarymesh-api-00005-abc")), "boundarymesh-api-00005-abc");
    assert.equal(activeProductionRevision(split("boundarymesh-worker-00003-ghi")), "boundarymesh-worker-00003-ghi");
    assert.throws(() => activeProductionRevision([...traffic("one", 50), ...traffic("two", 50)]), /exactly one revision/);
});

test("Firebase Hosting config is materialized byte-for-byte from the frozen release source", () => {
    const directory = mkdtempSync(join(tmpdir(), "bm-firebase-config-"));
    const destination = join(directory, "firebase.json");
    materializeFrozenFirebaseConfig({ destination, sourceSha: lock.sourceSha });
    assert.ok(existsSync(destination));
    assert.equal(sha256Bytes(readFileSync(destination)), lock.firebase.configSha256);
    assert.throws(() => materializeFrozenFirebaseConfig({ destination: join(directory, "wrong.json"), sourceSha: "f".repeat(40) }), /source SHA/);
});

test("ETag and stale-stage checks fail closed", () => {
    assert.throws(() => assertEtag("one", "two"), /ETag mismatch/);
    const baseline = { api: { revision: "a" }, web: { revision: "w" }, worker: { revision: "x" }, hosting: { version: "h" } };
    assert.doesNotThrow(() => assertStageBaseline(structuredClone(baseline), baseline));
    const changed = structuredClone(baseline);
    changed.api.revision = "other";
    assert.throws(() => assertStageBaseline(changed, baseline), /Stale stage baseline/);
});

test("stage evidence is canonical, sidecar-bound, and exact-run addressable", () => {
    const evidence = validStageEvidence();
    assert.ok(Object.values(evidence.runtime.checks).every((value) => typeof value === "boolean"));
    assert.ok(Object.values(evidence.hosting.checks).every((value) => typeof value === "boolean"));
    assert.equal(evidenceVersion("stage", "100"), `${lock.sourceSha}-run-100`);
    const directory = mkdtempSync(join(tmpdir(), "bm-evidence-"));
    const paths = writeDeploymentEvidence(evidence, directory, { operation: "stage", runId: "100" });
    assert.equal(verifyEvidenceFiles(paths.documentPath, paths.sidecarPath, { operation: "stage", runId: "100" }).status, "STAGED");
    writeFileSync(paths.sidecarPath, `${"0".repeat(64)}  deployment-evidence.json\n`);
    assert.throws(() => verifyEvidenceFiles(paths.documentPath, paths.sidecarPath), /SHA-256/);
});

test("promotion and rollback evidence preserve exact transitions and rollback identities", () => {
    const promotion = validPromotionEvidence();
    assert.equal(promotion.status, "PROMOTED");
    assert.equal(promotion.references.stageRunId, "100");
    assert.deepEqual(promotion.rollback, validStageEvidence("100").rollback);
    const rollback = buildEvidence({
        operation: "rollback",
        runtime: { before: promotion.runtime.result, result: validStageEvidence("100").runtime.before, checks: { orderedRollback: true } },
        hosting: { before: promotion.hosting.result, result: validStageEvidence("100").hosting.before, checks: { exactRollbackVersion: true } },
        priorEvidence: promotion,
        workflow: { runId: "300", runAttempt: "1", sha: "3".repeat(40), ref: "refs/heads/main" },
        references: { stageRunId: "100", promotionRunId: "200" },
        startedAt: "2026-09-30T00:03:00Z",
        completedAt: "2026-09-30T00:04:00Z",
    });
    assert.equal(rollback.status, "ROLLED_BACK");
    assert.deepEqual(rollback.rollback, promotion.rollback);
    const stage = validStageEvidence("100");
    const mismatchedRuntime = structuredClone(promotion.runtime);
    mismatchedRuntime.result.api.revision = "unrelated-revision";
    assert.throws(() => buildEvidence({
        operation: "promote", runtime: mismatchedRuntime, hosting: promotion.hosting, priorEvidence: stage,
        workflow: { runId: "201", runAttempt: "1", sha: "4".repeat(40), ref: "refs/heads/main" },
        references: { stageRunId: "100", promotionRunId: null },
        startedAt: "2026-09-30T00:01:00Z", completedAt: "2026-09-30T00:02:00Z",
    }), /staged candidate/);
});

test("partial promotion evidence records only staged candidate/baseline states and remains rollback-capable", () => {
    const stage = validStageEvidence("100");
    const failed = buildEvidence({
        operation: "promote",
        outcome: "failed",
        runtime: {
            before: stage.runtime.before,
            result: {
                api: { revision: stage.runtime.result.candidates.api.revision, image: stage.runtime.result.candidates.api.image, traffic: traffic(stage.runtime.result.candidates.api.revision) },
                web: stage.runtime.before.web,
                worker: stage.runtime.before.worker,
            },
            checks: { promotionCompleted: false, observedAfterJobs: true },
        },
        hosting: { before: stage.hosting.before, result: stage.hosting.before, checks: { promotionCompleted: false, notAttempted: true } },
        priorEvidence: stage,
        workflow: { runId: "250", runAttempt: "1", sha: "5".repeat(40), ref: "refs/heads/main" },
        references: { stageRunId: "100", promotionRunId: null },
        startedAt: "2026-09-30T00:02:00Z",
        completedAt: "2026-09-30T00:03:00Z",
    });
    assert.equal(failed.status, "PROMOTION_FAILED");
    assert.equal(failed.checks.operationSucceeded, false);
    assert.equal(validateDeploymentEvidence(failed, { operation: "promote", runId: "250" }).status, "PROMOTION_FAILED");
});

test("wrong candidate and wrong prior run evidence are rejected", () => {
    const evidence = validStageEvidence();
    const wrongCandidate = structuredClone(evidence);
    wrongCandidate.runtime.result.candidates.api.image = image("boundarymesh-api", `sha256:${"0".repeat(64)}`);
    assert.throws(() => validateDeploymentEvidence(wrongCandidate, { operation: "stage", runId: "100" }), /schema|lock|evidence/i);
    assert.throws(() => validateDeploymentEvidence(evidence, { operation: "stage", runId: "101" }), /workflow run/);
});

function tarHeader(name, type = "0", link = "", size = 0) {
    const block = Buffer.alloc(512);
    const put = (value, offset, length) => block.write(String(value), offset, Math.min(length, Buffer.byteLength(String(value))), "ascii");
    put(name, 0, 100); put("0000644\0", 100, 8); put("0000000\0", 108, 8); put("0000000\0", 116, 8);
    put(`${size.toString(8).padStart(11, "0")}\0`, 124, 12); put("00000000000\0", 136, 12);
    block.fill(0x20, 148, 156); put(type, 156, 1); put(link, 157, 100); put("ustar\0", 257, 6); put("00", 263, 2);
    const checksum = [...block].reduce((sum, byte) => sum + byte, 0);
    put(`${checksum.toString(8).padStart(6, "0")}\0 `, 148, 8);
    return block;
}

function archive(entries) {
    const blocks = [];
    for (const entry of entries) {
        const data = Buffer.from(entry.data ?? "");
        blocks.push(tarHeader(entry.name, entry.type ?? "0", entry.link ?? "", data.length), data, Buffer.alloc((512 - data.length % 512) % 512));
    }
    blocks.push(Buffer.alloc(1024));
    return gzipSync(Buffer.concat(blocks));
}

test("frontend archive accepts only regular files beneath frontend-dist", async () => {
    const directory = mkdtempSync(join(tmpdir(), "bm-archive-"));
    const safe = join(directory, "safe.tar.gz");
    writeFileSync(safe, archive([{ name: "frontend-dist/index.html", data: "ok" }]));
    assert.equal((await inspectFrontendArchive(safe)).length, 1);
    const output = join(directory, "output");
    await extractFrontendArchive({ archivePath: safe, destination: output, expectedSha256: createHash("sha256").update(readFileSync(safe)).digest("hex") });
    const traversal = join(directory, "traversal.tar.gz");
    writeFileSync(traversal, archive([{ name: "frontend-dist/../../escape.txt", data: "bad" }]));
    await assert.rejects(() => inspectFrontendArchive(traversal), /Unsafe|unexpected/);
    const link = join(directory, "link.tar.gz");
    writeFileSync(link, archive([{ name: "frontend-dist/link", type: "2", link: "/etc/passwd" }]));
    await assert.rejects(() => inspectFrontendArchive(link), /links|special/);
});

test("wrong frontend archive hash fails before extraction", async () => {
    const directory = mkdtempSync(join(tmpdir(), "bm-archive-hash-"));
    const path = join(directory, "frontend.tar.gz");
    writeFileSync(path, archive([{ name: "frontend-dist/index.html", data: "ok" }]));
    await assert.rejects(() => extractFrontendArchive({ archivePath: path, destination: join(directory, "out"), expectedSha256: "0".repeat(64) }), /SHA-256/);
});

test("Firebase command fence rejects project/site lifecycle and project switching", () => {
    assert.doesNotThrow(() => assertHostingCommand(["hosting:channel:deploy", "bm-100", "--project", "boundarymesh-dev"]));
    assert.throws(() => assertHostingCommand(["hosting:sites:create", "other", "--project", "boundarymesh-dev"]), /Forbidden/);
    assert.throws(() => assertHostingCommand(["hosting:channel:deploy", "bm-100", "--project", "other"]), /frozen project/);
});

test("evidence collision lookup accepts only the exact canonical NOT_FOUND result", () => {
    const lookup = {
        project: lock.project,
        location: lock.region,
        repository: lock.deploymentEvidence.repository,
        packageName: lock.deploymentEvidence.package,
        version: evidenceVersion("stage", "100"),
    };
    const absent = { status: 1, stdout: "", stderr: "ERROR: NOT_FOUND: Requested entity was not found." };
    assert.deepEqual(assertEvidenceLookupAbsent(absent, lookup, lookup), { version: lookup.version, absent: true });
    assert.throws(() => assertEvidenceLookupAbsent({ status: 0, stdout: "{}", stderr: "" }, lookup, lookup), /already exists/);
    for (const stderr of [
        "ERROR: PERMISSION_DENIED: denied",
        "ERROR: UNAUTHENTICATED: login",
        "ERROR: UNAVAILABLE: network failure",
        "connection reset",
        "{}",
    ]) {
        assert.throws(() => assertEvidenceLookupAbsent({ status: 1, stdout: "", stderr }, lookup, lookup), /without proving absence|canonical trustworthy/);
    }
    assert.throws(() => assertEvidenceLookupAbsent(absent, { ...lookup, repository: "wrong" }, lookup), /exact frozen repository/);
    assert.throws(() => assertEvidenceLookupAbsent(absent, { ...lookup, packageName: "wrong" }, lookup), /exact frozen repository/);
});

test("OCI preflight accepts only the exact immutable digest identity", () => {
    const reference = image("boundarymesh-api", lock.images.api);
    const valid = { status: 0, stdout: JSON.stringify({ image_summary: { fully_qualified_digest: reference } }), stderr: "" };
    assert.equal(assertExactOciLookup(valid, reference), reference);
    assert.throws(() => assertExactOciLookup({ status: 1, stdout: "", stderr: "NOT_FOUND" }, reference), /lookup failed/);
    assert.throws(() => assertExactOciLookup({ status: 0, stdout: "not-json", stderr: "" }, reference), /malformed JSON/);
    assert.throws(() => assertExactOciLookup({ status: 0, stdout: JSON.stringify({ image_summary: { fully_qualified_digest: image("boundarymesh-api", `sha256:${"0".repeat(64)}`) } }), stderr: "" }, reference), /different digest/);
});

test("preview collision lookup accepts only exact-site canonical absence", () => {
    const lookup = { site: "boundarymesh-dev", channel: "bm-100", resource: "sites/boundarymesh-dev/channels/bm-100" };
    const absent = { status: 404, ok: false, payload: { error: { code: 404, status: "NOT_FOUND" } } };
    assert.deepEqual(assertPreviewChannelLookupAbsent(absent, lookup, lookup), { channel: "bm-100", absent: true });
    assert.throws(() => assertPreviewChannelLookupAbsent({ status: 200, ok: true, payload: { name: lookup.resource } }, lookup, lookup), /already exists/);
    assert.throws(() => assertPreviewChannelLookupAbsent({ status: 200, ok: true, payload: { name: "sites/boundarymesh-dev/channels/other" } }, lookup, lookup), /wrong project\/site/);
    assert.throws(() => assertPreviewChannelLookupAbsent({ status: 403, ok: false, payload: { error: { code: 403, status: "PERMISSION_DENIED" } } }, lookup, lookup), /without proving exact absence/);
    assert.throws(() => assertPreviewChannelLookupAbsent({ status: 404, ok: false, payload: {} }, lookup, lookup), /without proving exact absence/);
    assert.throws(() => assertPreviewChannelLookupAbsent(absent, { ...lookup, site: "other" }, lookup), /exact frozen site/);
    const differentChannel = { status: 404, ok: false, payload: { error: { code: 404, status: "NOT_FOUND" } } };
    assert.doesNotThrow(() => assertPreviewChannelLookupAbsent(differentChannel, lookup, lookup));
});

test("mutation-time runtime baseline requires exact preflight ETags and fingerprints", () => {
    const approved = {
        baseline: { api: { revision: "a" }, web: { revision: "w" }, worker: { revision: "x" } },
        etags: { api: "1", web: "2", worker: "3" },
        invariants: { api: "a", web: "b", worker: "c" },
    };
    assert.doesNotThrow(() => assertRuntimePreflightMatches(structuredClone(approved), approved));
    const changed = structuredClone(approved);
    changed.etags.api = "changed";
    assert.throws(() => assertRuntimePreflightMatches(changed, approved), /changed after global stage preflight/);
});
