import assert from "node:assert/strict";
import { readFileSync, readdirSync } from "node:fs";
import { dirname, join } from "node:path";
import { test } from "node:test";
import { fileURLToPath } from "node:url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const srcDir = join(root, "src");

const forbidden = [
    /\bnew\s+gcp\.cloudrun\./,
    /\bnew\s+gcp\.firestore\.Index/,
    /\bnew\s+gcp\.firestore\.Field/,
    /\bnew\s+gcp\.firestore\.BackupSchedule/,
    /\bnew\s+gcp\.secretmanager\.SecretVersion\b/,
    /\bnew\s+gcp\.firebase\./,
    /\bnew\s+gcp\.serviceaccount\.Key\b/,
    /\bnew\s+gcp\.compute\./,
    /\bnew\s+gcp\.dns\./,
    /\bnew\s+gcp\.Provider\b/,
];

function sourceFiles() {
    return readdirSync(srcDir)
        .filter((name) => name.endsWith(".ts"))
        .map((name) => ({ name, text: readFileSync(join(srcDir, name), "utf8") }));
}

test("existing private workloads remain constrained while 15J-B adds only a dedicated web edge", () => {
    const text = sourceFiles()
        .map((file) => file.text)
        .join("\n");
    for (const token of forbidden) {
        assert.equal(token.test(text), false, `infra/src must not contain ${token}`);
    }
    assert.match(text, /new gcp\.projects\.Service/);
    assert.match(text, /new gcp\.artifactregistry\.Repository/);
    assert.match(text, /new gcp\.secretmanager\.Secret/);
    assert.match(text, /new gcp\.firestore\.Database/);
    assert.match(text, /artifactregistry\.googleapis\.com/);
    assert.match(text, /secretmanager\.googleapis\.com/);
    assert.match(text, /firestore\.googleapis\.com/);
    assert.match(text, /pubsub\.googleapis\.com/);
    assert.match(text, /cloudkms\.googleapis\.com/);
    assert.match(text, /run\.googleapis\.com/);
    assert.match(text, /"firestore-api"/);
    assert.match(text, /"pubsub-api"/);
    assert.match(text, /"kms-api"/);
    assert.match(text, /"attestation-key-ring"/);
    assert.match(text, /"attestation-signing-key"/);
    assert.match(text, /name:\s*"boundarymesh-attestation"/);
    assert.match(text, /name:\s*"exchange-attestation"/);
    assert.match(text, /purpose:\s*"ASYMMETRIC_SIGN"/);
    assert.match(text, /algorithm:\s*"EC_SIGN_P256_SHA256"/);
    assert.match(text, /protectionLevel:\s*"SOFTWARE"/);
    assert.match(text, /"delivery-topic"/);
    assert.match(text, /"delivery-worker-subscription"/);
    assert.match(text, /"delivery-dlq-topic"/);
    assert.match(text, /"delivery-dlq-inspect-subscription"/);
    assert.match(text, /name:\s*"boundarymesh-deliveries"/);
    assert.match(text, /name:\s*"boundarymesh-delivery-worker"/);
    assert.equal(/deadLetterPolicy/.test(text), false);
    assert.equal(/pushEndpoint/.test(text), false);
    assert.match(text, /"boundarymesh-database"/);
    assert.match(text, /name:\s*"boundarymesh"/);
    assert.match(text, /locationId:\s*foundation\.region/);
    assert.match(text, /type:\s*"FIRESTORE_NATIVE"/);
    assert.match(text, /databaseEdition:\s*"STANDARD"/);
    assert.match(text, /appEngineIntegrationMode:\s*"DISABLED"/);
    assert.match(text, /concurrencyMode:\s*"PESSIMISTIC"/);
    assert.match(text, /deleteProtectionState:\s*"DELETE_PROTECTION_ENABLED"/);
    assert.match(text, /repositoryId:\s*"boundarymesh"/);
    assert.match(text, /"hmac-quayforge"/);
    assert.match(text, /"covecrm-api"/);
    assert.match(text, /"lumenway-api"/);
    assert.match(text, /"rivertide-api"/);
    assert.equal(/new gcp\.secretmanager\.SecretVersion/.test(text), false);
    assert.match(text, /new gcp\.cloudrunv2\.Service/);
    assert.match(text, /new gcp\.cloudrunv2\.WorkerPool/);
    assert.match(text, /accountId:\s*API_SERVICE_ACCOUNT_ID/);
    assert.match(text, /accountId:\s*WORKER_SERVICE_ACCOUNT_ID/);
    assert.match(text, /roles\/datastore\.user/);
    assert.match(text, /roles\/pubsub\.publisher/);
    assert.match(text, /roles\/pubsub\.subscriber/);
    assert.match(text, /roles\/cloudkms\.signer/);
    assert.match(text, /roles\/cloudkms\.publicKeyViewer/);
    assert.match(text, /roles\/secretmanager\.secretAccessor/);
    assert.equal(/roles\/(owner|editor|viewer|pubsub\.admin|secretmanager\.admin|cloudkms\.admin|datastore\.owner)/i.test(text), false);
    const coreWorkloads = readFileSync(join(srcDir, "workloads.ts"), "utf8");
    assert.equal(/allUsers|allAuthenticatedUsers/.test(coreWorkloads), false);
    assert.equal(/allAuthenticatedUsers/.test(text), false);
    assert.equal(/roles\/cloudkms\.[^"\n]+[\s\S]{0,160}workerPrincipal/.test(text), false);
    assert.match(text, /manualInstanceCount:\s*1/);
    assert.match(text, /minInstanceCount:\s*0/);
    assert.match(text, /maxInstanceCount:\s*2/);
    assert.match(text, /invokerIamDisabled:\s*false/);
    assert.match(text, /PHASE_15J_C1E_API_IMAGE/);
    assert.match(text, /PHASE_15I_C1_WORKER_IMAGE/);
    assert.equal(/\(default\)/.test(text), false);
    assert.equal(/client_secret|private_key|BEGIN [A-Z]+ PRIVATE/i.test(text), false);
    assert.equal((text.match(/new gcp\.projects\.Service/g) ?? []).length, 9);
    assert.equal((text.match(/new gcp\.artifactregistry\.Repository/g) ?? []).length, 1);
    assert.equal((text.match(/new gcp\.secretmanager\.Secret\(/g) ?? []).length, 1);
    assert.equal((text.match(/new gcp\.firestore\.Database/g) ?? []).length, 1);
    assert.equal((text.match(/new gcp\.pubsub\.Topic\(/g) ?? []).length, 2);
    assert.equal((text.match(/new gcp\.pubsub\.Subscription\(/g) ?? []).length, 2);
    assert.equal((text.match(/new gcp\.storage\.Bucket\(/g) ?? []).length, 1);
    assert.equal((text.match(/new gcp\.kms\.KeyRing/g) ?? []).length, 2);
    assert.equal((text.match(/new gcp\.kms\.CryptoKey\(/g) ?? []).length, 2);
    assert.equal((text.match(/new gcp\.serviceaccount\.Account/g) ?? []).length, 5);
    assert.equal((text.match(/new gcp\.serviceaccount\.IAMMember/g) ?? []).length, 2);
    assert.equal((text.match(/new gcp\.iam\.WorkloadIdentityPool\(/g) ?? []).length, 1);
    assert.equal((text.match(/new gcp\.iam\.WorkloadIdentityPoolProvider/g) ?? []).length, 1);
    assert.equal((text.match(/new gcp\.projects\.IAMMember/g) ?? []).length, 3);
    assert.equal((text.match(/new gcp\.pubsub\.TopicIAMMember/g) ?? []).length, 1);
    assert.equal((text.match(/new gcp\.pubsub\.SubscriptionIAMMember/g) ?? []).length, 1);
    assert.equal((text.match(/new gcp\.kms\.CryptoKeyIAMMember/g) ?? []).length, 3);
    assert.equal((text.match(/new gcp\.secretmanager\.SecretIamMember/g) ?? []).length, 2);
    assert.equal((text.match(/new gcp\.cloudrunv2\.Service\(/g) ?? []).length, 2);
    assert.equal((text.match(/new gcp\.cloudrunv2\.WorkerPool/g) ?? []).length, 1);
    assert.equal((text.match(/new gcp\.cloudrunv2\.ServiceIamMember/g) ?? []).length, 2);
    assert.equal((text.match(/new gcp\.projects\.IAMCustomRole/g) ?? []).length, 2);
    assert.equal((text.match(/new gcp\.storage\.BucketIAMMember/g) ?? []).length, 1);
    const edge = readFileSync(join(srcDir, "web-edge.ts"), "utf8");
    assert.match(edge, /name:\s*coreApi\.name,[\s\S]*?role:\s*"roles\/run\.invoker",\s*member:\s*principal/);
    assert.match(edge, /name:\s*service\.name,[\s\S]*?member:\s*"allUsers"/);
    assert.equal(/roles\/(owner|editor|run\.admin|cloudkms|pubsub|secretmanager|datastore)/i.test(edge), false);
});

test("GitHub WIF foundation defines a tightly scoped provider and keyless verification principal", () => {
    const wif = readFileSync(join(srcDir, "wif.ts"), "utf8");
    const workflow = readFileSync(join(root, "..", ".github", "workflows", "wif-verify.yml"), "utf8");

    assert.equal((wif.match(/new gcp\.projects\.Service/g) ?? []).length, 3);
    assert.match(wif, /service:\s*"iam\.googleapis\.com"/);
    assert.match(wif, /service:\s*"iamcredentials\.googleapis\.com"/);
    assert.match(wif, /service:\s*"sts\.googleapis\.com"/);
    assert.equal(/compute\.googleapis\.com/.test(wif), false);
    assert.equal((wif.match(/disableOnDestroy:\s*false/g) ?? []).length, 3);

    assert.equal((wif.match(/new gcp\.iam\.WorkloadIdentityPool\(/g) ?? []).length, 1);
    assert.equal((wif.match(/new gcp\.iam\.WorkloadIdentityPoolProvider/g) ?? []).length, 1);
    assert.match(wif, /GITHUB_WIF_POOL_ID = "boundarymesh-github"/);
    assert.match(wif, /GITHUB_WIF_PROVIDER_ID = "github-actions"/);
    assert.match(wif, /issuerUri:\s*"https:\/\/token\.actions\.githubusercontent\.com"/);
    assert.equal(/allowedAudiences/.test(wif), false);

    const mappings = [
        ["google.subject", "assertion.sub"],
        ["attribute.repository_id", "assertion.repository_id"],
        ["attribute.repository_owner_id", "assertion.repository_owner_id"],
        ["attribute.repository_visibility", "assertion.repository_visibility"],
        ["attribute.ref", "assertion.ref"],
        ["attribute.environment", "assertion.environment"],
        ["attribute.workflow_ref", "assertion.workflow_ref"],
        ["attribute.event_name", "assertion.event_name"],
        ["attribute.repository", "assertion.repository"],
    ];
    for (const [attribute, assertion] of mappings) {
        assert.ok(wif.includes(`"${attribute}": "${assertion}"`));
    }
    assert.equal((wif.match(/^\s*"(?:google|attribute)\.[^"]+":\s*"assertion\.[^"]+",$/gm) ?? []).length, 9);

    for (const requiredCondition of [
        "1385415640",
        "179922674",
        "repository_visibility == 'private'",
        'GITHUB_MAIN_REF = "refs/heads/main"',
        'GITHUB_VERIFICATION_ENVIRONMENT = "boundarymesh-dev"',
        "Adellaghmari/boundarymesh/.github/workflows/wif-verify.yml@refs/heads/main",
        'GITHUB_CI_PREVIEW_ENVIRONMENT = "boundarymesh-ci-preview"',
        "Adellaghmari/boundarymesh/.github/workflows/infra-preview.yml@refs/heads/main",
        'GITHUB_WORKFLOW_DISPATCH_EVENT = "workflow_dispatch"',
    ]) {
        assert.ok(wif.includes(requiredCondition), `condition must include ${requiredCondition}`);
    }
    assert.equal(/job_workflow_ref/.test(wif), false);
    assert.equal((wif.match(/\|\|/g) ?? []).length, 1);
    assert.match(wif, /\]\.join\(" && "\)/);

    assert.equal((wif.match(/new gcp\.serviceaccount\.Account/g) ?? []).length, 1);
    assert.match(wif, /WIF_VERIFICATION_SERVICE_ACCOUNT_ID = "boundarymesh-wif-verify"/);
    assert.equal(/new gcp\.serviceaccount\.Key/.test(wif), false);
    assert.equal((wif.match(/new gcp\.serviceaccount\.IAMMember/g) ?? []).length, 1);
    assert.equal((wif.match(/role:\s*"roles\/iam\.workloadIdentityUser"/g) ?? []).length, 1);
    assert.equal((wif.match(/serviceAccountId:\s*verificationServiceAccount\.name/g) ?? []).length, 1);
    assert.equal(
        /workloadIdentityPools\/\$\{GITHUB_WIF_POOL_ID\}\/attribute\.repository_id\/\$\{GITHUB_REPOSITORY_ID\}/.test(
            wif,
        ),
        false,
    );
    assert.match(wif, /GITHUB_VERIFICATION_ENVIRONMENT = "boundarymesh-dev"/);
    assert.match(
        wif,
        /workloadIdentityPools\/\$\{GITHUB_WIF_POOL_ID\}\/attribute\.environment\/\$\{GITHUB_VERIFICATION_ENVIRONMENT\}/,
    );
    assert.match(
        wif,
        /"wif-verification-environment-impersonation"[\s\S]*?serviceAccountId:\s*verificationServiceAccount\.name,[\s\S]*?role:\s*"roles\/iam\.workloadIdentityUser",[\s\S]*?member:\s*environmentPrincipal/,
    );
    assert.equal(/new gcp\.projects\.IAM(Member|Binding|Policy)/.test(wif), false);
    assert.equal(/roles\/(owner|editor|run\.|artifactregistry\.|firebase|secretmanager|datastore|pubsub)/i.test(wif), false);
    assert.equal(/API_SERVICE_ACCOUNT_ID|WORKER_SERVICE_ACCOUNT_ID|boundarymesh-web/.test(wif), false);

    const resourceCreates = [
        /new gcp\.projects\.Service/g,
        /new gcp\.iam\.WorkloadIdentityPool\(/g,
        /new gcp\.iam\.WorkloadIdentityPoolProvider/g,
        /new gcp\.serviceaccount\.Account/g,
        /new gcp\.serviceaccount\.IAMMember/g,
    ].reduce((count, pattern) => count + (wif.match(pattern) ?? []).length, 0);
    assert.equal(resourceCreates, 7);

    assert.match(workflow, /^\s*workflow_dispatch:\s*$/m);
    assert.match(workflow, /^\s*environment:\s*boundarymesh-dev\s*$/m);
    assert.match(workflow, /^\s*contents:\s*read\s*$/m);
    assert.match(workflow, /^\s*id-token:\s*write\s*$/m);
});

test("Hosting rewrites select the BFF before the SPA and never target the private API", () => {
    const hosting = JSON.parse(readFileSync(join(root, "..", "firebase.json"), "utf8")).hosting;
    assert.equal(hosting.public, "frontend/dist");
    assert.deepEqual(hosting.rewrites.map((rule) => rule.source), ["/session/**", "/api/**", "**"]);
    assert.equal(hosting.rewrites[0].run.serviceId, "boundarymesh-web");
    assert.equal(hosting.rewrites[1].run.serviceId, "boundarymesh-web");
    assert.equal(hosting.rewrites[2].destination, "/index.html");
});

test("Hosting revalidates every SPA shell request before overriding only hashed assets", () => {
    const hosting = JSON.parse(readFileSync(join(root, "..", "firebase.json"), "utf8")).hosting;
    assert.deepEqual(hosting.headers.map((rule) => rule.source), ["**", "/assets/**"]);
    const shellHeaders = Object.fromEntries(hosting.headers[0].headers.map(({ key, value }) => [key, value]));
    const assetHeaders = Object.fromEntries(hosting.headers[1].headers.map(({ key, value }) => [key, value]));
    assert.equal(shellHeaders["Cache-Control"], "no-cache");
    assert.equal(assetHeaders["Cache-Control"], "public,max-age=31536000,immutable");
    assert.equal(hosting.rewrites.at(-1).destination, "/index.html");
});

test("stack YAML is non-secret and pins the verified GCP project", () => {
    const stack = readFileSync(join(root, "Pulumi.dev.yaml"), "utf8");
    assert.equal(stack.includes("secure:"), false);
    assert.equal(/GOOGLE_APPLICATION_CREDENTIALS|private_key|client_secret/i.test(stack), false);
    assert.equal(/^\s*(access_token|refresh_token|credentials):/im.test(stack), false);
    assert.match(stack, /boundarymesh:environment:\s*dev/);
    assert.match(stack, /gcp:project:\s*boundarymesh-dev/);
    assert.match(stack, /gcp:region:\s*europe-west4/);
});
