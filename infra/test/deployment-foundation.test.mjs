import assert from "node:assert/strict";
import { existsSync, readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { test } from "node:test";
import { fileURLToPath } from "node:url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const repositoryRoot = join(root, "..");
const source = readFileSync(join(root, "src", "deployment-foundation.ts"), "utf8");
const wif = readFileSync(join(root, "src", "wif.ts"), "utf8");
const workloads = readFileSync(join(root, "src", "workloads.ts"), "utf8");
const webEdge = readFileSync(join(root, "src", "web-edge.ts"), "utf8");
const index = readFileSync(join(root, "src", "index.ts"), "utf8");

function stringArray(text, name) {
    const body = text.match(new RegExp(`${name} = \\[([\\s\\S]*?)\\] as const`))?.[1] ?? "";
    return [...body.matchAll(/"([^"]+)"/g)].map((match) => match[1]);
}

test("split deploy identities are dedicated, keyless, and never cross-authorized", () => {
    assert.match(source, /RUNTIME_DEPLOY_SERVICE_ACCOUNT_ID = "boundarymesh-deploy"/);
    assert.match(source, /HOSTING_DEPLOY_SERVICE_ACCOUNT_ID = "boundarymesh-hosting-deploy"/);
    assert.match(source, /displayName: "BoundaryMesh Controlled Deployer"/);
    assert.match(source, /displayName: "BoundaryMesh Firebase Hosting Deployer"/);
    assert.equal((source.match(/new gcp\.serviceaccount\.Account/g) ?? []).length, 2);
    assert.equal(/new gcp\.serviceaccount\.Key/.test(source), false);
    assert.equal(/roles\/iam\.serviceAccountTokenCreator/.test(source), false);
    assert.match(index, /defineDeploymentFoundation/);
});

test("runtime and Hosting WIF contexts are exact and isolated", () => {
    for (const expected of [
        "assertion.repository_id == '${GITHUB_REPOSITORY_ID}'",
        "assertion.repository_owner_id == '${GITHUB_REPOSITORY_OWNER_ID}'",
        "assertion.repository_visibility == 'private'",
        "assertion.ref == '${GITHUB_MAIN_REF}'",
        "GITHUB_RUNTIME_DEPLOY_ENVIRONMENT = \"boundarymesh-deploy\"",
        "GITHUB_HOSTING_DEPLOY_ENVIRONMENT = \"boundarymesh-hosting-deploy\"",
        "Adellaghmari/boundarymesh/.github/workflows/deploy-release.yml@refs/heads/main",
        "workflow_dispatch",
    ]) assert.ok(wif.includes(expected), `missing WIF invariant: ${expected}`);
    assert.equal((wif.match(/\|\|/g) ?? []).length, 4);
    assert.match(source, /attribute\.environment\/\$\{GITHUB_RUNTIME_DEPLOY_ENVIRONMENT\}/);
    assert.match(source, /attribute\.environment\/\$\{GITHUB_HOSTING_DEPLOY_ENVIRONMENT\}/);
    assert.match(source, /serviceAccountId: runtimeDeployServiceAccount\.name/);
    assert.match(source, /serviceAccountId: hostingDeployServiceAccount\.name/);
    assert.match(source, /"runtime-deploy-environment-impersonation"[\s\S]*?serviceAccountId: runtimeDeployServiceAccount\.name,[\s\S]*?member: runtimeEnvironmentPrincipal/);
    assert.match(source, /"hosting-deploy-environment-impersonation"[\s\S]*?serviceAccountId: hostingDeployServiceAccount\.name,[\s\S]*?member: hostingEnvironmentPrincipal/);
});

test("controlled Run role is the exact existing-resource permission allowlist", () => {
    assert.deepEqual(stringArray(source, "CONTROLLED_RUN_DEPLOY_PERMISSIONS"), [
        "run.operations.get",
        "run.revisions.get",
        "run.revisions.list",
        "run.services.get",
        "run.services.getIamPolicy",
        "run.services.update",
        "run.workerpools.get",
        "run.workerpools.getIamPolicy",
        "run.workerpools.update",
    ]);
    assert.match(source, /CONTROLLED_RUN_DEPLOY_ROLE_ID = "boundarymeshControlledRunDeployer"/);
    const permissions = stringArray(source, "CONTROLLED_RUN_DEPLOY_PERMISSIONS");
    assert.equal(permissions.some((permission) => /\.(?:create|delete|setIamPolicy)$/.test(permission)), false);
    assert.equal(permissions.some((permission) => /^run\.(?:jobs|executions|tasks|instances)\./.test(permission)), false);
    assert.equal(/roles\/run\.(?:admin|developer)/.test(source), false);
});

test("Run operation polling uses an exact project-level single-permission role", () => {
    assert.match(source, /RUN_OPERATION_READER_ROLE_ID = "boundarymeshRunOperationReader"/);
    assert.deepEqual(stringArray(source, "RUN_OPERATION_READER_PERMISSIONS"), [
        "run.operations.get",
    ]);
    assert.equal(stringArray(source, "RUN_OPERATION_READER_PERMISSIONS").length, 1);
    assert.match(source, /title: "BoundaryMesh Run Operation Reader"/);
    assert.match(
        source,
        /"run-operation-reader-role"[\s\S]*?roleId: RUN_OPERATION_READER_ROLE_ID[\s\S]*?permissions: \[\.\.\.RUN_OPERATION_READER_PERMISSIONS\]/,
    );
    assert.equal((source.match(/role: runOperationReaderRole\.name/g) ?? []).length, 1);
    assert.match(
        source,
        /"runtime-deploy-operation-reader"[\s\S]*?role: runOperationReaderRole\.name,[\s\S]*?member: runtimePrincipal/,
    );
    assert.equal(/roles\/run\.(?:admin|developer|viewer)/.test(source), false);
    assert.equal(
        /new gcp\.projects\.IAMMember\(\s*"[^"]+",\s*\{[^}]*role: controlledRunDeployRole\.name/.test(source),
        false,
    );
});

test("Hosting Cloud Run read uses an exact Web-service-scoped single-permission role", () => {
    assert.match(source, /HOSTING_RUN_READER_ROLE_ID = "boundarymeshHostingRunReader"/);
    assert.deepEqual(stringArray(source, "HOSTING_RUN_READER_PERMISSIONS"), [
        "run.services.get",
    ]);
    assert.equal(stringArray(source, "HOSTING_RUN_READER_PERMISSIONS").length, 1);
    assert.match(source, /title: "BoundaryMesh Hosting Run Reader"/);
    assert.match(
        source,
        /"hosting-run-reader-role"[\s\S]*?roleId: HOSTING_RUN_READER_ROLE_ID[\s\S]*?permissions: \[\.\.\.HOSTING_RUN_READER_PERMISSIONS\]/,
    );
    assert.equal((source.match(/role: hostingRunReaderRole\.name/g) ?? []).length, 1);
    assert.match(
        source,
        /"hosting-deploy-web-run-reader"[\s\S]*?name: dependencies\.webService\.name,[\s\S]*?role: hostingRunReaderRole\.name,[\s\S]*?member: hostingPrincipal/,
    );
    assert.equal(/"hosting-deploy-web-run-reader"\s*,\s*\{[^}]*member: runtimePrincipal/.test(source), false);
    assert.equal(/new gcp\.projects\.IAMMember\(\s*"[^"]+",\s*\{[^}]*hostingRunReaderRole\.name/.test(source), false);
    assert.equal(/roles\/run\.(?:admin|developer|viewer)/.test(source), false);
    assert.equal(/roles\/firebase\.(?:admin|developAdmin)/.test(source), false);
});

test("Run role, actAs, and Invoker grants are resource scoped", () => {
    assert.equal((source.match(/new gcp\.cloudrunv2\.ServiceIamMember/g) ?? []).length, 4);
    assert.equal((source.match(/new gcp\.cloudrunv2\.WorkerPoolIamMember/g) ?? []).length, 1);
    for (const target of ["dependencies.apiService.name", "dependencies.webService.name", "dependencies.workerPool.name"]) {
        assert.ok(source.includes(`name: ${target}`));
    }
    assert.match(source, /role: controlledRunDeployRole\.name/g);
    assert.equal((source.match(/role: "roles\/iam\.serviceAccountUser"/g) ?? []).length, 1);
    for (const target of ["apiServiceAccount", "webServiceAccount", "workerServiceAccount"]) {
        assert.ok(source.includes(`dependencies.${target}`));
    }
    assert.match(source, /"runtime-deploy-api-invoker"[\s\S]*?role: "roles\/run\.invoker"[\s\S]*?member: runtimePrincipal/);
    assert.equal(
        /new gcp\.projects\.IAMMember\(\s*"[^"]+",\s*\{[^}]*roles\/iam\.serviceAccountUser/.test(source),
        false,
    );
});

test("Artifact Registry access separates readers from the evidence writer", () => {
    assert.equal((source.match(/role: ARTIFACT_READER_ROLE/g) ?? []).length, 3);
    assert.equal((source.match(/role: ARTIFACT_WRITER_ROLE/g) ?? []).length, 1);
    assert.match(source, /runtime-deploy-docker-reader[\s\S]*?dependencies\.dockerRepository\.repositoryId/);
    assert.match(source, /runtime-deploy-release-reader[\s\S]*?dependencies\.releaseRepository\.repositoryId/);
    assert.match(source, /hosting-deploy-release-reader[\s\S]*?dependencies\.releaseRepository\.repositoryId/);
    assert.match(source, /runtime-deploy-evidence-writer[\s\S]*?evidenceRepository\.repositoryId/);
    assert.equal(/hosting-deploy[\s\S]{0,300}ARTIFACT_WRITER_ROLE/.test(source), false);
    assert.equal(/new gcp\.projects\.IAMMember[\s\S]*roles\/artifactregistry/.test(source), false);
});

test("Hosting Admin is project scoped only to the Hosting identity and its excess is explicit", () => {
    assert.match(source, /FIREBASE_HOSTING_ROLE = "roles\/firebasehosting\.admin"/);
    assert.equal((source.match(/role: FIREBASE_HOSTING_ROLE/g) ?? []).length, 1);
    assert.match(source, /"hosting-deploy-firebase-admin"[\s\S]*?member: hostingPrincipal/);
    assert.equal(/member: runtimePrincipal[\s\S]{0,200}FIREBASE_HOSTING_ROLE/.test(source), false);
    assert.match(source, /Site create\/delete are platform-granted but BoundaryMesh-workflow-forbidden/);
    assert.equal(/roles\/(?:firebase\.admin|owner|editor)/.test(source), false);
    assert.deepEqual(stringArray(source, "HOSTING_WORKFLOW_FORBIDDEN_OPERATIONS"), [
        "firebase hosting:sites:create",
        "firebase hosting:sites:delete",
        "firebase init",
        "project switching",
        "arbitrary Firebase site input",
        "arbitrary Firebase project input",
    ]);
});

test("Hosting quota-project use is an exact single-permission custom role", () => {
    assert.match(source, /HOSTING_QUOTA_CONSUMER_ROLE_ID = "boundarymeshHostingQuotaConsumer"/);
    assert.deepEqual(stringArray(source, "HOSTING_QUOTA_CONSUMER_PERMISSIONS"), [
        "serviceusage.services.use",
    ]);
    assert.equal(stringArray(source, "HOSTING_QUOTA_CONSUMER_PERMISSIONS").length, 1);
    assert.match(source, /title: "BoundaryMesh Hosting Quota Consumer"/);
    assert.match(
        source,
        /description:[\s\S]*?"Allows the isolated BoundaryMesh Firebase Hosting deployment identity to designate "[\s\S]*?"boundarymesh-dev as the quota project for Firebase Hosting API requests\."/,
    );
    assert.match(
        source,
        /"hosting-quota-consumer-role"[\s\S]*?roleId: HOSTING_QUOTA_CONSUMER_ROLE_ID[\s\S]*?permissions: \[\.\.\.HOSTING_QUOTA_CONSUMER_PERMISSIONS\]/,
    );
});

test("Hosting quota-project use is bound only to the Hosting deploy identity", () => {
    assert.equal((source.match(/role: hostingQuotaConsumerRole\.name/g) ?? []).length, 1);
    assert.match(
        source,
        /"hosting-deploy-quota-consumer"[\s\S]*?role: hostingQuotaConsumerRole\.name,[\s\S]*?member: hostingPrincipal/,
    );
    assert.equal(
        /roles\/serviceusage\.(?:serviceUsageConsumer|serviceUsageAdmin)|roles\/(?:editor|owner)/.test(source),
        false,
    );
});

test("deployment evidence repository is isolated, generic, regional, and protected", () => {
    assert.match(source, /DEPLOYMENT_EVIDENCE_REPOSITORY_ID = "boundarymesh-deployments"/);
    assert.match(source, /repositoryId: DEPLOYMENT_EVIDENCE_REPOSITORY_ID/);
    assert.match(source, /location: foundation\.region/);
    assert.match(source, /format: "GENERIC"/);
    assert.match(source, /mode: "STANDARD_REPOSITORY"/);
    assert.match(source, /deletionPolicy: "PREVENT"/);
    assert.match(source, /protect: true/);
    assert.equal(/generic upload|docker push|artifact publication/i.test(source), false);
});

test("release ownership fences contain only the reviewed property paths", () => {
    assert.deepEqual(stringArray(workloads, "API_RELEASE_OWNERSHIP_PATHS"), [
        "template.containers[0].image",
        "template.revision",
        "traffics",
    ]);
    assert.deepEqual(stringArray(webEdge, "WEB_RELEASE_OWNERSHIP_PATHS"), [
        "template.containers[0].image",
        "template.revision",
        "traffics",
    ]);
    assert.deepEqual(stringArray(workloads, "WORKER_RELEASE_OWNERSHIP_PATHS"), [
        "template.containers[0].image",
        "template.revision",
        "instanceSplits",
    ]);
    for (const text of [workloads, webEdge]) {
        assert.equal(/ignoreChanges:\s*\[?\s*["']?(?:template|spec|resource)["']?\s*\]?/.test(text), false);
    }
    for (const forbidden of ["serviceAccount", "envs", "startupProbe", "livenessProbe", "scaling", "resources", "ingress"] ) {
        assert.equal([...stringArray(workloads, "API_RELEASE_OWNERSHIP_PATHS"), ...stringArray(workloads, "WORKER_RELEASE_OWNERSHIP_PATHS"), ...stringArray(webEdge, "WEB_RELEASE_OWNERSHIP_PATHS")].some((path) => path.includes(forbidden)), false);
    }
});

test("dormant deployment workflow source is present without expanding the infrastructure foundation", () => {
    assert.equal(existsSync(join(repositoryRoot, ".github", "workflows", "deploy-release.yml")), true);
    assert.equal(/gcloud\s+run|firebase\s+deploy|pulumi\s+(?:up|refresh)|compute\.googleapis\.com/i.test(source), false);
    assert.equal(/service_account_key|credentials_json|private_key|client_secret|firebase_token/i.test(source), false);
});
