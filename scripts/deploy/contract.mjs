import { createHash } from "node:crypto";
import { createReadStream, existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { basename, dirname, isAbsolute, join, normalize, relative, resolve, sep } from "node:path";
import { execFileSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import Ajv2020 from "ajv/dist/2020.js";
import addFormats from "ajv-formats";
import * as tar from "tar";
import { validateReleaseManifest } from "../release/release-manifest.mjs";

const here = dirname(fileURLToPath(import.meta.url));
export const repositoryRoot = resolve(here, "..", "..");
export const FROZEN_LOCK_PATH = join(here, "frozen-release.json");
export const RELEASE_SCHEMA_PATH = join(repositoryRoot, "contracts", "release-manifest", "v1.schema.json");
export const EVIDENCE_SCHEMA_PATH = join(repositoryRoot, "contracts", "deployment-evidence", "v1.schema.json");
export const OPERATIONS = Object.freeze(["stage", "promote", "rollback"]);
export const OCI_DIGEST_REFERENCE = /^[a-z0-9.-]+(?:\/[a-z0-9._-]+)+@sha256:[0-9a-f]{64}$/;
export const RUN_ID = /^[1-9][0-9]*$/;

export function loadFrozenRelease(path = FROZEN_LOCK_PATH) {
    const lock = JSON.parse(readFileSync(path, "utf8"));
    if (lock.schemaVersion !== "boundarymesh.deployment-lock/v1") {
        throw new Error("Unsupported deployment lock schema.");
    }
    return Object.freeze(lock);
}

export function sha256Bytes(value) {
    return createHash("sha256").update(value).digest("hex");
}

export function sha256File(path) {
    return sha256Bytes(readFileSync(path));
}

export function canonicalize(value) {
    if (Array.isArray(value)) return value.map(canonicalize);
    if (value && typeof value === "object") {
        return Object.fromEntries(Object.keys(value).sort().map((key) => [key, canonicalize(value[key])]));
    }
    return value;
}

export function canonicalJson(value) {
    return `${JSON.stringify(canonicalize(value), null, 2)}\n`;
}

function validatorFor(schemaPath) {
    const ajv = new Ajv2020({ allErrors: true, strict: true });
    addFormats(ajv);
    return ajv.compile(JSON.parse(readFileSync(schemaPath, "utf8")));
}

const releaseSchemaValidator = validatorFor(RELEASE_SCHEMA_PATH);
const evidenceSchemaValidator = validatorFor(EVIDENCE_SCHEMA_PATH);

function requireSchema(validate, value, label) {
    if (!validate(value)) {
        throw new Error(`${label} schema validation failed: ${validate.errors.map((entry) => `${entry.instancePath || "/"} ${entry.message}`).join("; ")}`);
    }
}

function exact(value, expected, label) {
    if (value !== expected) throw new Error(`${label} differs from the frozen deployment lock.`);
}

export function validateDispatchInputs({ operation, stageRunId = "", promotionRunId = "" }) {
    if (!OPERATIONS.includes(operation)) throw new Error("operation must be exactly stage, promote, or rollback.");
    if (operation === "stage") {
        if (stageRunId || promotionRunId) throw new Error("stage must not receive a prior run reference.");
    } else if (operation === "promote") {
        if (!RUN_ID.test(stageRunId) || promotionRunId) throw new Error("promote requires only an explicit numeric stage_run_id.");
    } else if (!RUN_ID.test(stageRunId) || !RUN_ID.test(promotionRunId)) {
        throw new Error("rollback requires explicit numeric stage_run_id and promotion_run_id values.");
    }
    return { operation, stageRunId: stageRunId || null, promotionRunId: promotionRunId || null };
}

export function verifyFrozenReleaseBytes(bytes, lock = loadFrozenRelease()) {
    exact(sha256Bytes(bytes), lock.releaseManifestSha256, "release manifest SHA-256");
    const manifest = JSON.parse(Buffer.from(bytes).toString("utf8"));
    requireSchema(releaseSchemaValidator, manifest, "release manifest");
    validateReleaseManifest(manifest);
    exact(`${manifest.repository.owner}/${manifest.repository.name}`, lock.repository, "repository");
    exact(manifest.repository.id, lock.repositoryId, "repository ID");
    exact(manifest.source.commitSha, lock.sourceSha, "source SHA");
    exact(manifest.source.ref, lock.sourceRef, "source ref");
    exact(manifest.publication.runId, lock.publicationRunId, "publication run");
    exact(manifest.publication.workflowPath, lock.publicationWorkflow, "publication workflow");
    exact(manifest.publication.environment, lock.publicationEnvironment, "publication environment");
    exact(manifest.artifacts.releaseManifest.package, lock.releasePackage, "release package");
    exact(manifest.artifacts.releaseManifest.version, lock.releaseVersion, "release version");
    for (const subject of ["api", "worker", "web"]) {
        exact(manifest.artifacts[subject].digest, lock.images[subject], `${subject} image digest`);
        const reference = `${manifest.artifacts[subject].repository}/${manifest.artifacts[subject].image}@${manifest.artifacts[subject].digest}`;
        if (!OCI_DIGEST_REFERENCE.test(reference) || reference.includes(":" + lock.sourceSha)) {
            throw new Error(`${subject} image identity is not an immutable OCI digest reference.`);
        }
    }
    exact(manifest.artifacts.frontend.package, lock.frontend.package, "frontend package");
    exact(manifest.artifacts.frontend.version, lock.frontend.version, "frontend version");
    exact(manifest.artifacts.frontend.filename, lock.frontend.archive, "frontend filename");
    exact(manifest.artifacts.frontend.sha256, lock.frontend.archiveSha256, "frontend archive SHA-256");
    return manifest;
}

export function verifyFrozenReleaseFile(path, lock = loadFrozenRelease()) {
    return verifyFrozenReleaseBytes(readFileSync(path), lock);
}

export function frozenImageReferences(manifest, lock = loadFrozenRelease()) {
    verifyFrozenReleaseBytes(Buffer.from(canonicalJson(manifest)), lock);
    return Object.fromEntries(["api", "worker", "web"].map((subject) => [
        subject,
        `${manifest.artifacts[subject].repository}/${manifest.artifacts[subject].image}@${manifest.artifacts[subject].digest}`,
    ]));
}

export function evidenceVersion(operation, runId, lock = loadFrozenRelease()) {
    if (!RUN_ID.test(String(runId))) throw new Error("Evidence run ID must be numeric and immutable.");
    const infix = operation === "stage" ? "run" : operation === "promote" ? "promote-run" : operation === "rollback" ? "rollback-run" : null;
    if (!infix) throw new Error("Unknown evidence operation.");
    return `${lock.sourceSha}-${infix}-${runId}`;
}

export function validateDeploymentEvidence(evidence, { operation, runId, lock = loadFrozenRelease() } = {}) {
    requireSchema(evidenceSchemaValidator, evidence, "deployment evidence");
    exact(evidence.release.sourceSha, lock.sourceSha, "evidence source SHA");
    exact(evidence.release.manifestSha256, lock.releaseManifestSha256, "evidence manifest SHA-256");
    exact(evidence.release.publicationRunId, lock.publicationRunId, "evidence publication run");
    if (operation) exact(evidence.operation, operation, "evidence operation");
    if (runId) exact(evidence.workflow.runId, String(runId), "evidence workflow run");
    if (evidence.operation === "stage") {
        const names = { api: "boundarymesh-api", web: "boundarymesh-web-edge", worker: "boundarymesh-worker" };
        for (const subject of ["api", "web", "worker"]) {
            const candidate = evidence.runtime.result.candidates[subject];
            exact(candidate.image, `${lock.region}-docker.pkg.dev/${lock.project}/${lock.dockerRepository}/${names[subject]}@${lock.images[subject]}`, `${subject} candidate image`);
            const allocation = subject === "worker" ? candidate.split : candidate.traffic;
            const candidateEntry = allocation.find((entry) => entry.revision === candidate.revision);
            if (!candidateEntry || candidateEntry.percent !== 0) throw new Error(`${subject} stage candidate must remain at zero production allocation.`);
            if (allocation.reduce((total, entry) => total + entry.percent, 0) !== 100) throw new Error(`${subject} stage production allocation must total exactly 100 percent.`);
        }
    }
    if (evidence.operation === "promote") {
        const names = { api: "boundarymesh-api", web: "boundarymesh-web-edge", worker: "boundarymesh-worker" };
        for (const subject of ["api", "web", "worker"]) {
            const result = evidence.runtime.result[subject];
            const frozenImage = `${lock.region}-docker.pkg.dev/${lock.project}/${lock.dockerRepository}/${names[subject]}@${lock.images[subject]}`;
            if (evidence.status === "PROMOTED") exact(result.image, frozenImage, `${subject} promoted image`);
            else if (result.image !== frozenImage && result.image !== evidence.runtime.before[subject].image) throw new Error(`${subject} failed-promotion state is outside the staged candidate/baseline set.`);
            const allocation = subject === "worker" ? result.split : result.traffic;
            if (allocation.length !== 1 || allocation[0].percent !== 100 || allocation[0].revision !== result.revision) throw new Error(`${subject} promotion evidence does not identify one 100-percent production revision.`);
        }
    }
    if (evidence.operation === "rollback") {
        for (const subject of ["api", "web", "worker"]) {
            const expected = evidence.rollback[`${subject}Revision`];
            if (evidence.runtime.result[subject].revision !== expected) throw new Error(`${subject} rollback result differs from the retained revision.`);
        }
        if (evidence.hosting.result.version !== evidence.rollback.firebaseVersion) throw new Error("Hosting rollback result differs from the retained version.");
    }
    return evidence;
}

export function verifyEvidenceFiles(documentPath, sidecarPath, options = {}) {
    const expected = readFileSync(sidecarPath, "utf8").trim();
    const match = /^([0-9a-f]{64})  deployment-evidence\.json$/.exec(expected);
    if (!match) throw new Error("Invalid deployment evidence SHA-256 sidecar format.");
    exact(sha256File(documentPath), match[1], "deployment evidence SHA-256");
    return validateDeploymentEvidence(JSON.parse(readFileSync(documentPath, "utf8")), options);
}

export function writeDeploymentEvidence(evidence, directory, options = {}) {
    validateDeploymentEvidence(evidence, options);
    mkdirSync(directory, { recursive: true });
    const documentPath = join(directory, "deployment-evidence.json");
    const sidecarPath = `${documentPath}.sha256`;
    if (existsSync(documentPath) || existsSync(sidecarPath)) throw new Error("Refusing to overwrite immutable deployment evidence.");
    const body = canonicalJson(evidence);
    writeFileSync(documentPath, body, { flag: "wx" });
    writeFileSync(sidecarPath, `${sha256Bytes(body)}  deployment-evidence.json\n`, { flag: "wx" });
    return { documentPath, sidecarPath };
}

export function normalizeTraffic(entries = []) {
    return entries.map((entry) => ({
        type: entry.type ?? null,
        revision: entry.revision ? String(entry.revision).split("/").at(-1) : null,
        percent: Number(entry.percent ?? 0),
        tag: entry.tag ?? null,
    })).sort((a, b) => JSON.stringify(a).localeCompare(JSON.stringify(b)));
}

function withoutImage(container) {
    const copy = structuredClone(container);
    delete copy.image;
    return copy;
}

export function runtimeInvariant(resource, iamPolicy = null) {
    const template = resource.template ?? {};
    const result = {
        name: resource.name,
        description: resource.description ?? null,
        labels: resource.labels ?? {},
        annotations: resource.annotations ?? {},
        ingress: resource.ingress ?? null,
        launchStage: resource.launchStage ?? null,
        binaryAuthorization: resource.binaryAuthorization ?? null,
        scaling: resource.scaling ?? null,
        invokerIamDisabled: resource.invokerIamDisabled ?? null,
        defaultUriDisabled: resource.defaultUriDisabled ?? null,
        iapEnabled: resource.iapEnabled ?? null,
        customAudiences: resource.customAudiences ?? [],
        sshEnabled: resource.sshEnabled ?? null,
        template: {
            labels: template.labels ?? {},
            annotations: template.annotations ?? {},
            scaling: template.scaling ?? null,
            vpcAccess: template.vpcAccess ?? null,
            timeout: template.timeout ?? null,
            serviceAccount: template.serviceAccount ?? null,
            containers: (template.containers ?? []).map(withoutImage),
            volumes: template.volumes ?? [],
            executionEnvironment: template.executionEnvironment ?? null,
            encryptionKey: template.encryptionKey ?? null,
            maxInstanceRequestConcurrency: template.maxInstanceRequestConcurrency ?? null,
            sessionAffinity: template.sessionAffinity ?? null,
            healthCheckDisabled: template.healthCheckDisabled ?? null,
            nodeSelector: template.nodeSelector ?? null,
        },
        iam: iamPolicy ? canonicalize({ bindings: (iamPolicy.bindings ?? []).map((binding) => ({ role: binding.role, members: [...(binding.members ?? [])].sort(), condition: binding.condition ?? null })).sort((a, b) => a.role.localeCompare(b.role)) }) : null,
    };
    return canonicalize(result);
}

export function assertInvariantUnchanged(before, after, label = "runtime") {
    if (canonicalJson(before) !== canonicalJson(after)) throw new Error(`${label} configuration or IAM changed outside the deployment allowlist.`);
}

export function assertEtag(expected, actual) {
    if (!expected || expected !== actual) throw new Error("ETag mismatch; refusing stale mutation.");
}

export function assertStageBaseline(current, recorded) {
    for (const field of ["api", "web", "worker", "hosting"]) {
        if (canonicalJson(current[field]) !== canonicalJson(recorded[field])) throw new Error(`Stale stage baseline: ${field} production state changed.`);
    }
}

function safeArchivePath(path) {
    const unix = path.replaceAll("\\", "/");
    const normalized = normalize(unix).replaceAll("\\", "/");
    return !isAbsolute(path) && !/^[A-Za-z]:/.test(path) && !normalized.startsWith("../") && normalized !== ".." && (normalized === "frontend-dist" || normalized.startsWith("frontend-dist/"));
}

export async function inspectFrontendArchive(archivePath) {
    const entries = [];
    const violations = [];
    await tar.list({
        file: archivePath,
        strict: true,
        onReadEntry(entry) {
            const type = entry.type;
            if (!safeArchivePath(entry.path)) violations.push(`Unsafe or unexpected archive path: ${entry.path}`);
            if (!["File", "Directory"].includes(type)) violations.push(`Archive links and special entries are forbidden: ${entry.path}`);
            entries.push({ path: entry.path.replaceAll("\\", "/"), type, size: entry.size });
        },
    });
    if (violations.length) throw new Error(violations.join("; "));
    if (!entries.some((entry) => entry.type === "File")) throw new Error("Frontend archive contains no files.");
    return entries;
}

export async function extractFrontendArchive({ archivePath, destination, expectedSha256, lock = loadFrozenRelease() }) {
    exact(sha256File(archivePath), expectedSha256 ?? lock.frontend.archiveSha256, "frontend archive SHA-256");
    const entries = await inspectFrontendArchive(archivePath);
    mkdirSync(destination, { recursive: true });
    await tar.extract({ file: archivePath, cwd: destination, strip: 1, preservePaths: false, strict: true });
    return entries;
}

export function materializeFrozenFirebaseConfig({ destination, sourceSha, lock = loadFrozenRelease() }) {
    exact(sourceSha ?? lock.sourceSha, lock.sourceSha, "Firebase config source SHA");
    const bytes = execFileSync("git", ["cat-file", "blob", `${lock.sourceSha}:${lock.firebase.configPath}`], { cwd: repositoryRoot, encoding: null });
    exact(sha256Bytes(bytes), lock.firebase.configSha256, "release-source firebase.json SHA-256");
    mkdirSync(dirname(destination), { recursive: true });
    writeFileSync(destination, bytes, { flag: "wx" });
    return destination;
}

export function assertWithin(parent, child) {
    const offset = relative(resolve(parent), resolve(child));
    if (offset.startsWith(`..${sep}`) || offset === ".." || isAbsolute(offset)) throw new Error("Path escapes its trusted working directory.");
}

function option(name) {
    const index = process.argv.indexOf(name);
    return index >= 0 ? process.argv[index + 1] : undefined;
}

async function main() {
    const command = process.argv[2];
    if (command === "validate-inputs") {
        process.stdout.write(`${JSON.stringify(validateDispatchInputs({ operation: option("--operation"), stageRunId: option("--stage-run-id") ?? "", promotionRunId: option("--promotion-run-id") ?? "" }))}\n`);
    } else if (command === "verify-release") {
        const manifest = verifyFrozenReleaseFile(option("--manifest"));
        process.stdout.write(`${JSON.stringify({ valid: true, images: frozenImageReferences(manifest) })}\n`);
    } else if (command === "verify-evidence") {
        verifyEvidenceFiles(option("--document"), option("--sidecar"), { operation: option("--operation"), runId: option("--run-id") });
        process.stdout.write("deployment evidence verified\n");
    } else if (command === "verify-archive") {
        await extractFrontendArchive({ archivePath: option("--archive"), destination: option("--destination") });
        process.stdout.write("frontend archive verified and safely extracted\n");
    } else if (command === "materialize-firebase-config") {
        materializeFrozenFirebaseConfig({ destination: option("--destination") });
        process.stdout.write("release-source firebase.json verified and materialized\n");
    } else if (command) {
        throw new Error(`Unknown deployment contract command: ${command}`);
    }
}

if (resolve(process.argv[1] ?? "") === resolve(fileURLToPath(import.meta.url))) await main();
