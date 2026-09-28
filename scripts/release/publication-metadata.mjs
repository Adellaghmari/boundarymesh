import { createHash } from "node:crypto";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { FULL_GIT_SHA, OCI_DIGEST, SHA256, serializeReleaseManifest } from "./release-manifest.mjs";

const OCI_REPOSITORY = "europe-west4-docker.pkg.dev/boundarymesh-dev/boundarymesh";
const GENERIC_REPOSITORY = "projects/boundarymesh-dev/locations/europe-west4/repositories/boundarymesh-releases";
const IMAGE_SUBJECTS = Object.freeze({ api: "boundarymesh-api", worker: "boundarymesh-worker", web: "boundarymesh-web-edge" });

function sha256File(file) {
    return createHash("sha256").update(readFileSync(resolve(file))).digest("hex");
}

function readJson(file, purpose) {
    try { return JSON.parse(readFileSync(resolve(file), "utf8")); }
    catch { throw new Error(`Invalid ${purpose} JSON: ${file}`); }
}

function requireString(value, name, pattern) {
    if (typeof value !== "string" || value.length === 0 || (pattern && !pattern.test(value))) throw new Error(`Invalid publication metadata ${name}.`);
}

function requireEvidenceObject(value, name) {
    if (!value || typeof value !== "object" || Array.isArray(value) || Object.keys(value).length === 0) {
        throw new Error(`Missing published ${name} evidence.`);
    }
}

function parseImageEvidence({ subject, metadataFile, indexFile, sbomFile, provenanceFile }) {
    const image = IMAGE_SUBJECTS[subject];
    if (!image) throw new Error(`Unknown OCI evidence subject: ${subject}`);
    const metadata = readJson(metadataFile, `${subject} Buildx metadata`);
    const digest = metadata["containerimage.digest"];
    requireString(digest, `${subject} image digest`, OCI_DIGEST);
    const index = readJson(indexFile, `${subject} OCI index`);
    if (!Array.isArray(index.manifests) || !index.manifests.some((descriptor) => descriptor?.annotations?.["vnd.docker.reference.type"] === "attestation-manifest")) {
        throw new Error(`Published ${subject} OCI index has no attestation manifest.`);
    }
    const sbom = readJson(sbomFile, `${subject} published SBOM`);
    const provenance = readJson(provenanceFile, `${subject} published provenance`);
    requireEvidenceObject(sbom, `${subject} SBOM`);
    requireEvidenceObject(provenance, `${subject} provenance`);
    const reference = `${OCI_REPOSITORY}/${image}@${digest}`;
    return {
        image: { repository: OCI_REPOSITORY, image, digest },
        sbom: { subject, reference: `${reference}#sbom`, digest: sha256File(sbomFile) },
        provenance: { subject, reference: `${reference}#provenance`, digest: sha256File(provenanceFile) },
    };
}

function parseArchiveSidecar(archiveFile, sidecarFile) {
    const archiveSha = sha256File(archiveFile);
    const sidecar = readFileSync(resolve(sidecarFile), "utf8");
    if (sidecar !== `${archiveSha}  frontend-dist.tar.gz\n`) throw new Error("Frontend archive SHA-256 sidecar does not match archive bytes.");
    return archiveSha;
}

function genericEvidence({ subject, filename, file }) {
    const digest = sha256File(file);
    return {
        subject,
        reference: `${GENERIC_REPOSITORY}/packages/boundarymesh-frontend/versions/{commit}/files/${filename}`,
        digest,
    };
}

export function buildPublicationMetadata({ source, publication, images, frontend, toolVersions, actionPins }) {
    requireString(source.commitSha, "source commit", FULL_GIT_SHA);
    requireString(source.contractTreeSha, "contract tree SHA", FULL_GIT_SHA);
    requireString(publication.runId, "run ID", /^\d+$/);
    const parsedImages = Object.fromEntries(Object.entries(IMAGE_SUBJECTS).map(([subject]) => [subject, parseImageEvidence({ subject, ...images[subject] })]));
    const archiveSha = parseArchiveSidecar(frontend.archiveFile, frontend.archiveSidecarFile);
    const frontendSbom = genericEvidence({ subject: "frontend", filename: "frontend-sbom.cdx.json", file: frontend.sbomFile });
    const frontendProvenance = genericEvidence({ subject: "frontend", filename: "frontend-dist.tar.gz.intoto.json", file: frontend.provenanceFile });
    frontendSbom.reference = frontendSbom.reference.replace("{commit}", source.commitSha);
    frontendProvenance.reference = frontendProvenance.reference.replace("{commit}", source.commitSha);
    const manifest = {
        schemaVersion: "boundarymesh.release/v1",
        repository: { owner: "Adellaghmari", name: "boundarymesh", id: "1385415640" },
        source: { commitSha: source.commitSha, ref: "refs/heads/main", contractPath: "contracts/release-manifest/v1.schema.json", contractTreeSha: source.contractTreeSha },
        publication: { workflowPath: ".github/workflows/artifact-publish.yml", runId: publication.runId, environment: "boundarymesh-artifact-publish", timestamp: publication.timestamp },
        artifacts: {
            api: parsedImages.api.image, worker: parsedImages.worker.image, web: parsedImages.web.image,
            frontend: { repository: GENERIC_REPOSITORY, package: "boundarymesh-frontend", version: source.commitSha, filename: "frontend-dist.tar.gz", sha256: archiveSha },
            releaseManifest: { repository: GENERIC_REPOSITORY, package: "boundarymesh-release", version: source.commitSha, filename: "release-manifest.json", sha256File: "release-manifest.json.sha256", hashStrategy: "external-sha256-sidecar" },
        },
        sboms: [parsedImages.api.sbom, parsedImages.worker.sbom, parsedImages.web.sbom, frontendSbom],
        provenance: [parsedImages.api.provenance, parsedImages.worker.provenance, parsedImages.web.provenance, frontendProvenance],
        toolVersions, actionPins,
    };
    return JSON.parse(serializeReleaseManifest(manifest));
}

export function writePublicationMetadata({ outputFile, ...input }) {
    const output = resolve(outputFile);
    if (existsSync(output)) throw new Error(`Refusing to overwrite immutable publication metadata: ${output}`);
    const metadata = buildPublicationMetadata(input);
    writeFileSync(output, serializeReleaseManifest(metadata), { flag: "wx" });
    return metadata;
}

function values(name) {
    return process.argv.flatMap((argument, index) => argument === name ? [process.argv[index + 1]] : []);
}
function option(name) { return values(name)[0]; }
function keyed(values, name) {
    return Object.fromEntries(values.map((value) => {
        const index = value.indexOf("=");
        if (index < 1) throw new Error(`Invalid ${name} value.`);
        return [value.slice(0, index), value.slice(index + 1)];
    }));
}

if (resolve(process.argv[1] ?? "") === resolve(fileURLToPath(import.meta.url))) {
    const imageFiles = keyed(values("--image"), "--image");
    const imageInput = Object.fromEntries(Object.keys(IMAGE_SUBJECTS).map((subject) => {
        const base = imageFiles[subject];
        if (!base) throw new Error(`Missing --image ${subject}=<directory>.`);
        return [subject, { metadataFile: `${base}/${subject}.metadata.json`, indexFile: `${base}/${subject}.index.json`, sbomFile: `${base}/${subject}.sbom.json`, provenanceFile: `${base}/${subject}.provenance.json` }];
    }));
    const outputFile = option("--output");
    const archiveFile = option("--frontend-archive");
    const archiveSidecarFile = option("--frontend-archive-sidecar");
    const sbomFile = option("--frontend-sbom");
    const provenanceFile = option("--frontend-provenance");
    const sourceCommit = option("--source-commit");
    const contractTreeSha = option("--contract-tree-sha");
    const runId = option("--run-id");
    const timestamp = option("--timestamp");
    if (![outputFile, archiveFile, archiveSidecarFile, sbomFile, provenanceFile, sourceCommit, contractTreeSha, runId, timestamp].every(Boolean)) {
        throw new Error("Usage: publication-metadata.mjs requires output, source, publication, frontend, and OCI evidence inputs.");
    }
    writePublicationMetadata({ outputFile, source: { commitSha: sourceCommit, contractTreeSha }, publication: { runId, timestamp }, images: imageInput, frontend: { archiveFile, archiveSidecarFile, sbomFile, provenanceFile }, toolVersions: keyed(values("--tool-version"), "--tool-version"), actionPins: keyed(values("--action-pin"), "--action-pin") });
}
