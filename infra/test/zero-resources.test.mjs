import assert from "node:assert/strict";
import { readFileSync, readdirSync } from "node:fs";
import { dirname, join } from "node:path";
import { test } from "node:test";
import { fileURLToPath } from "node:url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const srcDir = join(root, "src");

const forbidden = [
    /\bnew\s+CustomResource\b/,
    /\bextends\s+CustomResource\b/,
    /\bnew\s+ComponentResource\b/,
    /\bextends\s+ComponentResource\b/,
    /\bnew\s+gcp\./,
    /\bgcp\.cloudrun\b/,
    /\bgcp\.pubsub\b/,
    /\bgcp\.firestore\b/,
    /\bgcp\.secretmanager\b/,
    /\bgcp\.kms\b/,
    /\bgcp\.artifactregistry\b/,
    /\bgcp\.firebase\b/,
    /\bgcp\.serviceaccount\b/,
];

function sourceFiles() {
    return readdirSync(srcDir)
        .filter((name) => name.endsWith(".ts"))
        .map((name) => ({ name, text: readFileSync(join(srcDir, name), "utf8") }));
}

test("Phase 15A program defines zero Pulumi or GCP resources", () => {
    for (const file of sourceFiles()) {
        for (const token of forbidden) {
            assert.equal(token.test(file.text), false, `${file.name} must not contain ${token}`);
        }
    }
});

test("stack YAML stays non-secret and defers the GCP project to Phase 15B", () => {
    const stack = readFileSync(join(root, "Pulumi.dev.yaml"), "utf8");
    assert.equal(stack.includes("secure:"), false);
    assert.match(stack, /boundarymesh:environment:\s*dev/);
    assert.match(stack, /boundarymesh:region:\s*europe-west4/);
    assert.match(stack, /boundarymesh:gcpProjectId:\s*REPLACE_IN_PHASE_15B/);
});
