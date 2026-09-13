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
    /\bnew\s+gcp\.secretmanager\.SecretIam/,
    /\bnew\s+gcp\.kms\./,
    /\bnew\s+gcp\.firebase\./,
    /\bnew\s+gcp\.serviceaccount\./,
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

test("Phase 15G-A program may define Artifact Registry, Secret Manager, Firestore, and Pub/Sub delivery topology", () => {
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
    assert.match(text, /"firestore-api"/);
    assert.match(text, /"pubsub-api"/);
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
    assert.equal(/\(default\)/.test(text), false);
    assert.equal(/client_secret|private_key|BEGIN [A-Z]+ PRIVATE/i.test(text), false);
    assert.equal((text.match(/new gcp\.projects\.Service/g) ?? []).length, 4);
    assert.equal((text.match(/new gcp\.artifactregistry\.Repository/g) ?? []).length, 1);
    assert.equal((text.match(/new gcp\.secretmanager\.Secret/g) ?? []).length, 1);
    assert.equal((text.match(/new gcp\.firestore\.Database/g) ?? []).length, 1);
    assert.equal((text.match(/new gcp\.pubsub\.Topic/g) ?? []).length, 2);
    assert.equal((text.match(/new gcp\.pubsub\.Subscription/g) ?? []).length, 2);
    assert.equal((text.match(/new gcp\./g) ?? []).length, 11);
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
