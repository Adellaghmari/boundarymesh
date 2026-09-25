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

test("verification provider remains byte-for-byte limited to its original context", () => {
    assert.equal(/boundarymesh-ci-preview|infra-preview/.test(wif), false);
    assert.equal(/\|\|/.test(wif), false);
    assert.match(wif, /environment == 'boundarymesh-dev'/);
    assert.match(wif, /Adellaghmari\/boundarymesh\/\.github\/workflows\/wif-verify\.yml@refs\/heads\/main/);
    assert.match(wif, /event_name == 'workflow_dispatch'/);
    assert.equal((wif.match(/^\s*"(?:google|attribute)\.[^"]+":\s*"assertion\.[^"]+",$/gm) ?? []).length, 9);
});

test("initial CI phase adds no project reader, workflow, or backend migration", () => {
    assert.equal(/boundarymeshCiPreviewReader/.test(source + index), false);
    assert.equal(/new gcp\.projects\.IAM(?:Member|Binding|Policy)/.test(source), false);
    assert.equal(existsSync(join(repositoryRoot, ".github", "workflows", "infra-preview.yml")), false);
    assert.equal(/backend:\s*gs:\/\//.test(stack), false);
    assert.equal(/secretsprovider:\s*gcpkms:\/\//i.test(stack), false);
});
