import { existsSync, mkdirSync, readFileSync, readdirSync } from "node:fs";
import { basename, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { evidenceVersion, loadFrozenRelease, verifyEvidenceFiles, verifyFrozenReleaseFile } from "./contract.mjs";
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

export function publishEvidence({ operation, runId, directory, lock = loadFrozenRelease() }) {
    const version = evidenceVersion(operation, runId, lock);
    const files = readdirSync(directory).sort();
    if (JSON.stringify(files) !== JSON.stringify(["deployment-evidence.json", "deployment-evidence.json.sha256"])) {
        throw new Error("Refusing to publish an unexpected deployment evidence file set.");
    }
    const probe = runChecked("gcloud", ["artifacts", "versions", "describe", version, ...common(lock, lock.deploymentEvidence.repository), "--package", lock.deploymentEvidence.package], { capture: true, allowFailure: true });
    if (probe.status === 0) throw new Error(`Immutable deployment evidence version already exists: ${version}`);
    if (!/NOT_FOUND|not found/i.test(probe.stderr ?? "")) throw new Error("Deployment evidence collision preflight did not return a trustworthy not-found result.");
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
    } else if (command === "publish-evidence") {
        process.stdout.write(`${publishEvidence({ operation: option("--operation"), runId: option("--run-id"), directory: resolve(option("--directory")) })}\n`);
    } else {
        throw new Error(`Unknown Artifact Registry deployment command: ${command}`);
    }
}

if (resolve(process.argv[1] ?? "") === resolve(fileURLToPath(import.meta.url))) main();
