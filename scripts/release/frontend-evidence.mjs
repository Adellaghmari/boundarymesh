import { createHash } from "node:crypto";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { basename, resolve } from "node:path";
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";

const FULL_GIT_SHA = /^[0-9a-f]{40}$/;
const SHA256 = /^[0-9a-f]{64}$/;
const FRONTEND_ARCHIVE_NAME = "frontend-dist.tar.gz";

function canonicalize(value) {
    if (Array.isArray(value)) return value.map(canonicalize);
    if (value && typeof value === "object") {
        return Object.fromEntries(Object.keys(value).sort().map((key) => [key, canonicalize(value[key])]));
    }
    return value;
}

function sha256File(file) {
    return createHash("sha256").update(readFileSync(resolve(file))).digest("hex");
}

function writeImmutableJson(outputFile, value) {
    const output = resolve(outputFile);
    if (existsSync(output)) throw new Error(`Refusing to overwrite immutable evidence: ${output}`);
    const serialized = `${JSON.stringify(canonicalize(value), null, 2)}\n`;
    writeFileSync(output, serialized, { flag: "wx" });
    return createHash("sha256").update(serialized).digest("hex");
}

function requireString(value, name, pattern) {
    if (typeof value !== "string" || value.length === 0 || (pattern && !pattern.test(value))) {
        throw new Error(`Invalid frontend evidence ${name}.`);
    }
}

function requireTimestamp(value) {
    requireString(value, "timestamp");
    if (Number.isNaN(Date.parse(value))) throw new Error("Invalid frontend evidence timestamp.");
}

function assertNoSigningClaim(value) {
    if (value && typeof value === "object") {
        for (const [key, nested] of Object.entries(value)) {
            if (key.toLowerCase().includes("signature")) {
                throw new Error("Frontend generated evidence must not contain a signing claim.");
            }
            assertNoSigningClaim(nested);
        }
    }
}

export function normalizeCycloneDxSbom(sbom) {
    if (!sbom || typeof sbom !== "object" || Array.isArray(sbom) || sbom.bomFormat !== "CycloneDX" || sbom.specVersion !== "1.5") {
        throw new Error("npm did not produce a CycloneDX 1.5 SBOM.");
    }
    if (sbom.metadata?.component?.type !== "application" || sbom.metadata?.component?.name !== "frontend") {
        throw new Error("Frontend SBOM does not identify the frontend application root.");
    }
    const normalized = structuredClone(sbom);
    delete normalized.serialNumber;
    if (normalized.metadata) delete normalized.metadata.timestamp;
    assertNoSigningClaim(normalized);
    return normalized;
}

export function generateFrontendSbom({ frontendDirectory, outputFile, npmCommand = process.platform === "win32" ? "npm.cmd" : "npm" }) {
    const result = spawnSync(npmCommand, ["sbom", "--sbom-format", "cyclonedx", "--package-lock-only", "--sbom-type", "application"], {
        cwd: resolve(frontendDirectory),
        encoding: "utf8",
        // npm.cmd needs cmd.exe dispatch on Windows; Linux runners execute npm directly.
        shell: process.platform === "win32",
    });
    if (result.error || result.status !== 0) {
        throw new Error(`Frontend SBOM generation failed: ${result.error?.message ?? result.stderr?.trim() ?? "npm sbom exited unsuccessfully"}`);
    }
    let parsed;
    try {
        parsed = JSON.parse(result.stdout);
    } catch {
        throw new Error("Frontend SBOM generation did not return JSON.");
    }
    const sbom = normalizeCycloneDxSbom(parsed);
    return { sha256: writeImmutableJson(outputFile, sbom), sbom };
}

export function generateFrontendProvenance({
    archiveFile, outputFile, sourceCommit, sourceRef, workflowPath, workflowRef,
    runId, runAttempt, environment, timestamp, materialFiles, nodeVersion, npmVersion,
}) {
    requireString(sourceCommit, "sourceCommit", FULL_GIT_SHA);
    requireString(sourceRef, "sourceRef", /^refs\/heads\/main$/);
    requireString(workflowPath, "workflowPath", /^\.github\/workflows\/artifact-publish\.yml$/);
    requireString(workflowRef, "workflowRef", /^Adellaghmari\/boundarymesh\/\.github\/workflows\/artifact-publish\.yml@refs\/heads\/main$/);
    requireString(runId, "runId", /^\d+$/);
    requireString(runAttempt, "runAttempt", /^\d+$/);
    requireString(environment, "environment", /^boundarymesh-artifact-publish$/);
    requireTimestamp(timestamp);
    requireString(nodeVersion, "nodeVersion");
    requireString(npmVersion, "npmVersion");
    if (!Array.isArray(materialFiles) || materialFiles.length === 0) throw new Error("Frontend provenance requires source materials.");
    const archive = resolve(archiveFile);
    if (basename(archive) !== FRONTEND_ARCHIVE_NAME || !existsSync(archive)) {
        throw new Error("Frontend provenance requires the produced frontend-dist.tar.gz archive.");
    }
    const subjectSha256 = sha256File(archive);
    const materials = materialFiles.map(({ uri, file }) => {
        requireString(uri, "material URI");
        if (!existsSync(resolve(file))) throw new Error(`Frontend provenance material does not exist: ${file}`);
        return { uri, digest: { sha256: sha256File(file) } };
    });
    materials.unshift({ uri: `git+https://github.com/Adellaghmari/boundarymesh@${sourceCommit}`, digest: { gitCommit: sourceCommit } });
    const provenance = {
        _type: "https://in-toto.io/Statement/v1",
        subject: [{ name: FRONTEND_ARCHIVE_NAME, digest: { sha256: subjectSha256 } }],
        predicateType: "https://slsa.dev/provenance/v1",
        predicate: {
            buildDefinition: {
                buildType: "https://github.com/Adellaghmari/boundarymesh/.github/workflows/artifact-publish.yml#frontend-generated-evidence/v1",
                externalParameters: { repository: "Adellaghmari/boundarymesh", sourceCommit, sourceRef, workflowPath, workflowRef, environment },
                internalParameters: {
                    frontendBuildCommand: "npm run build --prefix frontend",
                    archiveCommand: "node scripts/release/build-frontend-archive.mjs --input frontend/dist --output release/frontend-dist.tar.gz --source-date-epoch <git-commit-epoch>",
                },
                resolvedDependencies: materials,
            },
            runDetails: {
                builder: { id: `${workflowRef}#generated-evidence`, version: { node: nodeVersion, npm: npmVersion } },
                metadata: { invocationId: `${runId}/${runAttempt}`, startedOn: timestamp, finishedOn: timestamp },
                byproducts: [],
            },
        },
    };
    assertNoSigningClaim(provenance);
    return { sha256: writeImmutableJson(outputFile, provenance), provenance, subjectSha256 };
}

function option(name) {
    const index = process.argv.indexOf(name);
    return index < 0 ? undefined : process.argv[index + 1];
}

if (resolve(process.argv[1] ?? "") === resolve(fileURLToPath(import.meta.url))) {
    const command = process.argv[2];
    if (command === "sbom") {
        const frontendDirectory = option("--frontend");
        const outputFile = option("--output");
        if (!frontendDirectory || !outputFile) throw new Error("Usage: frontend-evidence.mjs sbom --frontend <directory> --output <file>");
        const result = generateFrontendSbom({ frontendDirectory, outputFile });
        process.stdout.write(`frontend sbom sha256:${result.sha256}\n`);
    } else if (command === "provenance") {
        const archiveFile = option("--archive");
        const outputFile = option("--output");
        const materials = [
            ["file:frontend/package-lock.json", option("--lockfile")],
            ["file:frontend/package.json", option("--package-json")],
            ["file:scripts/release/build-frontend-archive.mjs", option("--archive-tool")],
        ].map(([uri, file]) => ({ uri, file }));
        const required = [archiveFile, outputFile, option("--source-commit"), option("--source-ref"), option("--workflow-path"), option("--workflow-ref"), option("--run-id"), option("--run-attempt"), option("--environment"), option("--timestamp"), option("--node-version"), option("--npm-version")];
        if (required.some((value) => !value) || materials.some(({ file }) => !file)) {
            throw new Error("Usage: frontend-evidence.mjs provenance --archive <frontend-dist.tar.gz> --output <file> plus source, workflow, run, tool, and material inputs.");
        }
        const result = generateFrontendProvenance({
            archiveFile, outputFile, sourceCommit: option("--source-commit"), sourceRef: option("--source-ref"), workflowPath: option("--workflow-path"), workflowRef: option("--workflow-ref"), runId: option("--run-id"), runAttempt: option("--run-attempt"), environment: option("--environment"), timestamp: option("--timestamp"), materialFiles: materials, nodeVersion: option("--node-version"), npmVersion: option("--npm-version"),
        });
        process.stdout.write(`frontend provenance sha256:${result.sha256}\n`);
    } else {
        throw new Error("Usage: frontend-evidence.mjs <sbom|provenance> ...");
    }
}
