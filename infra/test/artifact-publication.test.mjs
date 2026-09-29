import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import { copyFileSync, existsSync, mkdtempSync, mkdirSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join } from "node:path";
import { test } from "node:test";
import { fileURLToPath } from "node:url";
import { buildDeterministicFrontendArchive } from "../../scripts/release/build-frontend-archive.mjs";
import { generateFrontendProvenance, generateFrontendSbom } from "../../scripts/release/frontend-evidence.mjs";
import { buildPublicationMetadata, writePublicationMetadata } from "../../scripts/release/publication-metadata.mjs";
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

function resolveSha256sum() {
    const candidates = process.platform === "win32"
        ? ["sha256sum", "C:\\Program Files\\Git\\usr\\bin\\sha256sum.exe"]
        : ["sha256sum"];
    for (const candidate of candidates) {
        const probe = spawnSync(candidate, ["--version"], { encoding: "utf8" });
        if (!probe.error && probe.status === 0) return candidate;
    }
    throw new Error("GNU sha256sum is required for publication sidecar tests.");
}

function verifySidecar(directory, sidecar) {
    return spawnSync(resolveSha256sum(), ["--check", sidecar], {
        cwd: directory,
        encoding: "utf8",
    });
}

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

function buildArchiveFixture(temp) {
    const input = join(temp, "dist");
    mkdirSync(join(input, "assets"), { recursive: true });
    writeFileSync(join(input, "index.html"), "<main>BoundaryMesh</main>\n");
    writeFileSync(join(input, "assets", "app-abc.js"), "console.log('stable');\n");
    const archive = join(temp, "frontend-dist.tar.gz");
    const result = buildDeterministicFrontendArchive({ inputDirectory: input, outputFile: archive, sourceDateEpoch: 1700000000 });
    return { archive, ...result };
}

function fakeOciEvidence(temp, subject) {
    const directory = join(temp, subject);
    mkdirSync(directory, { recursive: true });
    writeFileSync(join(directory, `${subject}.metadata.json`), JSON.stringify({ "containerimage.digest": digest }));
    writeFileSync(join(directory, `${subject}.index.json`), JSON.stringify({ manifests: [{ annotations: { "vnd.docker.reference.type": "attestation-manifest" } }] }));
    writeFileSync(join(directory, `${subject}.sbom.json`), JSON.stringify({ bomFormat: "CycloneDX", components: [{ name: subject }] }));
    writeFileSync(join(directory, `${subject}.provenance.json`), JSON.stringify({ _type: "https://in-toto.io/Statement/v1", subject: [{ name: subject }] }));
    return {
        metadataFile: join(directory, `${subject}.metadata.json`),
        indexFile: join(directory, `${subject}.index.json`),
        sbomFile: join(directory, `${subject}.sbom.json`),
        provenanceFile: join(directory, `${subject}.provenance.json`),
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

test("WIF publication context remains exact when deployment contexts are added", () => {
    assert.match(wif, /GITHUB_ARTIFACT_PUBLISH_ENVIRONMENT = "boundarymesh-artifact-publish"/);
    assert.match(wif, /GITHUB_ARTIFACT_PUBLISH_WORKFLOW_REF =\s*\n\s*"Adellaghmari\/boundarymesh\/\.github\/workflows\/artifact-publish\.yml@refs\/heads\/main"/);
    assert.equal((wif.match(/\|\|/g) ?? []).length, 4);
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
    assert.match(workflow, /docker buildx build[\s\S]*--metadata-file/);
    assert.match(workflow, /imagetools inspect "\$reference" --raw/);
    assert.match(workflow, /imagetools inspect "\$reference" --format '\{\{json \.SBOM\}\}'/);
    assert.match(workflow, /imagetools inspect "\$reference" --format '\{\{json \.Provenance\}\}'/);
    assert.match(workflow, /frontend-evidence\.mjs sbom/);
    assert.match(workflow, /frontend-evidence\.mjs provenance/);
    assert.match(workflow, /publication-metadata\.mjs/);
    assert.match(workflow, /release\/frontend-dist\.tar\.gz\.sha256/);
    assert.match(workflow, /release\/release-manifest\.json\.sha256/);
    assert.match(workflow, /cd release\s+sha256sum --check frontend-dist\.tar\.gz\.sha256/);
    assert.match(workflow, /cd release\s+sha256sum --check release-manifest\.json\.sha256/);
    assert.equal(/sha256sum --check release\/(?:frontend-dist\.tar\.gz|release-manifest\.json)\.sha256/.test(workflow), false);
    assert.match(workflow, /--source-directory=release\/frontend-package/);
    assert.match(workflow, /--source-directory=release\/release-package/);
    assert.equal(/artifacts generic upload[^\n]*--source=/.test(workflow), false);
    const collisionCheck = workflow.indexOf("Verify immutable publication targets are absent");
    const firstRemoteWrite = workflow.indexOf("Remote-write boundary: immutable OCI publication begins");
    const metadataGeneration = workflow.indexOf("Generate immutable publication metadata and release manifest");
    const manifestGeneration = workflow.indexOf("release-manifest.mjs");
    assert.ok(collisionCheck >= 0 && firstRemoteWrite > collisionCheck);
    assert.ok(workflow.indexOf("frontend-sbom.cdx.json") < firstRemoteWrite);
    assert.ok(workflow.indexOf("frontend-dist.tar.gz.intoto.json") < firstRemoteWrite);
    assert.ok(metadataGeneration > firstRemoteWrite && manifestGeneration > metadataGeneration);
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

test("portable publication sidecars are verified from their own directory and fail closed", () => {
    const temp = mkdtempSync(join(tmpdir(), "boundarymesh-sidecars-"));
    const release = join(temp, "release");
    mkdirSync(release);
    try {
        const input = join(temp, "dist");
        mkdirSync(input);
        writeFileSync(join(input, "index.html"), "<main>BoundaryMesh</main>\n");
        const archive = join(release, "frontend-dist.tar.gz");
        buildDeterministicFrontendArchive({ inputDirectory: input, outputFile: archive, sourceDateEpoch: 1700000000 });
        assert.match(readFileSync(`${archive}.sha256`, "utf8"), /^[0-9a-f]{64}  frontend-dist\.tar\.gz\n$/);
        assert.equal(verifySidecar(release, "frontend-dist.tar.gz.sha256").status, 0);
        assert.notEqual(verifySidecar(temp, "release/frontend-dist.tar.gz.sha256").status, 0);

        const manifest = join(release, "release-manifest.json");
        writeFileSync(join(temp, "manifest-input.json"), JSON.stringify(manifestFixture()));
        writeReleaseManifest({ inputFile: join(temp, "manifest-input.json"), outputFile: manifest });
        assert.match(readFileSync(`${manifest}.sha256`, "utf8"), /^[0-9a-f]{64}  release-manifest\.json\n$/);
        assert.equal(verifySidecar(release, "release-manifest.json.sha256").status, 0);

        const tampered = join(temp, "tampered");
        mkdirSync(tampered);
        copyFileSync(manifest, join(tampered, "release-manifest.json"));
        copyFileSync(`${manifest}.sha256`, join(tampered, "release-manifest.json.sha256"));
        writeFileSync(join(tampered, "release-manifest.json"), "{}\n");
        assert.notEqual(verifySidecar(tampered, "release-manifest.json.sha256").status, 0);

        rmSync(archive);
        assert.notEqual(verifySidecar(release, "frontend-dist.tar.gz.sha256").status, 0);
    } finally {
        rmSync(temp, { recursive: true, force: true });
    }
});

test("frontend supply-chain evidence is standard, unsigned, and bound to the deterministic archive", () => {
    const temp = mkdtempSync(join(tmpdir(), "boundarymesh-frontend-evidence-"));
    try {
        const { archive, sha256 } = buildArchiveFixture(temp);
        const sbomFile = join(temp, "frontend-sbom.cdx.json");
        const provenanceFile = join(temp, "frontend-dist.tar.gz.intoto.json");
        const sbom = generateFrontendSbom({ frontendDirectory: join(repositoryRoot, "frontend"), outputFile: sbomFile });
        assert.equal(sbom.sbom.bomFormat, "CycloneDX");
        assert.equal(sbom.sbom.specVersion, "1.5");
        assert.equal(sbom.sbom.metadata.component.type, "application");
        assert.equal(sbom.sbom.metadata.component.name, "frontend");
        assert.equal(Object.hasOwn(sbom.sbom, "serialNumber"), false);
        assert.equal(Object.hasOwn(sbom.sbom.metadata, "timestamp"), false);
        const provenance = generateFrontendProvenance({
            archiveFile: archive, outputFile: provenanceFile, sourceCommit: sha, sourceRef: "refs/heads/main",
            workflowPath: ".github/workflows/artifact-publish.yml",
            workflowRef: "Adellaghmari/boundarymesh/.github/workflows/artifact-publish.yml@refs/heads/main",
            runId: "123456", runAttempt: "1", environment: "boundarymesh-artifact-publish", timestamp: "2026-09-27T12:00:00.000Z",
            materialFiles: [
                { uri: "file:frontend/package-lock.json", file: join(repositoryRoot, "frontend", "package-lock.json") },
                { uri: "file:frontend/package.json", file: join(repositoryRoot, "frontend", "package.json") },
                { uri: "file:scripts/release/build-frontend-archive.mjs", file: join(repositoryRoot, "scripts", "release", "build-frontend-archive.mjs") },
            ], nodeVersion: "v22", npmVersion: "11",
        });
        assert.equal(provenance.provenance._type, "https://in-toto.io/Statement/v1");
        assert.equal(provenance.provenance.predicateType, "https://slsa.dev/provenance/v1");
        assert.equal(provenance.provenance.subject[0].digest.sha256, sha256);
        assert.equal(provenance.provenance.predicate.buildDefinition.externalParameters.sourceCommit, sha);
        assert.equal(provenance.provenance.predicate.buildDefinition.externalParameters.sourceRef, "refs/heads/main");
        assert.equal(JSON.stringify(provenance.provenance).includes("signature"), false);
    } finally {
        rmSync(temp, { recursive: true, force: true });
    }
});

test("publication metadata carries four truthful evidence subjects into the release manifest", () => {
    const temp = mkdtempSync(join(tmpdir(), "boundarymesh-publication-metadata-"));
    try {
        const { archive } = buildArchiveFixture(temp);
        const sbomFile = join(temp, "frontend-sbom.cdx.json");
        const provenanceFile = join(temp, "frontend-dist.tar.gz.intoto.json");
        generateFrontendSbom({ frontendDirectory: join(repositoryRoot, "frontend"), outputFile: sbomFile });
        generateFrontendProvenance({
            archiveFile: archive, outputFile: provenanceFile, sourceCommit: sha, sourceRef: "refs/heads/main",
            workflowPath: ".github/workflows/artifact-publish.yml",
            workflowRef: "Adellaghmari/boundarymesh/.github/workflows/artifact-publish.yml@refs/heads/main",
            runId: "123456", runAttempt: "1", environment: "boundarymesh-artifact-publish", timestamp: "2026-09-27T12:00:00.000Z",
            materialFiles: [{ uri: "file:frontend/package-lock.json", file: join(repositoryRoot, "frontend", "package-lock.json") }], nodeVersion: "v22", npmVersion: "11",
        });
        const input = {
            source: { commitSha: sha, contractTreeSha: sha }, publication: { runId: "123456", timestamp: "2026-09-27T12:00:00.000Z" },
            images: { api: fakeOciEvidence(temp, "api"), worker: fakeOciEvidence(temp, "worker"), web: fakeOciEvidence(temp, "web") },
            frontend: { archiveFile: archive, archiveSidecarFile: `${archive}.sha256`, sbomFile, provenanceFile },
            toolVersions: { java: "25", maven: "3.9", node: "22", npm: "11", docker: "29", buildx: "0.36" },
            actionPins: { checkout: sha, auth: sha },
        };
        const metadata = buildPublicationMetadata(input);
        assert.deepEqual(metadata.sboms.map(({ subject }) => subject), ["api", "worker", "web", "frontend"]);
        assert.deepEqual(metadata.provenance.map(({ subject }) => subject), ["api", "worker", "web", "frontend"]);
        assert.match(metadata.sboms[3].reference, /boundarymesh-frontend\/versions\/a{40}\/files\/frontend-sbom\.cdx\.json$/);
        assert.match(metadata.provenance[3].reference, /frontend-dist\.tar\.gz\.intoto\.json$/);
        const metadataFile = join(temp, "publication-metadata.json");
        writePublicationMetadata({ outputFile: metadataFile, ...input });
        const manifestFile = join(temp, "release-manifest.json");
        writeReleaseManifest({ inputFile: metadataFile, outputFile: manifestFile });
        assert.ok(existsSync(`${manifestFile}.sha256`));
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
