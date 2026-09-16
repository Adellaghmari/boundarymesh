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
    /\bnew\s+gcp\.storage\./,
    /\bnew\s+gcp\.compute\./,
    /\bnew\s+gcp\.dns\./,
    /\bnew\s+gcp\.iam\./,
    /\bnew\s+gcp\.Provider\b/,
];

function sourceFiles() {
    return readdirSync(srcDir)
        .filter((name) => name.endsWith(".ts"))
        .map((name) => ({ name, text: readFileSync(join(srcDir, name), "utf8") }));
}

test("Phase 15I-A adds only the approved keyless workload preview and least-privilege IAM", () => {
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
    assert.equal(/allUsers|allAuthenticatedUsers/.test(text), false);
    assert.equal(/roles\/cloudkms\.[^"\n]+[\s\S]{0,160}workerPrincipal/.test(text), false);
    assert.match(text, /manualInstanceCount:\s*1/);
    assert.match(text, /minInstanceCount:\s*0/);
    assert.match(text, /maxInstanceCount:\s*2/);
    assert.match(text, /invokerIamDisabled:\s*false/);
    assert.match(text, /PHASE_15I_C1_API_IMAGE/);
    assert.match(text, /PHASE_15I_C1_WORKER_IMAGE/);
    assert.equal(/\(default\)/.test(text), false);
    assert.equal(/client_secret|private_key|BEGIN [A-Z]+ PRIVATE/i.test(text), false);
    assert.equal((text.match(/new gcp\.projects\.Service/g) ?? []).length, 6);
    assert.equal((text.match(/new gcp\.artifactregistry\.Repository/g) ?? []).length, 1);
    assert.equal((text.match(/new gcp\.secretmanager\.Secret\(/g) ?? []).length, 1);
    assert.equal((text.match(/new gcp\.firestore\.Database/g) ?? []).length, 1);
    assert.equal((text.match(/new gcp\.pubsub\.Topic\(/g) ?? []).length, 2);
    assert.equal((text.match(/new gcp\.pubsub\.Subscription\(/g) ?? []).length, 2);
    assert.equal((text.match(/new gcp\.kms\.KeyRing/g) ?? []).length, 1);
    assert.equal((text.match(/new gcp\.kms\.CryptoKey\(/g) ?? []).length, 1);
    assert.equal((text.match(/new gcp\.serviceaccount\.Account/g) ?? []).length, 2);
    assert.equal((text.match(/new gcp\.projects\.IAMMember/g) ?? []).length, 2);
    assert.equal((text.match(/new gcp\.pubsub\.TopicIAMMember/g) ?? []).length, 1);
    assert.equal((text.match(/new gcp\.pubsub\.SubscriptionIAMMember/g) ?? []).length, 1);
    assert.equal((text.match(/new gcp\.kms\.CryptoKeyIAMMember/g) ?? []).length, 2);
    assert.equal((text.match(/new gcp\.secretmanager\.SecretIamMember/g) ?? []).length, 2);
    assert.equal((text.match(/new gcp\.cloudrunv2\.Service/g) ?? []).length, 1);
    assert.equal((text.match(/new gcp\.cloudrunv2\.WorkerPool/g) ?? []).length, 1);
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
