import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { test } from "node:test";
import { fileURLToPath } from "node:url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const sharedState = readFileSync(join(root, "src", "shared-state.ts"), "utf8");
const index = readFileSync(join(root, "src", "index.ts"), "utf8");
const stack = readFileSync(join(root, "Pulumi.dev.yaml"), "utf8");

test("shared-state foundation defines one protected recoverable state bucket", () => {
    assert.equal((sharedState.match(/new gcp\.storage\.Bucket\(/g) ?? []).length, 1);
    assert.match(sharedState, /PULUMI_STATE_BUCKET_NAME = "bm-pulumi-state-eu4-21cad20b325f"/);
    assert.match(sharedState, /project:\s*foundation\.gcpProjectId/);
    assert.match(sharedState, /location:\s*foundation\.region/);
    assert.match(stack, /gcp:project:\s*boundarymesh-dev/);
    assert.match(stack, /gcp:region:\s*europe-west4/);
    assert.match(sharedState, /storageClass:\s*"STANDARD"/);
    assert.match(sharedState, /uniformBucketLevelAccess:\s*true/);
    assert.match(sharedState, /publicAccessPrevention:\s*"enforced"/);
    assert.match(sharedState, /versioning:\s*\{\s*enabled:\s*true\s*\}/);
    assert.match(sharedState, /forceDestroy:\s*false/);
    assert.match(
        sharedState,
        /action:\s*\{\s*type:\s*"Delete"\s*\}[\s\S]*daysSinceNoncurrentTime:\s*90[\s\S]*numNewerVersions:\s*10/,
    );
    assert.equal(/\bwebsite\s*:/.test(sharedState), false);
});

test("shared-state KMS resources are separate, symmetric, rotated, and protected", () => {
    assert.equal((sharedState.match(/new gcp\.kms\.KeyRing\(/g) ?? []).length, 1);
    assert.equal((sharedState.match(/new gcp\.kms\.CryptoKey\(/g) ?? []).length, 1);
    assert.match(sharedState, /PULUMI_SECRETS_KEY_RING_NAME = "boundarymesh-pulumi"/);
    assert.match(sharedState, /PULUMI_SECRETS_CRYPTO_KEY_NAME = "state-secrets"/);
    assert.equal(/boundarymesh-attestation|exchange-attestation/.test(sharedState), false);
    assert.match(sharedState, /keyRing:\s*secretsKeyRing\.id/);
    assert.match(sharedState, /purpose:\s*"ENCRYPT_DECRYPT"/);
    assert.match(sharedState, /algorithm:\s*"GOOGLE_SYMMETRIC_ENCRYPTION"/);
    assert.match(sharedState, /protectionLevel:\s*"SOFTWARE"/);
    assert.match(sharedState, /PULUMI_SECRETS_ROTATION_PERIOD = "7776000s"/);
    assert.match(sharedState, /rotationPeriod:\s*PULUMI_SECRETS_ROTATION_PERIOD/);
    assert.equal((sharedState.match(/protect:\s*true/g) ?? []).length, 3);
});

test("shared-state gate adds no identity, access, WIF, role, or API resources", () => {
    const resourceCreates = [
        /new gcp\.storage\.Bucket\(/g,
        /new gcp\.kms\.KeyRing\(/g,
        /new gcp\.kms\.CryptoKey\(/g,
    ].reduce((count, pattern) => count + (sharedState.match(pattern) ?? []).length, 0);
    assert.equal(resourceCreates, 3);

    for (const forbidden of [
        /new gcp\.[A-Za-z0-9_.]*IAM(?:Member|Binding|Policy)/,
        /new gcp\.serviceaccount\./,
        /new gcp\.iam\.WorkloadIdentity/,
        /new gcp\.projects\.IAM/,
        /new gcp\.projects\.Service/,
        /roles\//,
        /allUsers|allAuthenticatedUsers/,
    ]) {
        assert.equal(forbidden.test(sharedState), false, `shared-state source must not contain ${forbidden}`);
    }
});

test("shared-state exports only deterministic non-sensitive migration identifiers", () => {
    assert.match(index, /export const pulumiStateBucketName = sharedState\.stateBucket\.name/);
    assert.match(index, /export const pulumiStateBackendUrl = PULUMI_STATE_BACKEND_URL/);
    assert.match(index, /export const pulumiSecretsKeyRingName = PULUMI_SECRETS_KEY_RING_NAME/);
    assert.match(index, /export const pulumiSecretsCryptoKeyName = PULUMI_SECRETS_CRYPTO_KEY_NAME/);
    assert.match(index, /gcpkms:\/\/projects\/\$\{foundation\.gcpProjectId\}\/locations\/\$\{foundation\.region\}/);
    assert.equal(/secure:|password|credential|private_key|client_secret/i.test(sharedState), false);
});
