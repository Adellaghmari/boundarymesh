import { readFileSync, writeFileSync } from "node:fs";
import { resolve } from "node:path";
import { fileURLToPath } from "node:url";
import {
    canonicalJson,
    loadFrozenRelease,
    validateDeploymentEvidence,
    verifyEvidenceFiles,
    writeDeploymentEvidence,
} from "./contract.mjs";

function readJson(path) {
    return JSON.parse(readFileSync(path, "utf8"));
}

function exactTransition(actual, expected, label) {
    if (canonicalJson(actual) !== canonicalJson(expected)) throw new Error(`${label} does not match the referenced prior evidence.`);
}

function assertFullAllocation(state, field, label) {
    const entries = state[field];
    if (!Array.isArray(entries) || entries.length !== 1 || entries[0].percent !== 100 || entries[0].revision !== state.revision) {
        throw new Error(`${label} must assign exactly 100 percent to its recorded revision.`);
    }
}

function matchesState(actual, expected) {
    return canonicalJson(actual) === canonicalJson(expected);
}

function verifyPriorTransition(operation, status, runtime, hosting, references, priorEvidence) {
    if (operation === "stage") return;
    if (!priorEvidence) throw new Error(`${operation} requires validated prior deployment evidence.`);
    if (operation === "promote") {
        if (priorEvidence.operation !== "stage" || references.stageRunId !== priorEvidence.workflow.runId || references.promotionRunId) throw new Error("Promotion does not reference the exact stage evidence run.");
        exactTransition(runtime.before, priorEvidence.runtime.before, "Promotion runtime baseline");
        exactTransition(hosting.before, priorEvidence.hosting.before, "Promotion Hosting baseline");
        for (const subject of ["api", "web", "worker"]) {
            const candidate = priorEvidence.runtime.result.candidates[subject];
            const baseline = priorEvidence.runtime.before[subject];
            const result = runtime.result[subject];
            const isCandidate = result?.revision === candidate.revision && result?.image === candidate.image;
            const isBaseline = matchesState(result, baseline);
            if (status === "PROMOTED" ? !isCandidate : (!isCandidate && !isBaseline)) throw new Error(`Promotion ${subject} result differs from the staged candidate/baseline set.`);
            assertFullAllocation(result, subject === "worker" ? "split" : "traffic", `Promotion ${subject}`);
        }
        const hostingIsCandidate = hosting.result?.site === priorEvidence.hosting.before.site && hosting.result?.version === priorEvidence.hosting.result.version;
        const hostingIsBaseline = matchesState(hosting.result, priorEvidence.hosting.before);
        if (status === "PROMOTED" ? !hostingIsCandidate : (!hostingIsCandidate && !hostingIsBaseline)) throw new Error("Promotion Hosting result differs from the staged version/baseline set.");
        return;
    }
    if (priorEvidence.operation !== "promote" || references.stageRunId !== priorEvidence.references.stageRunId || references.promotionRunId !== priorEvidence.workflow.runId) throw new Error("Rollback does not reference the exact promotion evidence run.");
    exactTransition(runtime.before, priorEvidence.runtime.result, "Rollback runtime baseline");
    exactTransition(hosting.before, priorEvidence.hosting.result, "Rollback Hosting baseline");
    exactTransition(runtime.result, priorEvidence.runtime.before, "Rollback runtime result");
    if (hosting.result?.site !== priorEvidence.hosting.before.site || hosting.result?.version !== priorEvidence.hosting.before.version) {
        throw new Error("Rollback Hosting result does not restore the retained site/version identity.");
    }
}

export function buildEvidence({ operation, outcome = "success", runtime, hosting, workflow, references, priorEvidence = null, startedAt, completedAt = new Date().toISOString(), lock = loadFrozenRelease() }) {
    if (!["success", "failed"].includes(outcome) || (operation !== "promote" && outcome !== "success")) throw new Error("Invalid deployment operation outcome.");
    const status = operation === "promote" && outcome === "failed" ? "PROMOTION_FAILED" : { stage: "STAGED", promote: "PROMOTED", rollback: "ROLLED_BACK" }[operation];
    if (!status) throw new Error("Unknown deployment evidence operation.");
    if (runtime.operation && runtime.operation !== operation) throw new Error("Runtime result operation mismatch.");
    if (hosting.operation && hosting.operation !== operation) throw new Error("Hosting result operation mismatch.");
    verifyPriorTransition(operation, status, runtime, hosting, references, priorEvidence);
    let rollback;
    if (operation === "stage") {
        rollback = {
            apiRevision: runtime.before.api.revision,
            webRevision: runtime.before.web.revision,
            workerRevision: runtime.before.worker.revision,
            firebaseVersion: hosting.before.version,
        };
    } else {
        rollback = structuredClone(priorEvidence.rollback);
    }
    const evidence = {
        schemaVersion: "boundarymesh.deployment/v1",
        operation,
        status,
        release: { sourceSha: lock.sourceSha, manifestSha256: lock.releaseManifestSha256, publicationRunId: lock.publicationRunId },
        workflow: {
            runId: String(workflow.runId),
            runAttempt: String(workflow.runAttempt),
            sha: workflow.sha,
            ref: workflow.ref,
            path: ".github/workflows/deploy-release.yml",
        },
        environments: { runtime: "boundarymesh-deploy", hosting: "boundarymesh-hosting-deploy" },
        references: { stageRunId: references.stageRunId ?? null, promotionRunId: references.promotionRunId ?? null },
        runtime: { operation, before: runtime.before, result: runtime.result ?? { candidates: runtime.candidates }, checks: runtime.checks },
        hosting: { operation, before: hosting.before, result: hosting.result, checks: hosting.checks },
        checks: {
            frozenReleaseVerified: true,
            splitAuthorityPreserved: true,
            noApplicationBuild: true,
            noAutomaticRollback: true,
            operationSucceeded: outcome === "success",
        },
        timestamps: { startedAt, completedAt },
        rollback,
    };
    return validateDeploymentEvidence(evidence, { operation, runId: workflow.runId, lock });
}

export function encodeFile(path) {
    return readFileSync(path).toString("base64");
}

export function decodeEvidence(value) {
    const parsed = JSON.parse(Buffer.from(value, "base64").toString("utf8"));
    return validateDeploymentEvidence(parsed);
}

export function decodeJson(value) {
    return JSON.parse(Buffer.from(value, "base64").toString("utf8"));
}

function option(name) {
    const index = process.argv.indexOf(name);
    return index >= 0 ? process.argv[index + 1] : undefined;
}

function main() {
    const command = process.argv[2];
    if (command === "build") {
        const operation = option("--operation");
        const prior = option("--prior-evidence") ? readJson(option("--prior-evidence")) : null;
        const evidence = buildEvidence({
            operation,
            outcome: option("--outcome") ?? "success",
            runtime: readJson(option("--runtime")),
            hosting: readJson(option("--hosting")),
            priorEvidence: prior,
            workflow: { runId: option("--run-id"), runAttempt: option("--run-attempt"), sha: option("--sha"), ref: option("--ref") },
            references: { stageRunId: option("--stage-run-id") || null, promotionRunId: option("--promotion-run-id") || null },
            startedAt: option("--started-at"),
        });
        const paths = writeDeploymentEvidence(evidence, resolve(option("--output-dir")), { operation, runId: option("--run-id") });
        process.stdout.write(`${JSON.stringify(paths)}\n`);
    } else if (command === "verify") {
        verifyEvidenceFiles(resolve(option("--document")), resolve(option("--sidecar")), { operation: option("--operation"), runId: option("--run-id") });
        process.stdout.write("deployment evidence verified\n");
    } else if (command === "encode") {
        process.stdout.write(`${encodeFile(resolve(option("--file")))}\n`);
    } else if (command === "decode") {
        process.stdout.write(canonicalJson(decodeEvidence(option("--value"))));
    } else if (command === "decode-json") {
        process.stdout.write(canonicalJson(decodeJson(option("--value"))));
    } else if (command === "observe-promotion") {
        const prior = readJson(option("--prior-evidence"));
        validateDeploymentEvidence(prior, { operation: "stage" });
        const snapshot = readJson(option("--runtime-snapshot"));
        const runtimeResult = option("--runtime-result");
        const hostingResult = option("--hosting-result");
        const runtimeValue = option("--runtime-value");
        const hostingValue = option("--hosting-value");
        let runtime = { before: prior.runtime.before, result: snapshot.state, checks: { promotionCompleted: false, observedAfterJobs: true } };
        if (runtimeResult === "success" && runtimeValue) {
            runtime = decodeJson(runtimeValue);
            exactTransition(runtime.result, snapshot.state, "Post-promotion runtime observation");
            runtime.checks = { ...runtime.checks, promotionCompleted: true, observedAfterJobs: true };
        }
        const hosting = hostingValue
            ? decodeJson(hostingValue)
            : { before: prior.hosting.before, result: prior.hosting.before, checks: { promotionCompleted: false, notAttempted: true } };
        if (hostingResult === "success") hosting.checks = { ...hosting.checks, promotionCompleted: true };
        writeFileSync(resolve(option("--runtime-output")), canonicalJson(runtime), { flag: "wx" });
        writeFileSync(resolve(option("--hosting-output")), canonicalJson(hosting), { flag: "wx" });
        process.stdout.write(runtimeResult === "success" && hostingResult === "success" ? "success" : "failed");
    } else if (command === "assert-reference") {
        const evidence = readJson(option("--document"));
        validateDeploymentEvidence(evidence, { operation: option("--operation"), runId: option("--run-id") });
        if (option("--stage-run-id") && evidence.references.stageRunId !== option("--stage-run-id")) throw new Error("Deployment evidence stage_run_id does not match the explicit input.");
        process.stdout.write("deployment evidence reference verified\n");
    } else {
        throw new Error(`Unknown deployment evidence command: ${command}`);
    }
}

if (resolve(process.argv[1] ?? "") === resolve(fileURLToPath(import.meta.url))) main();
