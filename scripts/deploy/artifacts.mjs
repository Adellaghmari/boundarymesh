import { existsSync, mkdirSync, readFileSync, readdirSync, writeFileSync } from "node:fs";
import { basename, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import {
    canonicalJson,
    evidenceVersion,
    frozenImageReferences,
    loadFrozenRelease,
    verifyEvidenceFiles,
    verifyFrozenReleaseFile,
} from "./contract.mjs";
import { runChecked } from "./process.mjs";

function common(lock, repository) {
    return ["--project", lock.project, "--location", lock.region, "--repository", repository, "--quiet"];
}

export function downloadGeneric({ repository, packageName, version, destination, names = [], lock = loadFrozenRelease() }) {
    mkdirSync(destination, { recursive: true });
    for (const name of names.length ? names : [null]) {
        const args = ["artifacts", "generic", "download", ...common(lock, repository), "--package", packageName, "--version", version, "--destination", destination];
        if (name) args.push("--name", name);
        runChecked("gcloud", args);
    }
}

export function downloadAndVerifyRelease(destination, lock = loadFrozenRelease()) {
    downloadGeneric({
        repository: lock.releaseRepository,
        packageName: lock.releasePackage,
        version: lock.releaseVersion,
        destination,
        names: ["release-manifest.json", "release-manifest.json.sha256"],
        lock,
    });
    const manifestPath = join(destination, "release-manifest.json");
    const sidecarPath = join(destination, "release-manifest.json.sha256");
    const sidecar = readFileSync(sidecarPath, "utf8").trim();
    if (sidecar !== `${lock.releaseManifestSha256}  release-manifest.json`) throw new Error("Published release-manifest sidecar differs from the frozen lock.");
    return { manifestPath, manifest: verifyFrozenReleaseFile(manifestPath, lock) };
}

export function downloadAndVerifyFrontend(destination, lock = loadFrozenRelease()) {
    downloadGeneric({
        repository: lock.releaseRepository,
        packageName: lock.frontend.package,
        version: lock.frontend.version,
        destination,
        names: [lock.frontend.archive, `${lock.frontend.archive}.sha256`],
        lock,
    });
    const archivePath = join(destination, lock.frontend.archive);
    const sidecar = readFileSync(`${archivePath}.sha256`, "utf8").trim();
    if (sidecar !== `${lock.frontend.archiveSha256}  ${lock.frontend.archive}`) throw new Error("Published frontend sidecar differs from the frozen lock.");
    return archivePath;
}

export function downloadAndVerifyEvidence({ operation, runId, destination, lock = loadFrozenRelease() }) {
    const version = evidenceVersion(operation, runId, lock);
    downloadGeneric({ repository: lock.deploymentEvidence.repository, packageName: lock.deploymentEvidence.package, version, destination, lock });
    const documentPath = join(destination, "deployment-evidence.json");
    const sidecarPath = join(destination, "deployment-evidence.json.sha256");
    const files = readdirSync(destination).sort();
    if (JSON.stringify(files) !== JSON.stringify(["deployment-evidence.json", "deployment-evidence.json.sha256"])) {
        throw new Error("Deployment evidence package contains an unexpected file set.");
    }
    return { version, evidence: verifyEvidenceFiles(documentPath, sidecarPath, { operation, runId, lock }), documentPath, sidecarPath };
}

function evidenceLookup(operation, runId, lock = loadFrozenRelease()) {
    return {
        project: lock.project,
        location: lock.region,
        repository: lock.deploymentEvidence.repository,
        packageName: lock.deploymentEvidence.package,
        version: evidenceVersion(operation, runId, lock),
    };
}

export function assertEvidenceLookupAbsent(result, actualLookup, expectedLookup) {
    if (canonicalJson(actualLookup) !== canonicalJson(expectedLookup)) {
        throw new Error("Deployment evidence lookup did not use the exact frozen repository, package, and version.");
    }
    if (result.status === 0) throw new Error(`Immutable deployment evidence version already exists: ${expectedLookup.version}`);
    const diagnostic = `${result.stderr ?? ""}\n${result.stdout ?? ""}`.trim();
    if (/PERMISSION_DENIED|UNAUTHENTICATED|UNAVAILABLE|DEADLINE_EXCEEDED|connection|network|timeout/i.test(diagnostic)) {
        throw new Error("Deployment evidence collision preflight failed without proving absence.");
    }
    if (!/\bNOT_FOUND\b/.test(diagnostic) || !/Requested entity was not found/i.test(diagnostic)) {
        throw new Error("Deployment evidence collision preflight did not return a canonical trustworthy NOT_FOUND result.");
    }
    return { version: expectedLookup.version, absent: true };
}

export function assertEvidenceVersionAbsent({ operation, runId, lock = loadFrozenRelease(), runner = runChecked }) {
    const lookup = evidenceLookup(operation, runId, lock);
    const result = runner("gcloud", [
        "artifacts", "versions", "describe", lookup.version,
        ...common(lock, lookup.repository), "--package", lookup.packageName,
    ], { capture: true, allowFailure: true });
    return assertEvidenceLookupAbsent(result, lookup, evidenceLookup(operation, runId, lock));
}

export function assertExactOciLookup(result, expectedReference) {
    if (result.status !== 0) throw new Error(`Frozen OCI artifact lookup failed: ${expectedReference}`);
    let payload;
    try { payload = JSON.parse(result.stdout); } catch { throw new Error("Frozen OCI artifact lookup returned malformed JSON."); }
    const actual = payload.image_summary?.fully_qualified_digest ?? payload.imageSummary?.fullyQualifiedDigest;
    if (actual !== expectedReference) throw new Error("Frozen OCI artifact lookup returned a different digest identity.");
    return expectedReference;
}

export function preflightStageArtifacts({ manifestPath, runId, lock = loadFrozenRelease(), runner = runChecked }) {
    const manifest = verifyFrozenReleaseFile(manifestPath, lock);
    const images = frozenImageReferences(manifest, lock);
    for (const reference of Object.values(images)) {
        const result = runner("gcloud", ["artifacts", "docker", "images", "describe", reference, "--format=json", "--quiet"], { capture: true, allowFailure: true });
        assertExactOciLookup(result, reference);
    }
    const evidence = assertEvidenceVersionAbsent({ operation: "stage", runId, lock, runner });
    return { manifestSha256: lock.releaseManifestSha256, images, evidence };
}

export function publishEvidence({ operation, runId, directory, lock = loadFrozenRelease() }) {
    const version = evidenceVersion(operation, runId, lock);
    const files = readdirSync(directory).sort();
    if (JSON.stringify(files) !== JSON.stringify(["deployment-evidence.json", "deployment-evidence.json.sha256"])) {
        throw new Error("Refusing to publish an unexpected deployment evidence file set.");
    }
    assertEvidenceVersionAbsent({ operation, runId, lock });
    runChecked("gcloud", [
        "artifacts", "generic", "upload", ...common(lock, lock.deploymentEvidence.repository),
        "--package", lock.deploymentEvidence.package, "--version", version, "--source-directory", directory,
    ]);
    return version;
}

function option(name) {
    const index = process.argv.indexOf(name);
    return index >= 0 ? process.argv[index + 1] : undefined;
}

function main() {
    const command = process.argv[2];
    const destination = resolve(option("--destination") ?? ".");
    if (command === "download-release") {
        const result = downloadAndVerifyRelease(destination);
        process.stdout.write(`${result.manifestPath}\n`);
    } else if (command === "download-frontend") {
        process.stdout.write(`${downloadAndVerifyFrontend(destination)}\n`);
    } else if (command === "download-evidence") {
        const result = downloadAndVerifyEvidence({ operation: option("--operation"), runId: option("--run-id"), destination });
        process.stdout.write(`${JSON.stringify({ version: result.version, documentPath: result.documentPath, sidecarPath: result.sidecarPath })}\n`);
    } else if (command === "preflight-stage") {
        const result = preflightStageArtifacts({ manifestPath: resolve(option("--manifest")), runId: option("--run-id") });
        writeFileSync(resolve(option("--output")), canonicalJson(result), { flag: "wx" });
    } else if (command === "publish-evidence") {
        process.stdout.write(`${publishEvidence({ operation: option("--operation"), runId: option("--run-id"), directory: resolve(option("--directory")) })}\n`);
    } else {
        throw new Error(`Unknown Artifact Registry deployment command: ${command}`);
    }
}

if (resolve(process.argv[1] ?? "") === resolve(fileURLToPath(import.meta.url))) main();
