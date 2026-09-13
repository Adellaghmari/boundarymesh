import assert from "node:assert/strict";
import { readFileSync, readdirSync } from "node:fs";
import { dirname, join } from "node:path";
import { test } from "node:test";
import { fileURLToPath } from "node:url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const srcDir = join(root, "src");

const forbidden = [
    /\bnew\s+gcp\.cloudrun\./,
    /\bnew\s+gcp\.pubsub\./,
    /\bnew\s+gcp\.firestore\./,
    /\bnew\s+gcp\.secretmanager\./,
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

test("Phase 15D program may define only Artifact Registry resources", () => {
    const text = sourceFiles()
        .map((file) => file.text)
        .join("\n");
    for (const token of forbidden) {
        assert.equal(token.test(text), false, `infra/src must not contain ${token}`);
    }
    assert.match(text, /new gcp\.projects\.Service/);
    assert.match(text, /new gcp\.artifactregistry\.Repository/);
    assert.match(text, /artifactregistry\.googleapis\.com/);
    assert.match(text, /repositoryId:\s*"boundarymesh"/);
    assert.equal((text.match(/new gcp\./g) ?? []).length, 2);
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
