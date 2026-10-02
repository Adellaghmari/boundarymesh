import assert from "node:assert/strict";
import { existsSync, readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { test } from "node:test";
import { fileURLToPath } from "node:url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const repositoryRoot = join(root, "..");
const source = readFileSync(join(root, "src", "ci-preview.ts"), "utf8");
const index = readFileSync(join(root, "src", "index.ts"), "utf8");
const wif = readFileSync(join(root, "src", "wif.ts"), "utf8");
const stack = readFileSync(join(root, "Pulumi.dev.yaml"), "utf8");
const workflowPath = join(repositoryRoot, ".github", "workflows", "infra-preview.yml");
const workflow = readFileSync(workflowPath, "utf8");
const verificationWorkflowPath = join(repositoryRoot, ".github", "workflows", "wif-verify.yml");
const verificationWorkflow = readFileSync(verificationWorkflowPath, "utf8");

const commonClaims = {
    repository_id: "1385415640",
    repository_owner_id: "179922674",
    repository_visibility: "private",
    ref: "refs/heads/main",
};
const privilegedContexts = [
    {
        environment: "boundarymesh-dev",
        workflow_ref: "Adellaghmari/boundarymesh/.github/workflows/wif-verify.yml@refs/heads/main",
        event_name: "workflow_dispatch",
    },
    {
        environment: "boundarymesh-ci-preview",
        workflow_ref: "Adellaghmari/boundarymesh/.github/workflows/infra-preview.yml@refs/heads/main",
        event_name: "workflow_dispatch",
    },
    {
        environment: "boundarymesh-artifact-publish",
        workflow_ref: "Adellaghmari/boundarymesh/.github/workflows/artifact-publish.yml@refs/heads/main",
        event_name: "workflow_dispatch",
    },
    {
        environment: "boundarymesh-deploy",
        workflow_ref: "Adellaghmari/boundarymesh/.github/workflows/deploy-release.yml@refs/heads/main",
        event_name: "workflow_dispatch",
    },
    {
        environment: "boundarymesh-hosting-deploy",
        workflow_ref: "Adellaghmari/boundarymesh/.github/workflows/deploy-release.yml@refs/heads/main",
        event_name: "workflow_dispatch",
    },
    {
        environment: "boundarymesh-deploy",
        workflow_ref: "Adellaghmari/boundarymesh/.github/workflows/recover-promotion-evidence.yml@refs/heads/main",
        event_name: "workflow_dispatch",
    },
];

function providerAllows(assertion) {
    return (
        Object.entries(commonClaims).every(([claim, expected]) => assertion[claim] === expected) &&
        privilegedContexts.some((context) =>
            Object.entries(context).every(([claim, expected]) => assertion[claim] === expected),
        )
    );
}

test("CI preview source defines exactly five narrowly scoped resources", () => {
    const resourceCreates = [
        /new gcp\.serviceaccount\.Account/g,
        /new gcp\.projects\.IAMCustomRole/g,
        /new gcp\.storage\.BucketIAMMember/g,
        /new gcp\.kms\.CryptoKeyIAMMember/g,
        /new gcp\.serviceaccount\.IAMMember/g,
    ].reduce((count, pattern) => count + (source.match(pattern) ?? []).length, 0);

    assert.equal(resourceCreates, 5);
    assert.equal((source.match(/new gcp\.serviceaccount\.Account/g) ?? []).length, 1);
    assert.equal((source.match(/new gcp\.projects\.IAMCustomRole/g) ?? []).length, 1);
    assert.equal((source.match(/new gcp\.storage\.BucketIAMMember/g) ?? []).length, 1);
    assert.equal((source.match(/new gcp\.kms\.CryptoKeyIAMMember/g) ?? []).length, 1);
    assert.equal((source.match(/new gcp\.serviceaccount\.IAMMember/g) ?? []).length, 1);
});

test("CI preview service account is dedicated and keyless", () => {
    assert.match(source, /CI_PREVIEW_SERVICE_ACCOUNT_ID = "boundarymesh-ci-preview"/);
    assert.match(source, /accountId:\s*CI_PREVIEW_SERVICE_ACCOUNT_ID/);
    assert.match(source, /displayName:\s*"BoundaryMesh CI Preview"/);
    assert.equal(/new gcp\.serviceaccount\.Key/.test(source), false);
    assert.equal(/roles\/(owner|editor|viewer|run\.|artifactregistry\.|firebase|secretmanager|datastore|pubsub)/i.test(source), false);
    assert.equal(/roles\/iam\.serviceAccountTokenCreator/.test(source), false);
});

test("custom state role contains only object get and list", () => {
    assert.match(source, /CI_STATE_READER_ROLE_ID = "boundarymeshCiStateReader"/);
    assert.match(source, /title:\s*"BoundaryMesh CI State Reader"/);
    assert.match(source, /description:\s*"Read-only access to BoundaryMesh Pulumi state objects\."/);
    const permissionBlock = source.match(/CI_STATE_READER_PERMISSIONS = \[([\s\S]*?)\] as const/)?.[1] ?? "";
    const permissions = [...permissionBlock.matchAll(/"([^"]+)"/g)].map((match) => match[1]);
    assert.deepEqual(permissions, ["storage.objects.get", "storage.objects.list"]);
    assert.equal(/storage\.buckets\.get/.test(source), false);
    assert.equal(/storage\.objects\.(create|update|delete|restore|setIamPolicy)/.test(source), false);
});

test("state role is member-bound only to the existing state bucket", () => {
    assert.match(source, /new gcp\.storage\.BucketIAMMember/);
    assert.match(source, /bucket:\s*dependencies\.stateBucket\.name/);
    assert.match(source, /role:\s*stateReaderRole\.name/);
    assert.match(source, /member:\s*serviceAccountPrincipal/);
    assert.equal(/gcp\.storage\.BucketIAM(?:Binding|Policy)/.test(source), false);
    assert.equal(/allUsers|allAuthenticatedUsers/.test(source), false);
    assert.match(index, /stateBucket:\s*sharedState\.stateBucket/);
});

test("state KMS grant is decrypt-only and cannot reach attestation signing", () => {
    assert.match(source, /new gcp\.kms\.CryptoKeyIAMMember/);
    assert.match(source, /cryptoKeyId:\s*dependencies\.stateSecretsCryptoKey\.id/);
    assert.match(source, /role:\s*"roles\/cloudkms\.cryptoKeyDecrypter"/);
    assert.match(source, /member:\s*serviceAccountPrincipal/);
    assert.equal(/cryptoKeyEncrypterDecrypter|useToEncrypt|cloudkms\.admin/i.test(source), false);
    assert.equal(/boundarymesh-attestation|exchange-attestation|ASYMMETRIC_SIGN|signer/.test(source), false);
    assert.match(index, /stateSecretsCryptoKey:\s*sharedState\.secretsCryptoKey/);
});

test("CI federation authorization is service-account and environment scoped", () => {
    assert.match(source, /CI_PREVIEW_ENVIRONMENT = "boundarymesh-ci-preview"/);
    assert.match(source, /serviceAccountId:\s*serviceAccount\.name/);
    assert.match(source, /role:\s*"roles\/iam\.workloadIdentityUser"/);
    assert.match(
        source,
        /workloadIdentityPools\/\$\{GITHUB_WIF_POOL_ID\}\/attribute\.environment\/\$\{CI_PREVIEW_ENVIRONMENT\}/,
    );
    assert.match(source, /dependsOn:\s*\[dependencies\.wifProvider\]/);
    assert.equal(/new gcp\.projects\.IAM(?:Member|Binding|Policy)/.test(source), false);
});

test("provider source expresses four immutable claims and exactly six privileged contexts", () => {
    for (const expected of [
        "1385415640",
        "179922674",
        "repository_visibility == 'private'",
        "refs/heads/main",
        "boundarymesh-dev",
        "Adellaghmari/boundarymesh/.github/workflows/wif-verify.yml@refs/heads/main",
        "boundarymesh-ci-preview",
        "Adellaghmari/boundarymesh/.github/workflows/infra-preview.yml@refs/heads/main",
        "boundarymesh-artifact-publish",
        "Adellaghmari/boundarymesh/.github/workflows/artifact-publish.yml@refs/heads/main",
        "boundarymesh-deploy",
        "boundarymesh-hosting-deploy",
        "Adellaghmari/boundarymesh/.github/workflows/deploy-release.yml@refs/heads/main",
        "Adellaghmari/boundarymesh/.github/workflows/recover-promotion-evidence.yml@refs/heads/main",
        "workflow_dispatch",
    ]) {
        assert.ok(wif.includes(expected), `provider condition must include ${expected}`);
    }
    assert.match(wif, /GITHUB_OIDC_COMMON_CONDITIONS = \[/);
    assert.match(wif, /GITHUB_OIDC_VERIFICATION_CONTEXT_CONDITIONS = \[/);
    assert.match(wif, /GITHUB_OIDC_CI_PREVIEW_CONTEXT_CONDITIONS = \[/);
    assert.match(wif, /GITHUB_OIDC_ARTIFACT_PUBLISH_CONTEXT_CONDITIONS = \[/);
    assert.match(wif, /GITHUB_OIDC_RUNTIME_DEPLOY_CONTEXT_CONDITIONS = \[/);
    assert.match(wif, /GITHUB_OIDC_HOSTING_DEPLOY_CONTEXT_CONDITIONS = \[/);
    assert.match(wif, /GITHUB_OIDC_EVIDENCE_RECOVERY_CONTEXT_CONDITIONS = \[/);
    assert.match(
        wif,
        /`\(\(\$\{GITHUB_OIDC_VERIFICATION_CONTEXT_CONDITIONS\.join\(" && "\)\}\) \|\| `/,
    );
    assert.equal((wif.match(/\|\|/g) ?? []).length, 5);
    assert.equal(privilegedContexts.length, 6);
    assert.equal(/\.github\/workflows\/\*/.test(wif), false);
    assert.equal((wif.match(/^\s*"(?:google|attribute)\.[^"]+":\s*"assertion\.[^"]+",$/gm) ?? []).length, 9);
});

test("provider semantics allow only the six exact privileged contexts", () => {
    for (const context of privilegedContexts) {
        assert.equal(providerAllows({ ...commonClaims, ...context }), true);
    }
});

test("provider semantics reject immutable-claim, context, workflow, ref, and event mutations", () => {
    const verification = { ...commonClaims, ...privilegedContexts[0] };
    const ciPreview = { ...commonClaims, ...privilegedContexts[1] };
    const runtimeDeploy = { ...commonClaims, ...privilegedContexts[3] };
    const hostingDeploy = { ...commonClaims, ...privilegedContexts[4] };
    const evidenceRecovery = { ...commonClaims, ...privilegedContexts[5] };
    const rejected = [
        { ...verification, repository_id: "9999999999" },
        { ...verification, repository_owner_id: "999999999" },
        { ...verification, repository_visibility: "public" },
        { ...verification, ref: "refs/heads/feature" },
        { ...verification, ref: "refs/tags/v1.0.0" },
        { ...verification, environment: "production" },
        { ...verification, workflow_ref: privilegedContexts[1].workflow_ref },
        { ...ciPreview, workflow_ref: privilegedContexts[0].workflow_ref },
        { ...runtimeDeploy, environment: "boundarymesh-deploy-other" },
        { ...hostingDeploy, environment: "boundarymesh-hosting-deploy-other" },
        { ...runtimeDeploy, workflow_ref: privilegedContexts[2].workflow_ref },
        { ...hostingDeploy, event_name: "push" },
        {
            ...verification,
            workflow_ref: "Adellaghmari/boundarymesh/.github/workflows/other.yml@refs/heads/main",
        },
        { ...verification, event_name: "push" },
        { ...verification, event_name: "pull_request" },
        { ...verification, event_name: "pull_request_target" },
        { ...verification, event_name: "schedule" },
        { ...verification, event_name: "workflow_call" },
        { ...verification, event_name: "repository_dispatch" },
        { ...evidenceRecovery, ref: "refs/heads/codex/phase16e6-p4a-recovery-wif-workflow" },
        {
            ...evidenceRecovery,
            workflow_ref: "Adellaghmari/boundarymesh/.github/workflows/other.yml@refs/heads/main",
        },
        { ...evidenceRecovery, event_name: "pull_request" },
        { ...evidenceRecovery, repository_id: "9999999999" },
        { ...evidenceRecovery, repository_owner_id: "999999999" },
        { ...evidenceRecovery, environment: "boundarymesh-hosting-deploy" },
    ];

    for (const assertion of rejected) {
        assert.equal(providerAllows(assertion), false);
    }
});

test("CI preview remains without project reader or passphrase-backed secrets", () => {
    assert.equal(/boundarymeshCiPreviewReader/.test(source + index), false);
    assert.equal(/new gcp\.projects\.IAM(?:Member|Binding|Policy)/.test(source), false);
    assert.equal(existsSync(workflowPath), true);
    assert.equal(/backend:\s*gs:\/\//.test(stack), false);
    assert.match(
        stack,
        /secretsprovider:\s*gcpkms:\/\/projects\/boundarymesh-dev\/locations\/europe-west4\/keyRings\/boundarymesh-pulumi\/cryptoKeys\/state-secrets/,
    );
    assert.equal(/PULUMI_CONFIG_PASSPHRASE|passphrase/i.test(workflow), false);
});

test("dormant infrastructure preview workflow is manual, main-gated, and least privileged", () => {
    assert.match(workflow, /^on:\s*\n\s{2}workflow_dispatch:\s*$/m);
    assert.match(workflow, /^\s{4}environment:\s*boundarymesh-ci-preview\s*$/m);
    assert.match(workflow, /^\s{2}contents:\s*read\s*$/m);
    assert.match(workflow, /^\s{2}id-token:\s*write\s*$/m);
    assert.equal(/^\s*(push|pull_request|pull_request_target|schedule|workflow_call|repository_dispatch):/m.test(workflow), false);
    assert.equal(/^\s{2}(actions|checks|deployments|issues|packages|pull-requests|security-events|statuses):/m.test(workflow), false);
});

test("manual workflows expose only the bounded live federation test modes", () => {
    assert.match(
        workflow,
        /options:\s*\n\s*- positive\s*\n\s*- cross_sa_denied\s*\n\s*- wrong_context_denied/,
    );
    assert.match(
        verificationWorkflow,
        /options:\s*\n\s*- positive\s*\n\s*- cross_sa_denied\s*\n\s*- wrong_context_denied\s*\n\s*- missing_environment_denied/,
    );
    for (const sourceText of [workflow, verificationWorkflow]) {
        assert.match(sourceText, /^on:\s*\n\s{2}workflow_dispatch:\s*$/m);
        assert.equal(
            /^\s*(push|pull_request|pull_request_target|schedule|workflow_call|repository_dispatch):/m.test(
                sourceText,
            ),
            false,
        );
        assert.equal(/^\s{2}(actions|checks|deployments|issues|packages|pull-requests|security-events|statuses):/m.test(sourceText), false);
        assert.equal(/service_account_key|credentials_json|private_key|client_secret|roles\/iam\.serviceAccountTokenCreator/i.test(sourceText), false);
        assert.equal(/\$\{\{\s*steps\.[^}]+\.outputs\.|toJSON\s*\(/i.test(sourceText), false);
        assert.equal(/echo\s+.*(?:token|credential|secret|password)/i.test(sourceText), false);
    }
});

test("negative harnesses prove provider and service-account boundaries without exporting credentials", () => {
    for (const sourceText of [workflow, verificationWorkflow]) {
        assert.match(sourceText, /id:\s*admitted_auth/);
        assert.match(sourceText, /id:\s*forbidden_auth/);
        assert.match(sourceText, /continue-on-error:\s*true/);
        assert.match(sourceText, /if:\s*\$\{\{ always\(\) \}\}/);
        assert.match(sourceText, /ADMITTED_OUTCOME:\s*\$\{\{ steps\.admitted_auth\.outcome \}\}/);
        assert.match(sourceText, /FORBIDDEN_OUTCOME:\s*\$\{\{ steps\.forbidden_auth\.outcome \}\}/);
        assert.match(sourceText, /create_credentials_file:\s*false/);
        assert.match(sourceText, /export_environment_variables:\s*false/);
        assert.match(sourceText, /google-github-actions\/auth@7c6bc770dae815cd3e89ee6cdf493a5fab2cc093/);
    }

    assert.match(workflow, /environment:\s*boundarymesh-ci-preview[\s\S]*?service_account:\s*boundarymesh-wif-verify@/);
    assert.match(workflow, /environment:\s*boundarymesh-dev[\s\S]*?service_account:\s*boundarymesh-wif-verify@/);
    assert.match(verificationWorkflow, /environment:\s*boundarymesh-dev[\s\S]*?service_account:\s*boundarymesh-ci-preview@/);
    assert.match(verificationWorkflow, /environment:\s*boundarymesh-ci-preview[\s\S]*?service_account:\s*boundarymesh-ci-preview@/);
    assert.match(verificationWorkflow, /missing-environment-denial:[\s\S]*?runs-on:\s*ubuntu-latest\s*\n\s*permissions:/);
});

test("infrastructure preview workflow uses only the frozen keyless identity and backend", () => {
    assert.match(
        workflow,
        /google-github-actions\/auth@7c6bc770dae815cd3e89ee6cdf493a5fab2cc093/,
    );
    assert.match(
        workflow,
        /projects\/891977254468\/locations\/global\/workloadIdentityPools\/boundarymesh-github\/providers\/github-actions/,
    );
    assert.match(workflow, /service_account:\s*boundarymesh-ci-preview@boundarymesh-dev\.iam\.gserviceaccount\.com/);
    assert.match(workflow, /create_credentials_file:\s*true/);
    assert.match(workflow, /export_environment_variables:\s*true/);
    assert.match(workflow, /pulumi-version:\s*3\.262\.0/);
    assert.match(workflow, /pulumi login gs:\/\/bm-pulumi-state-eu4-21cad20b325f/);
    assert.equal(/service_account_key|credentials_json|private_key|client_secret|roles\/iam\.serviceAccountTokenCreator/i.test(workflow), false);
});

test("infrastructure workflow can only run the exact non-refresh preview", () => {
    const pulumiCommands = workflow.match(/^\s*run:\s*pulumi\s+.*$/gm) ?? [];
    assert.deepEqual(pulumiCommands.map((line) => line.trim()), [
        "run: pulumi login gs://bm-pulumi-state-eu4-21cad20b325f",
        "run: pulumi preview --stack dev --diff --non-interactive --suppress-permalink",
    ]);
    assert.equal(/\brefresh\b|pulumi\s+up|pulumi\s+destroy|pulumi\s+import|pulumi\s+stack\s+(migrate|change-secrets-provider)/i.test(workflow), false);
    assert.equal(/echo\s+.*(?:token|credential|secret|password)/i.test(workflow), false);
});
