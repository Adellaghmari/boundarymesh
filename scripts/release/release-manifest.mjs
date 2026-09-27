import { createHash } from "node:crypto";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { basename, resolve } from "node:path";
import { fileURLToPath } from "node:url";

export const RELEASE_SCHEMA_VERSION = "boundarymesh.release/v1";
export const FULL_GIT_SHA = /^[0-9a-f]{40}$/;
export const SHA256 = /^[0-9a-f]{64}$/;
export const OCI_DIGEST = /^sha256:[0-9a-f]{64}$/;

function requireExactKeys(value, path, required) {
    if (!value || typeof value !== "object" || Array.isArray(value)) {
        throw new Error(`Invalid release manifest field: ${path}`);
    }
    const actual = Object.keys(value).sort();
    const expected = [...required].sort();
    if (actual.length !== expected.length || actual.some((key, index) => key !== expected[index])) {
        throw new Error(`Invalid release manifest fields: ${path}`);
    }
}

function requireString(value, path, pattern) {
    if (typeof value !== "string" || value.length === 0 || (pattern && !pattern.test(value))) {
        throw new Error(`Invalid release manifest field: ${path}`);
    }
}

function validateImage(image, path, expectedImage) {
    requireExactKeys(image, path, ["repository", "image", "digest"]);
    requireString(image.repository, `${path}.repository`);
    if (image.repository !== "europe-west4-docker.pkg.dev/boundarymesh-dev/boundarymesh") {
        throw new Error(`Invalid release manifest field: ${path}.repository`);
    }
    if (image.image !== expectedImage) throw new Error(`Invalid release manifest field: ${path}.image`);
    requireString(image.digest, `${path}.digest`, OCI_DIGEST);
}

function validateEvidenceList(entries, path) {
    if (!Array.isArray(entries) || entries.length < 4) {
        throw new Error(`${path} must contain container and frontend evidence.`);
    }
    for (const [index, entry] of entries.entries()) {
        requireExactKeys(entry, `${path}[${index}]`, ["subject", "reference", "digest"]);
        requireString(entry?.subject, `${path}[${index}].subject`);
        requireString(entry?.reference, `${path}[${index}].reference`);
        requireString(entry?.digest, `${path}[${index}].digest`, SHA256);
    }
}

export function validateReleaseManifest(manifest) {
    if (!manifest || typeof manifest !== "object" || Array.isArray(manifest)) {
        throw new Error("Release manifest must be an object.");
    }
    if (manifest.schemaVersion !== RELEASE_SCHEMA_VERSION) {
        throw new Error("Unsupported release manifest schemaVersion.");
    }
    requireExactKeys(manifest, "manifest", [
        "schemaVersion", "repository", "source", "publication", "artifacts", "sboms",
        "provenance", "toolVersions", "actionPins",
    ]);
    for (const forbidden of ["deployed", "deploymentStatus", "environmentStatus"]) {
        if (Object.hasOwn(manifest, forbidden)) {
            throw new Error(`Mutable deployment field is forbidden: ${forbidden}`);
        }
    }
    requireExactKeys(manifest.repository, "repository", ["owner", "name", "id"]);
    if (manifest.repository.owner !== "Adellaghmari" || manifest.repository.name !== "boundarymesh" || manifest.repository.id !== "1385415640") {
        throw new Error("Invalid release manifest field: repository");
    }
    requireExactKeys(manifest.source, "source", ["commitSha", "ref", "contractPath", "contractTreeSha"]);
    requireString(manifest.source?.commitSha, "source.commitSha", FULL_GIT_SHA);
    requireString(manifest.source?.ref, "source.ref", /^refs\/heads\/main$/);
    requireString(manifest.source?.contractPath, "source.contractPath");
    requireString(manifest.source?.contractTreeSha, "source.contractTreeSha", FULL_GIT_SHA);
    requireExactKeys(manifest.publication, "publication", ["workflowPath", "runId", "environment", "timestamp"]);
    if (manifest.publication.workflowPath !== ".github/workflows/artifact-publish.yml") {
        throw new Error("Invalid release manifest field: publication.workflowPath");
    }
    requireString(manifest.publication?.runId, "publication.runId", /^\d+$/);
    if (manifest.publication.environment !== "boundarymesh-artifact-publish") {
        throw new Error("Invalid release manifest field: publication.environment");
    }
    requireString(manifest.publication?.timestamp, "publication.timestamp");
    if (Number.isNaN(Date.parse(manifest.publication.timestamp))) {
        throw new Error("Invalid release manifest field: publication.timestamp");
    }
    requireExactKeys(manifest.artifacts, "artifacts", ["api", "worker", "web", "frontend", "releaseManifest"]);
    validateImage(manifest.artifacts.api, "artifacts.api", "boundarymesh-api");
    validateImage(manifest.artifacts.worker, "artifacts.worker", "boundarymesh-worker");
    validateImage(manifest.artifacts.web, "artifacts.web", "boundarymesh-web-edge");
    const frontend = manifest.artifacts?.frontend;
    requireExactKeys(frontend, "artifacts.frontend", ["repository", "package", "version", "filename", "sha256"]);
    requireString(frontend?.repository, "artifacts.frontend.repository");
    requireString(frontend?.package, "artifacts.frontend.package");
    requireString(frontend?.version, "artifacts.frontend.version", FULL_GIT_SHA);
    requireString(frontend?.filename, "artifacts.frontend.filename");
    requireString(frontend?.sha256, "artifacts.frontend.sha256", SHA256);
    if (
        frontend.repository !== "projects/boundarymesh-dev/locations/europe-west4/repositories/boundarymesh-releases" ||
        frontend.package !== "boundarymesh-frontend" || frontend.filename !== "frontend-dist.tar.gz"
    ) throw new Error("Invalid release manifest field: artifacts.frontend coordinates");
    const own = manifest.artifacts?.releaseManifest;
    requireExactKeys(own, "artifacts.releaseManifest", ["repository", "package", "version", "filename", "sha256File", "hashStrategy"]);
    requireString(own?.repository, "artifacts.releaseManifest.repository");
    requireString(own?.package, "artifacts.releaseManifest.package");
    requireString(own?.version, "artifacts.releaseManifest.version", FULL_GIT_SHA);
    requireString(own?.filename, "artifacts.releaseManifest.filename");
    requireString(own?.sha256File, "artifacts.releaseManifest.sha256File");
    if (own?.hashStrategy !== "external-sha256-sidecar") {
        throw new Error("Release manifest must use the external SHA-256 sidecar strategy.");
    }
    if (
        own.repository !== "projects/boundarymesh-dev/locations/europe-west4/repositories/boundarymesh-releases" ||
        own.package !== "boundarymesh-release" || own.filename !== "release-manifest.json" ||
        own.sha256File !== "release-manifest.json.sha256"
    ) throw new Error("Invalid release manifest field: artifacts.releaseManifest coordinates");
    validateEvidenceList(manifest.sboms, "sboms");
    validateEvidenceList(manifest.provenance, "provenance");
    const toolFields = ["java", "maven", "node", "npm", "docker", "buildx"];
    requireExactKeys(manifest.toolVersions, "toolVersions", toolFields);
    for (const field of toolFields) {
        requireString(manifest.toolVersions?.[field], `toolVersions.${field}`);
    }
    const actionPins = manifest.actionPins;
    if (!actionPins || typeof actionPins !== "object" || Object.keys(actionPins).length === 0) {
        throw new Error("actionPins must record the publication workflow action commits.");
    }
    for (const [action, pin] of Object.entries(actionPins)) {
        requireString(action, "actionPins action");
        requireString(pin, `actionPins.${action}`, FULL_GIT_SHA);
    }
    return manifest;
}

function canonicalize(value) {
    if (Array.isArray(value)) return value.map(canonicalize);
    if (value && typeof value === "object") {
        return Object.fromEntries(
            Object.keys(value)
                .sort()
                .map((key) => [key, canonicalize(value[key])]),
        );
    }
    return value;
}

export function serializeReleaseManifest(manifest) {
    validateReleaseManifest(manifest);
    return `${JSON.stringify(canonicalize(manifest), null, 2)}\n`;
}

export function writeReleaseManifest({ inputFile, outputFile }) {
    const output = resolve(outputFile);
    if (existsSync(output) || existsSync(`${output}.sha256`)) {
        throw new Error(`Refusing to overwrite immutable release manifest: ${output}`);
    }
    const manifest = JSON.parse(readFileSync(resolve(inputFile), "utf8"));
    const serialized = serializeReleaseManifest(manifest);
    const sha256 = createHash("sha256").update(serialized).digest("hex");
    writeFileSync(output, serialized, { flag: "wx" });
    writeFileSync(`${output}.sha256`, `${sha256}  ${basename(output)}\n`, { flag: "wx" });
    return sha256;
}

function option(name) {
    const index = process.argv.indexOf(name);
    return index >= 0 ? process.argv[index + 1] : undefined;
}

if (resolve(process.argv[1] ?? "") === resolve(fileURLToPath(import.meta.url))) {
    const inputFile = option("--input");
    const outputFile = option("--output");
    if (!inputFile || !outputFile) {
        throw new Error("Usage: node release-manifest.mjs --input <metadata.json> --output <manifest.json>");
    }
    const sha256 = writeReleaseManifest({ inputFile, outputFile });
    process.stdout.write(`release manifest sha256:${sha256}\n`);
}
