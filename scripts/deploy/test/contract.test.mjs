import assert from "node:assert/strict";
import { execFileSync } from "node:child_process";
import { createHash } from "node:crypto";
import { cpSync, existsSync, mkdirSync, mkdtempSync, readFileSync, rmSync, symlinkSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
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
    repositoryRoot,
    sha256Bytes,
    validateDispatchInputs,
    validateRecoveryDispatchInputs,
    validateDeploymentEvidence,
    verifyEvidenceFiles,
    verifyFrozenReleaseBytes,
    verifyFrozenReleaseFile,
    writeDeploymentEvidence,
} from "../contract.mjs";
import { buildEvidence } from "../evidence.mjs";
import { assertEvidenceLookupAbsent, assertExactOciLookup } from "../artifacts.mjs";
import {
    assertAuthoritativePreviewResult,
    assertHostingCommand,
    assertPreviewChannelLookupAbsent,
    deployPreview,
    firebaseHostingResponseValue,
    sanitizeFirebaseHostingError,
} from "../cloud-hosting.mjs";
import {
    activeProductionRevision,
    assertOciPreflightMatches,
    assertRevisionImage,
    assertRuntimePreflightMatches,
    cloudRunResponseValue,
    health,
    requireHealthIdToken,
    sanitizeCloudRunError,
    selectRuntimeManifest,
} from "../cloud-runtime.mjs";
import { runChecked, sanitizeChildProcessDiagnostics } from "../process.mjs";

const fixture = join(import.meta.dirname, "fixtures", "release-manifest.json");
const lock = loadFrozenRelease();
const image = (name, digest) => `europe-west4-docker.pkg.dev/boundarymesh-dev/boundarymesh/${name}@${digest}`;
const traffic = (revision, percent = 100, tag = null) => [{ type: "TRAFFIC_TARGET_ALLOCATION_TYPE_REVISION", revision, percent, tag }];
const split = (revision, percent = 100) => [{ type: "INSTANCE_SPLIT_ALLOCATION_TYPE_REVISION", revision, percent, tag: null }];
const runtimeDigests = {
    api: "sha256:41e70a6fc2acd7716b91de754f28e4ac4f1a9b1cb05467a44d1f7986f2c2eff9",
    web: "sha256:3c6c39778e5df4445ad9e71db5e0ff188dfd751094b5fb8cdc520bfdbd050109",
    worker: "sha256:32e4c4aebc758f01053d3488e35e80d0294ce59e13031fb2a55c71dbaac426ed",
};
const imageResolution = (subject) => ({
    parentDigest: lock.images[subject],
    parentMediaType: "application/vnd.oci.image.index.v1+json",
    runtimeDigest: runtimeDigests[subject],
    runtimeMediaType: "application/vnd.oci.image.manifest.v1+json",
    platform: { os: "linux", architecture: "amd64" },
    relationship: "oci-index-child",
});

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
                api: { revision: "boundarymesh-api-c-100", image: apiImage, imageResolution: imageResolution("api"), traffic: [...traffic("boundarymesh-api-00004-9hw"), ...traffic("boundarymesh-api-c-100", 0, "candidate-100")], health: { path: "/api/v1/health", status: 200 } },
                web: { revision: "boundarymesh-web-c-100", image: webImage, imageResolution: imageResolution("web"), traffic: [...traffic("boundarymesh-web-00004-k48"), ...traffic("boundarymesh-web-c-100", 0, "candidate-100")], health: { path: "/actuator/health", status: 200 } },
                worker: { revision: "boundarymesh-worker-c-100", image: workerImage, imageResolution: imageResolution("worker"), split: [...split("boundarymesh-worker-00002-v6j"), ...split("boundarymesh-worker-c-100", 0)], ready: true },
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
    assert.deepEqual(validateRecoveryDispatchInputs({ stageRunId: "36941014905", promotionRunId: "36945783284" }), {
        stageRunId: "36941014905",
        promotionRunId: "36945783284",
    });
    assert.throws(() => validateRecoveryDispatchInputs({ stageRunId: "latest", promotionRunId: "36945783284" }), /explicit numeric/);
    assert.throws(() => validateRecoveryDispatchInputs({ stageRunId: "36941014905", promotionRunId: "36941014905" }), /distinct/);
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

test("Firebase Hosting config is materialized byte-for-byte from the vendored frozen release", () => {
    const contractSource = readFileSync(fileURLToPath(new URL("../contract.mjs", import.meta.url)), "utf8");
    assert.doesNotMatch(contractSource, /cat-file/);
    const directory = mkdtempSync(join(tmpdir(), "bm-firebase-config-"));
    const destination = join(directory, "firebase.json");
    materializeFrozenFirebaseConfig({ destination, sourceSha: lock.sourceSha });
    assert.ok(existsSync(destination));
    assert.equal(sha256Bytes(readFileSync(destination)), lock.firebase.configSha256);
    assert.equal(sha256Bytes(readFileSync(join(repositoryRoot, lock.firebase.vendoredPath))), lock.firebase.configSha256);
    const workingHosting = JSON.parse(readFileSync(join(repositoryRoot, "firebase.json"), "utf8")).hosting;
    assert.deepEqual(workingHosting.redirects, [
        { source: "/explore", destination: "/", type: 301 },
        { source: "/explore/**", destination: "/", type: 301 },
    ]);
    assert.equal(workingHosting.rewrites.some((rule) => rule.source === "/explore"), false);
    assert.equal(workingHosting.rewrites.find((rule) => rule.source === "/operator").destination, "/operator.html");
    assert.equal(workingHosting.rewrites.at(-1).destination, "/index.html");
    assert.equal(
        sha256Bytes(readFileSync(join(repositoryRoot, "scripts/deploy/release/frozen/firebase.a876a7bfa5fa86f88e8387efb557773281daa6e6.json"))),
        "34d9f2a073429420b6a15b9133486096fba75a1255ec1f3431fda5b2fd5a242c",
    );
    assert.throws(() => materializeFrozenFirebaseConfig({ destination: join(directory, "wrong.json"), sourceSha: "f".repeat(40) }), /source SHA/);
});

test("frozen Firebase reconstruction fails closed without Git history or the current config", () => {
    const frozenDir = join(repositoryRoot, "scripts", "deploy", "release", "frozen");
    const scratch = [];
    const writeVendor = (sourceSha, bytes) => {
        const name = `firebase.${sourceSha}.json`;
        const path = join(frozenDir, name);
        writeFileSync(path, bytes);
        scratch.push(path);
        return `scripts/deploy/release/frozen/${name}`;
    };
    try {
        const missingSha = "1".repeat(40);
        assert.throws(() => materializeFrozenFirebaseConfig({
            destination: join(tmpdir(), "missing.json"),
            sourceSha: missingSha,
            lock: { ...lock, sourceSha: missingSha, firebase: { ...lock.firebase, vendoredPath: `scripts/deploy/release/frozen/firebase.${missingSha}.json` } },
        }), /missing/);
        const wrongSha = "2".repeat(40);
        const wrongPath = writeVendor(wrongSha, Buffer.from("{}\n"));
        assert.throws(() => materializeFrozenFirebaseConfig({
            destination: join(tmpdir(), "wrong-bytes.json"),
            sourceSha: wrongSha,
            lock: { ...lock, sourceSha: wrongSha, firebase: { ...lock.firebase, vendoredPath: wrongPath, configSha256: "3".repeat(64) } },
        }), /SHA-256/);
        const substitutedSha = "4".repeat(40);
        const substitutedPath = writeVendor(substitutedSha, readFileSync(join(repositoryRoot, "scripts/deploy/release/frozen/firebase.a876a7bfa5fa86f88e8387efb557773281daa6e6.json")));
        assert.throws(() => materializeFrozenFirebaseConfig({
            destination: join(tmpdir(), "substituted.json"),
            sourceSha: substitutedSha,
            lock: { ...lock, sourceSha: substitutedSha, firebase: { ...lock.firebase, vendoredPath: substitutedPath } },
        }), /SHA-256/);
        assert.throws(() => materializeFrozenFirebaseConfig({
            destination: join(tmpdir(), "escaped.json"),
            sourceSha: lock.sourceSha,
            lock: { ...lock, firebase: { ...lock.firebase, vendoredPath: "firebase.json" } },
        }), /historical source SHA|escapes/);
        assert.throws(() => materializeFrozenFirebaseConfig({
            destination: join(tmpdir(), "renamed.json"),
            sourceSha: "5".repeat(40),
            lock: { ...lock, sourceSha: "5".repeat(40), firebase: { ...lock.firebase } },
        }), /historical source SHA/);
        assert.throws(() => materializeFrozenFirebaseConfig({
            destination: join(tmpdir(), "malformed.json"),
            sourceSha: "zz",
            lock: { ...lock, sourceSha: "zz", firebase: { ...lock.firebase, vendoredPath: "scripts/deploy/release/frozen/firebase.zz.json" } },
        }), /malformed/);
    } finally {
        for (const path of scratch) rmSync(path, { force: true });
    }
});

test("a single-commit repository can reconstruct the frozen Firebase config", () => {
    const directory = mkdtempSync(join(tmpdir(), "bm-single-branch-"));
    try {
        for (const relativePath of [
            "scripts/deploy/contract.mjs",
            "scripts/deploy/frozen-release.json",
            lock.firebase.vendoredPath,
            "scripts/release/release-manifest.mjs",
            "contracts/release-manifest/v1.schema.json",
            "contracts/deployment-evidence/v1.schema.json",
        ]) {
            const destination = join(directory, relativePath);
            mkdirSync(dirname(destination), { recursive: true });
            cpSync(join(repositoryRoot, relativePath), destination);
        }
        symlinkSync(join(repositoryRoot, "scripts", "deploy", "node_modules"), join(directory, "node_modules"), "junction");
        execFileSync("git", ["init", "-b", "main"], { cwd: directory, stdio: "pipe" });
        execFileSync("git", ["add", "scripts", "contracts"], { cwd: directory, stdio: "pipe" });
        execFileSync("git", ["-c", "user.email=boundarymesh-test@example.com", "-c", "user.name=BoundaryMesh Test", "commit", "-m", "snapshot"], { cwd: directory, stdio: "pipe" });
        assert.equal(execFileSync("git", ["rev-list", "--count", "HEAD"], { cwd: directory, encoding: "utf8" }).trim(), "1");
        assert.throws(() => execFileSync("git", ["cat-file", "-e", `${lock.sourceSha}^{commit}`], { cwd: directory, stdio: "pipe" }));
        const script = `
            import { mkdirSync, readFileSync } from "node:fs";
            import { materializeFrozenFirebaseConfig, loadFrozenRelease, sha256Bytes } from "./scripts/deploy/contract.mjs";
            const lock = loadFrozenRelease();
            mkdirSync("./out", { recursive: true });
            materializeFrozenFirebaseConfig({ destination: "./out/firebase.json", sourceSha: lock.sourceSha });
            if (sha256Bytes(readFileSync("./out/firebase.json")) !== "${"93458d9cdc3ad454f064fdc13fe405f405afa761a8d2da010ba205580d3bd983"}") process.exit(2);
        `;
        execFileSync(process.execPath, ["--input-type=module", "-e", script], { cwd: directory, stdio: "pipe" });
    } finally {
        rmSync(join(directory, "node_modules"), { force: true });
        rmSync(directory, { recursive: true, force: true });
    }
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

function previewRestState({ status = "FINALIZED", channelId = "bm-100", versionSite = "boundarymesh-dev", url = "https://boundarymesh-dev--bm-100-abc.web.app" } = {}) {
    const versionName = `sites/${versionSite}/versions/preview100`;
    return {
        channel: {
            name: `sites/boundarymesh-dev/channels/${channelId}`,
            url,
            release: {
                name: `sites/boundarymesh-dev/channels/${channelId}/releases/release100`,
                version: { name: versionName },
            },
        },
        version: {
            name: versionName,
            status,
            ...(status === "FINALIZED" ? { finalizeTime: "2026-10-01T18:41:37Z" } : {}),
        },
    };
}

test("Hosting deploy ignores CLI JSON shape and trusts exact FINALIZED REST state", async () => {
    const valid = previewRestState();
    let cliCalls = 0;
    const requested = [];
    const result = await deployPreview("100", {
        workDirectory: "/tmp/hosting",
        cliDeploy: () => {
            cliCalls += 1;
            return { undocumented: { changed: "shape" } };
        },
        request: async (path) => {
            requested.push(path);
            return path.includes("/versions/") ? valid.version : valid.channel;
        },
    });
    assert.equal(cliCalls, 1);
    assert.deepEqual(requested, [
        "sites/boundarymesh-dev/channels/bm-100",
        "sites/boundarymesh-dev/versions/preview100",
    ]);
    assert.deepEqual(result, {
        channel: "bm-100",
        release: "sites/boundarymesh-dev/channels/bm-100/releases/release100",
        version: "sites/boundarymesh-dev/versions/preview100",
        url: "https://boundarymesh-dev--bm-100-abc.web.app",
    });
});

test("Hosting authoritative result rejects missing, wrong, or non-finalized REST resources", () => {
    const valid = previewRestState();
    assert.throws(() => assertAuthoritativePreviewResult({ channel: null, version: valid.version, runId: "100", site: "boundarymesh-dev" }), /wrong preview channel/);
    assert.throws(() => assertAuthoritativePreviewResult({ channel: { ...valid.channel, release: undefined }, version: valid.version, runId: "100", site: "boundarymesh-dev" }), /no exact immutable release/);
    assert.throws(() => assertAuthoritativePreviewResult({ ...valid, channel: { ...valid.channel, name: "sites/boundarymesh-dev/channels/bm-999" }, runId: "100", site: "boundarymesh-dev" }), /wrong preview channel/);
    assert.throws(() => assertAuthoritativePreviewResult({ ...valid, channel: { ...valid.channel, url: undefined }, runId: "100", site: "boundarymesh-dev" }), /valid URL/);
    assert.throws(() => assertAuthoritativePreviewResult({ ...valid, version: { ...valid.version, name: "sites/other/versions/preview100" }, runId: "100", site: "boundarymesh-dev" }), /wrong released version/);
    for (const status of ["CREATED", "ABANDONED"]) {
        const state = previewRestState({ status });
        assert.throws(() => assertAuthoritativePreviewResult({ ...state, runId: "100", site: "boundarymesh-dev" }), /not FINALIZED/);
    }
    assert.doesNotThrow(() => assertAuthoritativePreviewResult({ ...valid, runId: "100", site: "boundarymesh-dev" }));
});

test("Hosting CLI failure aborts before authoritative REST classification", async () => {
    let restCalls = 0;
    await assert.rejects(() => deployPreview("100", {
        workDirectory: "/tmp/hosting",
        cliDeploy: () => { throw new Error("Firebase CLI failed."); },
        request: async () => {
            restCalls += 1;
            return {};
        },
    }), /Firebase CLI failed/);
    assert.equal(restCalls, 0);
});

test("health token contract separates base audience from tagged request endpoint", async () => {
    const token = "header.payload.signature";
    const calls = [];
    const result = await health("https://candidate-100---boundarymesh-api.example.run.app", {
        audience: "https://boundarymesh-api.example.run.app",
        path: "/api/v1/health",
        token,
        fetchImpl: async (url, options) => {
            calls.push({ url: String(url), options });
            return { ok: true, status: 200 };
        },
    });
    assert.equal(calls[0].url, "https://candidate-100---boundarymesh-api.example.run.app/api/v1/health");
    assert.equal(calls[0].options.headers.Authorization, `Bearer ${token}`);
    assert.deepEqual(result, { path: "/api/v1/health", status: 200 });
    assert.doesNotMatch(JSON.stringify(result), new RegExp(token.replace(/\./g, "\\.")));
});

test("health token failures are fail-closed before requests and non-2xx is rejected", async () => {
    assert.throws(() => requireHealthIdToken({}), /missing or malformed/);
    let calls = 0;
    const previous = process.env.BOUNDARYMESH_API_HEALTH_ID_TOKEN;
    delete process.env.BOUNDARYMESH_API_HEALTH_ID_TOKEN;
    try {
        await assert.rejects(() => health("https://candidate.example.run.app", {
            audience: "https://service.example.run.app",
            path: "/api/v1/health",
            fetchImpl: async () => {
                calls += 1;
                return { ok: true, status: 200 };
            },
        }), /missing or malformed/);
    } finally {
        if (previous === undefined) delete process.env.BOUNDARYMESH_API_HEALTH_ID_TOKEN;
        else process.env.BOUNDARYMESH_API_HEALTH_ID_TOKEN = previous;
    }
    assert.equal(calls, 0);
    await assert.rejects(() => health("https://candidate.example.run.app", {
        audience: "https://service.example.run.app",
        path: "/api/v1/health",
        token: "header.payload.signature",
        fetchImpl: async () => ({ ok: false, status: 503 }),
    }), /HTTP 503/);
});

test("Firebase diagnostics extract only allowlisted Google ErrorInfo fields", () => {
    const diagnostic = sanitizeFirebaseHostingError({
        httpStatus: 403,
        payload: {
            error: {
                code: 403,
                status: "PERMISSION_DENIED",
                message: "The caller lacks a required permission.",
                details: [{
                    "@type": "type.googleapis.com/google.rpc.ErrorInfo",
                    reason: "IAM_PERMISSION_DENIED",
                    domain: "googleapis.com",
                    metadata: {
                        // Hypothetical parser fixture, not observed run evidence.
                        permission: "serviceusage.services.use",
                        service: "firebasehosting.googleapis.com",
                        resource: "projects/boundarymesh-dev",
                        consumer: "projects/boundarymesh-dev",
                        quota_project: "boundarymesh-dev",
                        project: "boundarymesh-dev",
                        site: "boundarymesh-dev",
                        internal_debug_context: "must-not-appear",
                    },
                }],
            },
        },
    });
    assert.deepEqual(diagnostic, {
        httpStatus: 403,
        error: {
            code: 403,
            status: "PERMISSION_DENIED",
            message: "The caller lacks a required permission.",
        },
        errorInfo: [{
            reason: "IAM_PERMISSION_DENIED",
            domain: "googleapis.com",
            metadata: {
                service: "firebasehosting.googleapis.com",
                permission: "serviceusage.services.use",
                resource: "projects/boundarymesh-dev",
                consumer: "projects/boundarymesh-dev",
                quota_project: "boundarymesh-dev",
                project: "boundarymesh-dev",
                site: "boundarymesh-dev",
            },
        }],
    });
});

test("Firebase diagnostics support message-only errors and ignore unknown detail types", () => {
    const diagnostic = sanitizeFirebaseHostingError({
        httpStatus: 403,
        payload: {
            error: {
                message: "Request denied.",
                details: [
                    { "@type": "type.googleapis.com/google.rpc.DebugInfo", detail: "unsafe-debug-detail" },
                    null,
                    "malformed",
                ],
            },
        },
    });
    assert.deepEqual(diagnostic, { httpStatus: 403, error: { message: "Request denied." } });
    assert.doesNotMatch(JSON.stringify(diagnostic), /unsafe-debug-detail/);
});

test("Firebase diagnostics bound messages and sanitize control characters", () => {
    const diagnostic = sanitizeFirebaseHostingError({
        httpStatus: 403,
        payload: { error: { message: `${"x".repeat(2500)}\nforged-log-line\u0000` } },
    });
    assert.equal(diagnostic.error.message.length, 2048);
    assert.doesNotMatch(diagnostic.error.message, /[\u0000-\u001f\u007f]/);
});

test("Firebase diagnostics fail closed for missing payloads and network errors", () => {
    assert.deepEqual(sanitizeFirebaseHostingError({ httpStatus: 503 }), { httpStatus: 503, error: {} });
    let networkError;
    assert.throws(
        () => firebaseHostingResponseValue({ ok: false, networkError: new TypeError("fetch failed") }),
        (error) => {
            networkError = error;
            return error.name === "FirebaseHostingRequestError";
        },
    );
    assert.deepEqual(networkError.diagnostic, { httpStatus: null, error: { message: "fetch failed" } });
    assert.match(networkError.message, /Firebase Hosting request failed/);
});

test("Firebase diagnostics never disclose credentials, headers, or unknown payload fields", () => {
    const sentinels = [
        "bearer-secret-123",
        "private-key-secret-456",
        "service-account-secret@example.test",
        "github-token-secret-789",
        "header-secret-abc",
        "environment-secret-def",
        "unknown-metadata-secret-ghi",
        "ya29.access-token-secret",
        "github_pat_token_secret",
        "eyJheader.payload.signature",
        "pem-private-key-secret",
    ];
    let captured;
    assert.throws(
        () => firebaseHostingResponseValue({
            status: 403,
            ok: false,
            payload: {
                error: {
                    code: 403,
                    status: "PERMISSION_DENIED",
                    message: `Authorization: Bearer ${sentinels[0]} private_key=${sentinels[1]} client_email=${sentinels[2]} github_token=${sentinels[3]} ${sentinels[7]} ${sentinels[8]} ${sentinels[9]} -----BEGIN PRIVATE KEY-----${sentinels[10]}-----END PRIVATE KEY-----`,
                    details: [{
                        "@type": "type.googleapis.com/google.rpc.ErrorInfo",
                        reason: "DENIED",
                        domain: "googleapis.com",
                        metadata: { unsafe: sentinels[6] },
                    }],
                },
                headers: { Authorization: `Bearer ${sentinels[4]}` },
                credential: { private_key: sentinels[1], client_email: sentinels[2] },
                env: { GITHUB_TOKEN: sentinels[5] },
            },
        }),
        (error) => {
            captured = error;
            return error.name === "FirebaseHostingRequestError";
        },
    );
    const rendered = `${captured.message}\n${JSON.stringify(captured.diagnostic)}`;
    for (const sentinel of sentinels) assert.doesNotMatch(rendered, new RegExp(sentinel.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")));
    assert.doesNotMatch(rendered, /Bearer bearer-secret/);
});

test("Firebase success responses remain unchanged", () => {
    const payload = { name: "projects/boundarymesh-dev/sites/boundarymesh-dev" };
    assert.equal(firebaseHostingResponseValue({ status: 200, ok: true, payload }), payload);
});

test("Cloud Run diagnostics extract only allowlisted Google ErrorInfo fields", () => {
    const diagnostic = sanitizeCloudRunError({
        httpStatus: 403,
        payload: {
            error: {
                code: 403,
                status: "PERMISSION_DENIED",
                message: "The caller cannot poll this operation.",
                details: [{
                    "@type": "type.googleapis.com/google.rpc.ErrorInfo",
                    reason: "IAM_PERMISSION_DENIED",
                    domain: "googleapis.com",
                    metadata: {
                        service: "run.googleapis.com",
                        permission: "run.operations.get",
                        resource: "projects/boundarymesh-dev/locations/europe-west4/operations/operation-1",
                        consumer: "projects/boundarymesh-dev",
                        quota_project: "boundarymesh-dev",
                        project: "boundarymesh-dev",
                        location: "europe-west4",
                        operation: "operation-1",
                        unsafe_debug_context: "must-not-appear",
                    },
                }],
            },
        },
    });
    assert.deepEqual(diagnostic, {
        httpStatus: 403,
        error: {
            code: 403,
            status: "PERMISSION_DENIED",
            message: "The caller cannot poll this operation.",
        },
        errorInfo: [{
            reason: "IAM_PERMISSION_DENIED",
            domain: "googleapis.com",
            metadata: {
                service: "run.googleapis.com",
                permission: "run.operations.get",
                resource: "projects/boundarymesh-dev/locations/europe-west4/operations/operation-1",
                consumer: "projects/boundarymesh-dev",
                quota_project: "boundarymesh-dev",
                project: "boundarymesh-dev",
                location: "europe-west4",
                operation: "operation-1",
            },
        }],
    });
    assert.doesNotMatch(JSON.stringify(diagnostic), /must-not-appear/);
});

test("Cloud Run diagnostics support message-only errors and ignore malformed or unknown details", () => {
    const diagnostic = sanitizeCloudRunError({
        httpStatus: 403,
        payload: {
            error: {
                message: "Request denied.",
                details: [
                    { "@type": "type.googleapis.com/google.rpc.DebugInfo", detail: "unsafe" },
                    null,
                    "malformed",
                    { "@type": "type.googleapis.com/google.rpc.ErrorInfo", metadata: [] },
                ],
            },
        },
    });
    assert.deepEqual(diagnostic, { httpStatus: 403, error: { message: "Request denied." } });
});

test("Cloud Run diagnostics bound messages and sanitize control characters", () => {
    const diagnostic = sanitizeCloudRunError({
        httpStatus: 403,
        payload: { error: { message: `${"x".repeat(2500)}\nforged\u0000line` } },
    });
    assert.equal(diagnostic.error.message.length, 2048);
    assert.doesNotMatch(diagnostic.error.message, /[\u0000-\u001f\u007f]/);
});

test("Cloud Run diagnostics fail closed for missing payloads and preserve success values", () => {
    assert.deepEqual(sanitizeCloudRunError({ httpStatus: 503 }), { httpStatus: 503, error: {} });
    let failure;
    assert.throws(
        () => cloudRunResponseValue({ status: 403, ok: false }),
        (error) => {
            failure = error;
            return error.name === "CloudRunRequestError";
        },
    );
    assert.deepEqual(failure.diagnostic, { httpStatus: 403, error: {} });
    const payload = { name: "projects/p/locations/r/operations/o" };
    assert.equal(cloudRunResponseValue({ status: 200, ok: true, payload }), payload);
});

test("Cloud Run diagnostics never disclose credentials, headers, or unknown payload fields", () => {
    const sentinels = [
        "bearer-runtime-secret",
        "private-runtime-secret",
        "runtime-secret@example.test",
        "github_pat_runtime_secret",
        "unknown-runtime-secret",
        "ya29.runtime-token-secret",
        "eyJruntime.header.signature",
        "pem-runtime-secret",
    ];
    let captured;
    assert.throws(
        () => cloudRunResponseValue({
            status: 403,
            ok: false,
            payload: {
                error: {
                    code: 403,
                    message: `Authorization: Bearer ${sentinels[0]} private_key=${sentinels[1]} client_email=${sentinels[2]} github_token=${sentinels[3]} ${sentinels[5]} ${sentinels[6]} -----BEGIN PRIVATE KEY-----${sentinels[7]}-----END PRIVATE KEY-----`,
                    details: [{
                        "@type": "type.googleapis.com/google.rpc.ErrorInfo",
                        metadata: { unsafe: sentinels[4] },
                    }],
                },
                headers: { Authorization: `Bearer ${sentinels[0]}` },
                env: { PRIVATE_KEY: sentinels[1] },
            },
        }),
        (error) => {
            captured = error;
            return error.name === "CloudRunRequestError";
        },
    );
    const rendered = `${captured.message}\n${JSON.stringify(captured.diagnostic)}`;
    for (const sentinel of sentinels) {
        assert.doesNotMatch(rendered, new RegExp(sentinel.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")));
    }
});

test("Firebase child diagnostics retain useful stderr, stdout, both streams, and empty output", () => {
    assert.deepEqual(
        sanitizeChildProcessDiagnostics({ stderr: "Error: permission denied" }),
        { stderr: "Error: permission denied" },
    );
    assert.deepEqual(
        sanitizeChildProcessDiagnostics({ stdout: JSON.stringify({ status: "PERMISSION_DENIED" }) }),
        { stdout: JSON.stringify({ status: "PERMISSION_DENIED" }) },
    );
    assert.deepEqual(
        sanitizeChildProcessDiagnostics({ stdout: "stdout detail", stderr: "stderr detail" }),
        { stdout: "stdout detail", stderr: "stderr detail" },
    );
    assert.deepEqual(sanitizeChildProcessDiagnostics({ stdout: "", stderr: "" }), {});
});

test("Firebase child diagnostics bound output and normalize control characters", () => {
    const diagnostic = sanitizeChildProcessDiagnostics({
        stdout: `${"x".repeat(5000)}\nforged\u0000line`,
        stderr: `${"y".repeat(5000)}\r\nother`,
    });
    assert.equal(diagnostic.stdout.length, 4096);
    assert.equal(diagnostic.stderr.length, 4096);
    assert.doesNotMatch(`${diagnostic.stdout}${diagnostic.stderr}`, /[\u0000-\u001f\u007f]/);
});

test("Firebase child diagnostics redact token, key, credential, cookie, and API-key sentinels", () => {
    const sentinels = [
        "child-bearer-secret",
        "eyJchild.header.signature",
        "child-private-key-secret",
        "child-credential-secret",
        "child-cookie-secret",
        "AIza1234567890abcdefghijklmnopqrstuvwxyz",
        "github_pat_child_secret",
        "ya29.child-oauth-secret",
    ];
    const diagnostic = sanitizeChildProcessDiagnostics({
        stdout: `Authorization: Bearer ${sentinels[0]} JWT ${sentinels[1]} api_key=${sentinels[5]} github_token=${sentinels[6]} ${sentinels[7]}`,
        stderr: `-----BEGIN PRIVATE KEY-----${sentinels[2]}-----END PRIVATE KEY----- {"private_key":"${sentinels[3]}"} Cookie: ${sentinels[4]}`,
    });
    const rendered = JSON.stringify(diagnostic);
    for (const sentinel of sentinels) {
        assert.doesNotMatch(rendered, new RegExp(sentinel.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")));
    }
    assert.match(rendered, /REDACTED/);
});

test("Firebase CLI failure handling opts into sanitized child diagnostics only", () => {
    const source = readFileSync(join(import.meta.dirname, "..", "cloud-hosting.mjs"), "utf8");
    assert.match(source, /capture:\s*true,[\s\S]*?sanitizeFailureOutput:\s*true/);
    assert.equal((source.match(/sanitizeFailureOutput:\s*true/g) ?? []).length, 1);
});

test("sanitized child diagnostics retain fail-closed exit semantics", () => {
    const secret = "child-process-bearer-secret";
    assert.throws(
        () => runChecked(
            process.execPath,
            ["--eval", `process.stdout.write("useful stdout"); process.stderr.write("Authorization: Bearer ${secret}"); process.exit(7);`],
            { capture: true, sanitizeFailureOutput: true },
        ),
        (error) => {
            assert.match(error.message, /failed with exit code 7/);
            assert.match(error.message, /useful stdout/);
            assert.doesNotMatch(error.message, new RegExp(secret));
            return true;
        },
    );
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

test("OCI platform contract accepts a single OCI or Docker image manifest", () => {
    const document = {
        schemaVersion: 2,
        config: { digest: `sha256:${"1".repeat(64)}` },
        layers: [],
    };
    for (const mediaType of [
        "application/vnd.oci.image.manifest.v1+json",
        "application/vnd.docker.distribution.manifest.v2+json",
    ]) {
        assert.deepEqual(
            selectRuntimeManifest({ frozenDigest: lock.images.api, mediaType, document }),
            {
                parentDigest: lock.images.api,
                parentMediaType: mediaType,
                runtimeDigest: lock.images.api,
                runtimeMediaType: mediaType,
                platform: { os: "linux", architecture: "amd64" },
                relationship: "single-manifest",
            },
        );
    }
});

test("OCI platform contract selects exactly one linux amd64 child from a multi-platform index", () => {
    const amd64 = `sha256:${"2".repeat(64)}`;
    const arm64 = `sha256:${"3".repeat(64)}`;
    const contract = selectRuntimeManifest({
        frozenDigest: lock.images.api,
        mediaType: "application/vnd.oci.image.index.v1+json",
        document: {
            schemaVersion: 2,
            manifests: [
                { mediaType: "application/vnd.oci.image.manifest.v1+json", digest: arm64, platform: { os: "linux", architecture: "arm64" } },
                { mediaType: "application/vnd.oci.image.manifest.v1+json", digest: amd64, platform: { os: "linux", architecture: "amd64" } },
            ],
        },
    });
    assert.equal(contract.runtimeDigest, amd64);
    assert.equal(contract.relationship, "oci-index-child");
});

test("OCI platform contract ignores BuildKit attestation manifests", () => {
    const runnable = `sha256:${"4".repeat(64)}`;
    const attestation = `sha256:${"5".repeat(64)}`;
    const contract = selectRuntimeManifest({
        frozenDigest: lock.images.api,
        mediaType: "application/vnd.oci.image.index.v1+json",
        document: {
            schemaVersion: 2,
            manifests: [
                { mediaType: "application/vnd.oci.image.manifest.v1+json", digest: runnable, platform: { os: "linux", architecture: "amd64" } },
                {
                    mediaType: "application/vnd.oci.image.manifest.v1+json",
                    digest: attestation,
                    platform: { os: "unknown", architecture: "unknown" },
                    annotations: {
                        "vnd.docker.reference.digest": runnable,
                        "vnd.docker.reference.type": "attestation-manifest",
                    },
                },
            ],
        },
    });
    assert.equal(contract.runtimeDigest, runnable);
});

test("OCI platform contract rejects zero or multiple runnable linux amd64 children", () => {
    const descriptor = (digit, architecture = "amd64") => ({
        mediaType: "application/vnd.oci.image.manifest.v1+json",
        digest: `sha256:${digit.repeat(64)}`,
        platform: { os: "linux", architecture },
    });
    const index = (manifests) => ({
        frozenDigest: lock.images.api,
        mediaType: "application/vnd.oci.image.index.v1+json",
        document: { schemaVersion: 2, manifests },
    });
    assert.throws(() => selectRuntimeManifest(index([descriptor("6", "arm64")])), /exactly one runnable linux\/amd64/);
    assert.throws(() => selectRuntimeManifest(index([descriptor("7"), descriptor("8")])), /exactly one runnable linux\/amd64/);
});

test("OCI platform contract rejects malformed indexes and descriptors", () => {
    assert.throws(
        () => selectRuntimeManifest({
            frozenDigest: lock.images.api,
            mediaType: "application/vnd.oci.image.index.v1+json",
            document: { schemaVersion: 2, manifests: "not-an-array" },
        }),
        /neither a runnable image manifest nor an image index/,
    );
    assert.throws(
        () => selectRuntimeManifest({
            frozenDigest: lock.images.api,
            mediaType: "application/vnd.oci.image.index.v1+json",
            document: {
                schemaVersion: 2,
                manifests: [{ mediaType: "application/vnd.oci.image.manifest.v1+json", digest: "bad", platform: { os: "linux", architecture: "amd64" } }],
            },
        }),
        /exactly one runnable linux\/amd64/,
    );
});

test("candidate revision assertion accepts only the precomputed exact runtime digest", () => {
    const frozenImage = image("boundarymesh-api", lock.images.api);
    const contract = imageResolution("api");
    assert.doesNotThrow(() => assertRevisionImage({
        containers: [{ image: image("boundarymesh-api", contract.runtimeDigest) }],
    }, frozenImage, contract));
    assert.throws(() => assertRevisionImage({
        containers: [{ image: image("boundarymesh-api", `sha256:${"9".repeat(64)}`) }],
    }, frozenImage, contract), /exact allowed runtime platform digest/);
    assert.throws(() => assertRevisionImage({
        containers: [{ imageDigest: `sha256:${"a".repeat(64)}` }],
    }, frozenImage, contract), /exact allowed runtime platform digest/);
    assert.throws(() => assertRevisionImage({
        containers: [{ image: image("boundarymesh-api", contract.runtimeDigest) }],
    }, frozenImage, { ...contract, parentDigest: `sha256:${"b".repeat(64)}` }), /not bound to the frozen parent/);
});

test("OCI preflight contracts stay bound to every frozen parent and exact platform", () => {
    const manifest = verifyFrozenReleaseFile(fixture);
    const contracts = Object.fromEntries(["api", "web", "worker"].map((subject) => [
        subject,
        imageResolution(subject),
    ]));
    assert.equal(assertOciPreflightMatches(manifest, contracts), contracts);
    assert.throws(
        () => assertOciPreflightMatches(manifest, {
            ...contracts,
            web: { ...contracts.web, platform: { os: "linux", architecture: "arm64" } },
        }),
        /web OCI preflight contract/,
    );
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
