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

function dependencies(name) {
    const needs = workflow.jobs[name].needs ?? [];
    return Array.isArray(needs) ? needs : [needs];
}

function ancestors(name, found = new Set()) {
    for (const dependency of dependencies(name)) {
        if (!found.has(dependency)) {
            found.add(dependency);
            ancestors(dependency, found);
        }
    }
    return found;
}

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

test("runtime health uses fresh keyless ID tokens with the base service audience", () => {
    assert.equal(workflow.env.API_SERVICE_URL, "https://boundarymesh-api-jjt6nkfsna-ez.a.run.app");
    for (const name of ["stage-preflight-runtime", "stage-runtime", "promote-runtime"]) {
        const job = workflow.jobs[name];
        const auth = job.steps.find((step) => step.id === "runtime-health-auth");
        assert.ok(auth, `${name} has no runtime health auth step`);
        assert.equal(auth.uses, "google-github-actions/auth@7c6bc770dae815cd3e89ee6cdf493a5fab2cc093");
        assert.equal(auth.with.service_account, "${{ env.RUNTIME_DEPLOY_SA }}");
        assert.equal(auth.with.token_format, "id_token");
        assert.equal(auth.with.id_token_audience, "${{ env.API_SERVICE_URL }}");
        assert.equal(auth.with.credentials_json, undefined);
        const healthStep = job.steps.find((step) =>
            String(step.run ?? "").includes("cloud-runtime.mjs") &&
            (String(step.run).includes(" preflight ") || String(step.run).includes(" stage ") || String(step.run).includes(" promote ")));
        assert.equal(healthStep.env.BOUNDARYMESH_API_HEALTH_ID_TOKEN, "${{ steps.runtime-health-auth.outputs.id_token }}");
        assert.ok(job.steps.indexOf(auth) < job.steps.indexOf(healthStep));
    }
    assert.doesNotMatch(runtime, /print-identity-token|credentials_json|service-account.*key/i);
    assert.doesNotMatch(workflowText, /credentials_json|echo\s+.*BOUNDARYMESH_API_HEALTH_ID_TOKEN/i);
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

test("both stage mutations are behind every authority-separated preflight and the explicit barrier", () => {
    const required = ["validate-release", "stage-preflight-runtime", "stage-preflight-hosting", "stage-preflight-barrier"];
    for (const name of ["stage-runtime", "stage-hosting"]) {
        const reachable = ancestors(name);
        for (const dependency of required) assert.ok(reachable.has(dependency), `${name} can bypass ${dependency}`);
    }
    assert.deepEqual(dependencies("stage-preflight-barrier").sort(), ["stage-preflight-hosting", "stage-preflight-runtime", "validate-release"]);
    assert.equal(workflow.jobs["stage-preflight-runtime"].environment, "boundarymesh-deploy");
    assert.equal(workflow.jobs["stage-preflight-hosting"].environment, "boundarymesh-hosting-deploy");
});

test("preflight jobs contain every global validation and no mutating command", () => {
    const runtimePreflight = JSON.stringify(workflow.jobs["stage-preflight-runtime"]);
    const hostingPreflight = JSON.stringify(workflow.jobs["stage-preflight-hosting"]);
    assert.match(runtimePreflight, /download-release/);
    assert.match(runtimePreflight, /artifacts\.mjs preflight-stage/);
    assert.match(runtimePreflight, /cloud-runtime\.mjs preflight --manifest/);
    assert.match(hostingPreflight, /download-release/);
    assert.match(hostingPreflight, /download-frontend/);
    assert.match(hostingPreflight, /cloud-hosting\.mjs preflight/);
    for (const body of [runtimePreflight, hostingPreflight]) {
        assert.doesNotMatch(body, /cloud-runtime\.mjs stage|cloud-hosting\.mjs stage|hosting:channel:deploy|artifacts generic upload|publish-evidence/);
    }
    assert.match(artifacts, /preflightStageArtifacts/);
    assert.match(artifacts, /assertExactOciLookup/);
    assert.match(runtime, /resolveRuntimeImageContracts/);
    assert.match(runtime, /selectRuntimeManifest/);
    assert.match(artifacts, /assertEvidenceVersionAbsent/);
    assert.match(hosting, /preflightHosting/);
    assert.match(hosting, /assertPreviewChannelAbsent/);
    assert.match(hosting, /inspectFrontendArchive/);
    assert.match(hosting, /materializeFrozenFirebaseConfig/);
});

test("mutation jobs revalidate preflight baselines before their first writes", () => {
    const runtimeStage = JSON.stringify(workflow.jobs["stage-runtime"]);
    const hostingStage = JSON.stringify(workflow.jobs["stage-hosting"]);
    assert.match(runtimeStage, /--preflight/);
    assert.match(hostingStage, /--preflight/);
    assert.match(runtime, /assertRuntimePreflightMatches\(await runtimePreflightSnapshot\(before\), preflight\)/);
    assert.ok(runtime.indexOf("assertRuntimePreflightMatches(await runtimePreflightSnapshot(before), preflight)") < runtime.indexOf("await patchResource(current"));
    assert.match(hosting, /canonicalJson\(before\) !== canonicalJson\(preflight\.before\)/);
    assert.match(hosting, /const mutationBaseline = await readHostingBaseline\(lock\)/);
    const deployPreview = hosting.indexOf("await deployPreview(runId");
    assert.ok(hosting.indexOf("const mutationBaseline = await readHostingBaseline(lock)") < deployPreview);
    assert.ok(hosting.lastIndexOf("await assertPreviewChannelAbsent(runId") < deployPreview);
    assert.match(artifacts, /assertEvidenceVersionAbsent\(\{ operation, runId, lock \}\)/);
});

test("stage evidence requires both mutations and cannot publish for partial success", () => {
    assert.deepEqual(dependencies("record-stage-evidence").sort(), ["stage-hosting", "stage-runtime", "validate-release"]);
    assert.doesNotMatch(workflow.jobs["record-stage-evidence"].if, /always\(\)/);
    assert.match(workflow.jobs["record-stage-evidence"].if, /operation == 'stage'/);
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
    assert.equal(lock.sourceSha, "7352538198f2d3d372130977d311c48011fa85eb");
    assert.equal(lock.publicationRunId, "37292939514");
    assert.equal(lock.releaseManifestSha256, "fb7e2680b4192b829883734add0dd60341736363e52a8fbdda1a2cbf0994ed37");
    assert.equal(lock.images.api, "sha256:cdc8b108097c6c950da63222881750afa6361e4f7eed58798a3b6fe7ec11d4fd");
    assert.equal(lock.images.worker, "sha256:1aba22d24c4bf0f71f8ebfd98439d555f2d27937cef969995e340c2a49bc59c0");
    assert.equal(lock.images.web, "sha256:492824f251076cc2c9d151f34d8000d596f9e6d2b5610e0485433cd58806df0e");
    assert.equal(lock.frontend.archiveSha256, "e1e251d1f4727aeda3e95086b47118baec4436a2cf59eb79feb7bbb3997bb077");
    assert.equal(lock.firebase.configSha256, "8dc8ca5b52f08c9179a8fced41734e1ad7b79b1d053a61bf085a83055da31b4a");
    assert.equal(lock.firebase.vendoredPath, "scripts/deploy/release/frozen/firebase.7352538198f2d3d372130977d311c48011fa85eb.json");
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
    assert.match(artifacts, /canonical trustworthy NOT_FOUND result/);
    assert.match(artifacts, /unexpected deployment evidence file set/);
    assert.match(artifacts, /lock\.deploymentEvidence\.repository/);
    assert.doesNotMatch(artifacts, /docker\s+push|boundarymesh-frontend.*upload|boundarymesh-release.*upload/);
    assert.match(artifacts, /repository: lock\.releaseRepository,[\s\S]*packageName: lock\.frontend\.package/);
});

test("evidence-only recovery is isolated from the deployment mutation workflow", () => {
    const recover = readFileSync(join(root, "scripts", "deploy", "recover-promotion-evidence.mjs"), "utf8");
    assert.doesNotMatch(workflowText, /recover-promotion-evidence/);
    assert.deepEqual(workflow.on.workflow_dispatch.inputs.operation.options, ["stage", "promote", "rollback"]);
    assert.doesNotMatch(recover, /cloud-runtime\.mjs|cloud-hosting\.mjs/);
    assert.doesNotMatch(recover, /promoteRuntime|promoteHosting|rollbackRuntime|rollbackHosting/);
    assert.doesNotMatch(recover, /pulumi|gcloud iam|hosting:channel:deploy|releases\?versionName=/);
    assert.match(recover, /recover only/);
    assert.match(JSON.stringify(workflow.jobs["record-promote-evidence"]), /observe-promotion/);
});

test("dedicated recovery workflow is dispatch-only, evidence-only, and mutation-isolated", () => {
    const recoveryPath = join(root, ".github", "workflows", "recover-promotion-evidence.yml");
    const recoveryText = readFileSync(recoveryPath, "utf8");
    const recovery = parse(recoveryText);
    assert.deepEqual(Object.keys(recovery.on), ["workflow_dispatch"]);
    assert.equal(recovery.on.workflow_dispatch.inputs.operation, undefined);
    assert.deepEqual(Object.keys(recovery.on.workflow_dispatch.inputs).sort(), ["promotion_run_id", "stage_run_id"]);
    assert.equal(recovery.concurrency.group, "boundarymesh-promotion-evidence-recovery");
    assert.notEqual(recovery.concurrency.group, "boundarymesh-production-deployment");
    assert.deepEqual(recovery.permissions, { contents: "read", "id-token": "write", actions: "read" });
    assert.equal(recovery.permissions.contents !== "write", true);
    assert.equal(recovery.permissions.actions !== "write", true);
    assert.deepEqual(Object.keys(recovery.jobs).sort(), ["hosting-read-preflight", "recover-promotion-evidence"]);
    assert.equal(recovery.jobs["hosting-read-preflight"].environment, "boundarymesh-hosting-deploy");
    assert.equal(recovery.jobs["recover-promotion-evidence"].environment, "boundarymesh-deploy");
    assert.deepEqual(recovery.jobs["recover-promotion-evidence"].needs, "hosting-read-preflight");
    assert.match(recovery.jobs["hosting-read-preflight"].if, /github\.ref == 'refs\/heads\/main'/);
    assert.match(recovery.jobs["recover-promotion-evidence"].if, /github\.ref == 'refs\/heads\/main'/);
    assert.equal(recovery.env.RUNTIME_DEPLOY_SA, "boundarymesh-deploy@boundarymesh-dev.iam.gserviceaccount.com");
    assert.equal(recovery.env.HOSTING_DEPLOY_SA, "boundarymesh-hosting-deploy@boundarymesh-dev.iam.gserviceaccount.com");
    const hostingAuth = JSON.stringify(recovery.jobs["hosting-read-preflight"]);
    const evidenceAuth = JSON.stringify(recovery.jobs["recover-promotion-evidence"]);
    assert.match(recoveryText, /service_account: \$\{\{ env\.HOSTING_DEPLOY_SA \}\}/);
    assert.match(recoveryText, /service_account: \$\{\{ env\.RUNTIME_DEPLOY_SA \}\}/);
    assert.doesNotMatch(hostingAuth, /RUNTIME_DEPLOY_SA/);
    assert.doesNotMatch(hostingAuth, /recover-promotion-evidence\.mjs recover/);
    assert.doesNotMatch(hostingAuth, /artifacts\.mjs/);
    assert.doesNotMatch(evidenceAuth, /HOSTING_DEPLOY_SA/);
    assert.doesNotMatch(evidenceAuth, /cloud-hosting\.mjs/);
    assert.match(recoveryText, /google-github-actions\/auth@7c6bc770dae815cd3e89ee6cdf493a5fab2cc093/);
    assert.doesNotMatch(recoveryText, /credentials_json|service_account_key|private_key/);
    assert.doesNotMatch(recoveryText, /cloud-runtime\.mjs\s+promote|cloud-hosting\.mjs\s+promote/);
    assert.doesNotMatch(recoveryText, /rollbackRuntime|rollbackHosting|cloud-runtime\.mjs\s+rollback|cloud-hosting\.mjs\s+rollback/);
    assert.doesNotMatch(recoveryText, /pulumi\s+up|gcloud\s+run\s+services\s+update|gcloud\s+run\s+worker-pools\s+update/);
    assert.doesNotMatch(recoveryText, /hosting:channel:deploy|releases\?versionName=|firebase\s+deploy|gh\s+workflow\s+(run|rerun)/);
    assert.match(recoveryText, /cloud-hosting\.mjs snapshot/);
    assert.match(recoveryText, /cloud-runtime\.mjs snapshot/);
    assert.match(recoveryText, /seal-hosting-handoff/);
    assert.match(recoveryText, /verify-hosting-handoff/);
    assert.match(recoveryText, /assert-worker-rollback/);
    assert.doesNotMatch(recoveryText, /--worker-pool=boundarymesh-worker/);
    assert.doesNotMatch(recoveryText, /gcloud run worker-pools revisions describe/);
    assert.doesNotMatch(recoveryText, /gcloud run revisions describe "\$WORKER_ROLLBACK"/);
    assert.match(recoveryText, /recover-promotion-evidence\.mjs recover[\s\S]*--publish/);
    assert.doesNotMatch(recoveryText, /print-identity-token|print-access-token|echo\s+.*(?:TOKEN|PASSWORD|SECRET)/i);
    assert.doesNotMatch(recoveryText, /actions\/(?:upload|download)-artifact/);
    const uses = [...recoveryText.matchAll(/uses:\s*([^\s@]+)@([^\s]+)/g)];
    assert.ok(uses.length > 0);
    for (const [, action, pin] of uses) assert.match(pin, /^[0-9a-f]{40}$/, `${action} is not commit pinned`);
});

test("workflow has no automatic rollback, latest-release selection, or credential output", () => {
    assert.doesNotMatch(workflowText, /if:\s*failure\(\)|if:\s*always\(\).*rollback|automatic rollback/i);
    assert.doesNotMatch(workflowText, /--version\s+latest|tags\/latest|:latest\b/);
    assert.doesNotMatch(workflowText, /print-identity-token|print-access-token|cat\s+.*credentials|echo\s+.*(?:TOKEN|PASSWORD|SECRET)/i);
});
