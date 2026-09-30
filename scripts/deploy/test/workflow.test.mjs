import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { join } from "node:path";
import test from "node:test";
import { parse } from "yaml";

const root = join(import.meta.dirname, "..", "..", "..");
const workflowPath = join(root, ".github", "workflows", "deploy-release.yml");
const workflowText = readFileSync(workflowPath, "utf8");
const workflow = parse(workflowText);
const runtime = readFileSync(join(root, "scripts", "deploy", "cloud-runtime.mjs"), "utf8");
const hosting = readFileSync(join(root, "scripts", "deploy", "cloud-hosting.mjs"), "utf8");
const artifacts = readFileSync(join(root, "scripts", "deploy", "artifacts.mjs"), "utf8");
const lock = JSON.parse(readFileSync(join(root, "scripts", "deploy", "frozen-release.json"), "utf8"));

test("workflow has only manual dispatch, bounded operations, main guards, and serialized production mutation", () => {
    assert.deepEqual(Object.keys(workflow.on), ["workflow_dispatch"]);
    assert.deepEqual(workflow.on.workflow_dispatch.inputs.operation.options, ["stage", "promote", "rollback"]);
    assert.deepEqual(Object.keys(workflow.on.workflow_dispatch.inputs).sort(), ["operation", "promotion_run_id", "stage_run_id"]);
    assert.equal(workflow.concurrency.group, "boundarymesh-production-deployment");
    assert.equal(workflow.concurrency["cancel-in-progress"], false);
    for (const job of Object.values(workflow.jobs)) {
        assert.match(job.if, /github\.ref == 'refs\/heads\/main'/);
        assert.equal(job["timeout-minutes"], 30);
    }
});

test("top-level permissions are minimal and every action reference is a full commit SHA", () => {
    assert.deepEqual(workflow.permissions, { contents: "read", "id-token": "write" });
    const uses = [...workflowText.matchAll(/uses:\s*([^\s@]+)@([^\s]+)/g)];
    assert.ok(uses.length > 0);
    for (const [, action, pin] of uses) assert.match(pin, /^[0-9a-f]{40}$/, `${action} is not commit pinned`);
});

test("runtime and Hosting authority remain in separate jobs and use exact identities", () => {
    for (const [name, job] of Object.entries(workflow.jobs)) {
        const body = JSON.stringify(job);
        if (job.environment === "boundarymesh-deploy") {
            assert.doesNotMatch(body, /HOSTING_DEPLOY_SA/);
            assert.match(body, /RUNTIME_DEPLOY_SA/);
        }
        if (job.environment === "boundarymesh-hosting-deploy") {
            assert.doesNotMatch(body, /RUNTIME_DEPLOY_SA/);
            assert.match(body, /HOSTING_DEPLOY_SA/);
        }
    }
    assert.equal(workflow.env.WIF_PROVIDER, "projects/891977254468/locations/global/workloadIdentityPools/boundarymesh-github/providers/github-actions");
});

test("stage, promotion, and rollback are separate human-dispatched paths", () => {
    assert.match(workflow.jobs["stage-runtime"].if, /operation == 'stage'/);
    assert.match(workflow.jobs["promote-runtime"].if, /operation == 'promote'/);
    assert.match(workflow.jobs["rollback-hosting"].if, /operation == 'rollback'/);
    assert.ok(workflow.jobs["promote-hosting"].needs.includes("promote-runtime"));
    assert.ok(workflow.jobs["rollback-runtime"].needs.includes("rollback-hosting"));
    assert.doesNotMatch(workflowText, /workflow_dispatches|gh workflow run|actions\/workflows\/.*dispatches/);
    assert.match(workflow.jobs["record-promote-evidence"].if, /always\(\)/);
    assert.match(JSON.stringify(workflow.jobs["record-promote-evidence"]), /observe-promotion/);
});

test("promotion and rollback require exact immutable evidence run references", () => {
    assert.match(workflowText, /download-evidence --operation stage --run-id '\$\{\{ inputs\.stage_run_id \}\}'/);
    assert.match(workflowText, /download-evidence --operation promote --run-id '\$\{\{ inputs\.promotion_run_id \}\}'/);
    assert.match(workflowText, /assert-reference[\s\S]*--stage-run-id '\$\{\{ inputs\.stage_run_id \}\}'/);
    assert.doesNotMatch(workflowText, /previous latest|latest-stage|--version latest/);
});

test("every immutable evidence record independently reverifies the frozen published release", () => {
    for (const name of ["record-stage-evidence", "record-promote-evidence", "record-rollback-evidence"]) {
        assert.match(JSON.stringify(workflow.jobs[name]), /download-release --destination/);
    }
    const recordJobs = ["record-stage-evidence", "record-promote-evidence", "record-rollback-evidence"]
        .map((name) => JSON.stringify(workflow.jobs[name])).join("\n");
    assert.match(recordJobs, /decode-json/);
    assert.match(recordJobs, /decode --value/);
});

test("workflow contains no application build, republish, infrastructure, or identity mutation path", () => {
    const forbidden = [
        /docker\s+(?:build|buildx)/i, /mvn\s+(?:package|verify)/i, /gradle\s+build/i,
        /npm\s+(?:run\s+build|ci\s+--prefix\s+frontend)/i, /vite\s+build/i,
        /pulumi\s+(?:up|refresh)/i, /terraform\s+apply/i,
        /gcloud\s+(?:iam|services|projects\s+add-iam-policy-binding)/i,
        /artifact-publish\.yml|gh\s+workflow\s+run/i,
    ];
    for (const pattern of forbidden) assert.doesNotMatch(workflowText, pattern);
});

test("application identities are fixed by the frozen release lock", () => {
    assert.equal(lock.sourceSha, "a876a7bfa5fa86f88e8387efb557773281daa6e6");
    assert.equal(lock.publicationRunId, "36492495801");
    assert.equal(lock.releaseManifestSha256, "ecdbaf88710ff73fd694c5c4e13c0a8ce300fdacf2a3761064b7511bd627300d");
    assert.equal(lock.images.api, "sha256:bcfd9b01a3189228f583fa3f5d1ed8797f73d214af85bea4d84caeedfa0db8d7");
    assert.equal(lock.images.worker, "sha256:7b1841487a2d1b3751e8bc63ee5323965e76ebeae9ae581c95b672f50154bb2d");
    assert.equal(lock.images.web, "sha256:85f98f90450b7807f55649f58807f952010a6278793b808ae1f4df78a22c0b04");
    assert.equal(lock.frontend.archiveSha256, "80d4cfa4e88cac297c7a130ac5c83d68b468b06b60e04537c5060cb275943d84");
    assert.equal(lock.firebase.configSha256, "34d9f2a073429420b6a15b9133486096fba75a1255ec1f3431fda5b2fd5a242c");
});

test("runtime mutation adapter uses ETags, allowMissing=false, and exact field masks", () => {
    assert.match(runtime, /etag: resource\.etag/);
    assert.match(runtime, /allowMissing: false/);
    assert.match(runtime, /"template\.revision,template\.containers,traffic"/);
    assert.match(runtime, /"template\.revision,template\.containers,instanceSplits"/);
    assert.match(runtime, /"traffic"\)/);
    assert.match(runtime, /"instanceSplits"\)/);
    assert.doesNotMatch(runtime, /updateMask[^\n]*["'`]\*["'`]/);
    assert.match(runtime, /MAX_API_RESPONSE_BYTES/);
});

test("runtime candidates stay unpromoted and IAM invariants remain explicit", () => {
    assert.match(runtime, /revision, percent: 0, tag: stageTag/);
    assert.match(runtime, /revision: workerRevision, percent: 0/);
    assert.match(runtime, /Private API unexpectedly has a public Invoker/);
    assert.match(runtime, /serviceAccount:\$\{WEB_SA\}/);
    assert.match(runtime, /assertInvariantUnchanged/);
    assert.match(runtime, /promotionEvidence\.runtime\.result/);
});

test("Hosting operations are fixed to one site, safe archive, preview stage, and exact-version release", () => {
    assert.match(hosting, /hosting:channel:deploy/);
    assert.match(hosting, /const channelId = `bm-\$\{runId\}`/);
    assert.match(hosting, /extractFrontendArchive/);
    assert.match(hosting, /materializeFrozenFirebaseConfig/);
    assert.match(hosting, /releases\?versionName=/);
    assert.match(hosting, /FORBIDDEN_OPERATIONS/);
    assert.match(hosting, /Stale promoted Hosting baseline; refusing rollback/);
    assert.match(hosting, /"firebase init"/);
    assert.match(hosting, /"hosting:sites:create"/);
    assert.match(hosting, /"hosting:sites:delete"/);
});

test("only deployment evidence is uploaded and immutable collisions fail first", () => {
    assert.match(artifacts, /Immutable deployment evidence version already exists/);
    assert.match(artifacts, /trustworthy not-found result/);
    assert.match(artifacts, /unexpected deployment evidence file set/);
    assert.match(artifacts, /lock\.deploymentEvidence\.repository/);
    assert.doesNotMatch(artifacts, /docker\s+push|boundarymesh-frontend.*upload|boundarymesh-release.*upload/);
    assert.match(artifacts, /repository: lock\.releaseRepository,[\s\S]*packageName: lock\.frontend\.package/);
});

test("workflow has no automatic rollback, latest-release selection, or credential output", () => {
    assert.doesNotMatch(workflowText, /if:\s*failure\(\)|if:\s*always\(\).*rollback|automatic rollback/i);
    assert.doesNotMatch(workflowText, /--version\s+latest|tags\/latest|:latest\b/);
    assert.doesNotMatch(workflowText, /print-identity-token|print-access-token|cat\s+.*credentials|echo\s+.*(?:TOKEN|PASSWORD|SECRET)/i);
});
