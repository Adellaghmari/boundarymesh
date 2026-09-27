import assert from "node:assert/strict";
import { mkdtempSync, mkdirSync, readFileSync, rmSync, writeFileSync, existsSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join } from "node:path";
import { test } from "node:test";
import { fileURLToPath } from "node:url";
import { buildDeterministicFrontendArchive } from "../../scripts/release/build-frontend-archive.mjs";
import { serializeReleaseManifest, writeReleaseManifest } from "../../scripts/release/release-manifest.mjs";

const infraRoot = join(dirname(fileURLToPath(import.meta.url)), "..");
const repositoryRoot = join(infraRoot, "..");
const source = readFileSync(join(infraRoot, "src", "artifact-publication.ts"), "utf8");
const registry = readFileSync(join(infraRoot, "src", "registry.ts"), "utf8");
const wif = readFileSync(join(infraRoot, "src", "wif.ts"), "utf8");
const templatePath = join(repositoryRoot, "docs", "templates", "artifact-publish.yml");
const template = readFileSync(templatePath, "utf8");
const workflow = readFileSync(join(repositoryRoot, ".github", "workflows", "artifact-publish.yml"), "utf8");
const schema = JSON.parse(readFileSync(join(repositoryRoot, "contracts", "release-manifest", "v1.schema.json"), "utf8"));

const sha = "a".repeat(40);
const hash = "b".repeat(64);
const digest = `sha256:${"c".repeat(64)}`;

function manifestFixture() {
    const image = (name) => ({
        repository: "europe-west4-docker.pkg.dev/boundarymesh-dev/boundarymesh",
        image: name,
        digest,
    });
    const evidence = ["api", "worker", "web", "frontend"].map((subject) => ({
        subject,
        reference: `evidence/${subject}.json`,
        digest: hash,
    }));
    return {
        schemaVersion: "boundarymesh.release/v1",
        repository: { owner: "Adellaghmari", name: "boundarymesh", id: "1385415640" },
        source: {
            commitSha: sha,
            ref: "refs/heads/main",
            contractPath: "contracts/release-manifest/v1.schema.json",
            contractTreeSha: sha,
        },
        publication: {
            workflowPath: ".github/workflows/artifact-publish.yml",
            runId: "123456",
            environment: "boundarymesh-artifact-publish",
            timestamp: "2026-09-27T12:00:00.000Z",
        },
        artifacts: {
            api: image("boundarymesh-api"),
            worker: image("boundarymesh-worker"),
            web: image("boundarymesh-web-edge"),
            frontend: {
                repository: "projects/boundarymesh-dev/locations/europe-west4/repositories/boundarymesh-releases",
                package: "boundarymesh-frontend",
                version: sha,
                filename: "frontend-dist.tar.gz",
                sha256: hash,
            },
            releaseManifest: {
                repository: "projects/boundarymesh-dev/locations/europe-west4/repositories/boundarymesh-releases",
                package: "boundarymesh-release",
                version: sha,
                filename: "release-manifest.json",
                sha256File: "release-manifest.json.sha256",
                hashStrategy: "external-sha256-sidecar",
            },
        },
        sboms: evidence,
        provenance: evidence,
        toolVersions: { java: "25", maven: "3.9", node: "22", npm: "10", docker: "29", buildx: "0.36" },
        actionPins: { checkout: sha, auth: sha },
    };
}

test("publication identity is dedicated, keyless, and creates exactly five scoped resources", () => {
    const creates = [
        /new gcp\.serviceaccount\.Account/g,
        /new gcp\.artifactregistry\.Repository\(/g,
        /new gcp\.artifactregistry\.RepositoryIamMember/g,
        /new gcp\.serviceaccount\.IAMMember/g,
    ].reduce((sum, pattern) => sum + (source.match(pattern) ?? []).length, 0);
    assert.equal(creates, 5);
    assert.match(source, /ARTIFACT_PUBLISHER_SERVICE_ACCOUNT_ID = "boundarymesh-artifact-publish"/);
    assert.equal((source.match(/new gcp\.artifactregistry\.RepositoryIamMember/g) ?? []).length, 2);
    assert.match(source, /role:\s*ARTIFACT_WRITER_ROLE/g);
    assert.equal((source.match(/role:\s*ARTIFACT_WRITER_ROLE/g) ?? []).length, 2);
    assert.match(source, /role:\s*"roles\/iam\.workloadIdentityUser"/);
    assert.match(source, /attribute\.environment\/\$\{GITHUB_ARTIFACT_PUBLISH_ENVIRONMENT\}/);
    assert.equal(/new gcp\.serviceaccount\.Key/.test(source), false);
    assert.equal(/new gcp\.projects\.IAM(?:Member|Binding|Policy)/.test(source), false);
    assert.equal(/roles\/iam\.(?:serviceAccountTokenCreator|serviceAccountUser)/.test(source), false);
    assert.equal(/secretmanager|datastore|firestore|pubsub|cloudkms|run\.admin|storage\./i.test(source), false);
});

test("Artifact Registry publication is immutable, regional, repository-scoped, and retention-safe", () => {
    assert.match(registry, /repositoryId:\s*"boundarymesh"/);
    assert.match(registry, /dockerConfig:\s*\{ immutableTags: true \}/);
    assert.match(source, /RELEASE_REPOSITORY_ID = "boundarymesh-releases"/);
    assert.match(source, /format:\s*"GENERIC"/);
    assert.match(source, /location:\s*foundation\.region/);
    assert.match(source, /deletionPolicy:\s*"PREVENT"/);
    assert.equal(/cleanupPolicies|cleanupPolicyDryRun/.test(source + registry), false);
    assert.equal(/new gcp\.projects\.IAM/.test(source), false);
    assert.match(source, /repository:\s*dependencies\.dockerRepository\.repositoryId/);
    assert.match(source, /repository:\s*releaseRepository\.repositoryId/);
});

test("WIF publication branch is the exact third context and admits no other event", () => {
    assert.match(wif, /GITHUB_ARTIFACT_PUBLISH_ENVIRONMENT = "boundarymesh-artifact-publish"/);
    assert.match(wif, /GITHUB_ARTIFACT_PUBLISH_WORKFLOW_REF =\s*\n\s*"Adellaghmari\/boundarymesh\/\.github\/workflows\/artifact-publish\.yml@refs\/heads\/main"/);
    assert.equal((wif.match(/\|\|/g) ?? []).length, 2);
    for (const forbidden of ["push", "pull_request", "pull_request_target", "schedule", "workflow_call", "repository_dispatch"]) {
        assert.equal(new RegExp(`event_name == ['\"]${forbidden}`).test(wif), false);
    }
});

test("publication workflow is manual-only, least privileged, and matches the reviewed contract", () => {
    assert.equal(existsSync(join(repositoryRoot, ".github", "workflows", "artifact-publish.yml")), true);
    assert.match(template, /NON-RUNNABLE PHASE 16D WORKFLOW CONTRACT/);
    assert.match(workflow, /^\s*workflow_dispatch:\s*$/m);
    assert.match(workflow, /^\s{2}contents:\s*read\s*$/m);
    assert.match(workflow, /^\s{2}id-token:\s*write\s*$/m);
    assert.match(workflow, /^\s{4}environment:\s*boundarymesh-artifact-publish\s*$/m);
    assert.match(workflow, /if: github\.ref == 'refs\/heads\/main'/);
    assert.match(workflow, /service_account: boundarymesh-artifact-publish@boundarymesh-dev\.iam\.gserviceaccount\.com/);
    assert.equal(/^(?:\s*)(?:push|pull_request|pull_request_target|schedule|workflow_call|repository_dispatch):/m.test(workflow), false);
    const actions = [...workflow.matchAll(/uses:\s*[^@\s]+@([^\s]+)/g)].map((match) => match[1]);
    assert.ok(actions.length >= 5);
    assert.ok(actions.every((pin) => /^[0-9a-f]{40}$/.test(pin)));
    assert.equal(/pulumi\s+(?:up|refresh)|gcloud\s+run\s+deploy|firebase\s+deploy|service_account_key|credentials_json|private_key|client_secret/i.test(workflow), false);
    assert.equal(/echo\s+.*(?:token|credential|secret|password)/i.test(workflow), false);
    assert.equal(/:latest\b/.test(workflow), false);
});

test("all production Docker stages pin validated linux amd64 digests", () => {
    for (const path of ["backend/api/Dockerfile", "backend/worker/Dockerfile", "backend/web-edge/Dockerfile"]) {
        const dockerfile = readFileSync(join(repositoryRoot, path), "utf8");
        const from = dockerfile.match(/^FROM\s+\S+/gm) ?? [];
        assert.equal(from.length, 2);
        assert.ok(from.every((line) => /eclipse-temurin:25\.0\.4_7-(?:jdk|jre)-noble@sha256:[0-9a-f]{64}/.test(line)));
    }
});

test("frontend archive is deterministic and normalizes ustar metadata", () => {
    const temp = mkdtempSync(join(tmpdir(), "boundarymesh-release-"));
    try {
        const input = join(temp, "dist");
        mkdirSync(join(input, "assets"), { recursive: true });
        writeFileSync(join(input, "index.html"), "<main>BoundaryMesh</main>\n");
        writeFileSync(join(input, "assets", "app-abc.js"), "console.log('stable');\n");
        const first = join(temp, "first.tar.gz");
        const second = join(temp, "second.tar.gz");
        const a = buildDeterministicFrontendArchive({ inputDirectory: input, outputFile: first, sourceDateEpoch: 1700000000 });
        const b = buildDeterministicFrontendArchive({ inputDirectory: input, outputFile: second, sourceDateEpoch: 1700000000 });
        assert.equal(a.sha256, b.sha256);
        assert.deepEqual(readFileSync(first), readFileSync(second));
        assert.equal(a.entryCount, 2);
        assert.match(readFileSync(`${first}.sha256`, "utf8"), new RegExp(`^${a.sha256}  first\\.tar\\.gz\\n$`));
    } finally {
        rmSync(temp, { recursive: true, force: true });
    }
});

test("release manifest is canonical, validates immutable identities, and uses an external self-hash", () => {
    const manifest = manifestFixture();
    const serialized = serializeReleaseManifest(manifest);
    assert.equal(serialized, serializeReleaseManifest(JSON.parse(serialized)));
    assert.equal(schema.properties.schemaVersion.const, "boundarymesh.release/v1");
    assert.equal(schema.additionalProperties, false);
    assert.equal(schema.properties.artifacts.properties.releaseManifest.properties.hashStrategy.const, "external-sha256-sidecar");
    assert.equal(/"(?:deployed|deploymentStatus|environmentStatus)"/.test(serialized), false);

    for (const mutate of [
        (value) => { value.source.commitSha = "short"; },
        (value) => { value.artifacts.api.digest = "sha256:bad"; },
        (value) => { value.artifacts.frontend.sha256 = "bad"; },
        (value) => { value.deployed = false; },
    ]) {
        const invalid = manifestFixture();
        mutate(invalid);
        assert.throws(() => serializeReleaseManifest(invalid));
    }

    const temp = mkdtempSync(join(tmpdir(), "boundarymesh-manifest-"));
    try {
        const input = join(temp, "input.json");
        const output = join(temp, "release-manifest.json");
        writeFileSync(input, JSON.stringify(manifest));
        const result = writeReleaseManifest({ inputFile: input, outputFile: output });
        assert.match(result, /^[0-9a-f]{64}$/);
        assert.match(readFileSync(`${output}.sha256`, "utf8"), new RegExp(`^${result}  release-manifest\\.json\\n$`));
    } finally {
        rmSync(temp, { recursive: true, force: true });
    }
});
