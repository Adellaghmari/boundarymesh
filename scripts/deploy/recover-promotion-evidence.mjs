import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { assertEvidenceVersionAbsent, publishEvidence } from "./artifacts.mjs";
import { evidenceVersion, loadFrozenRelease, writeDeploymentEvidence } from "./contract.mjs";
import {
    assertPromotionRecoveryPreconditions,
    buildRecoveredPromotionEvidence,
} from "./evidence.mjs";

function readJson(path) {
    return JSON.parse(readFileSync(path, "utf8"));
}

function option(name) {
    const index = process.argv.indexOf(name);
    return index >= 0 ? process.argv[index + 1] : undefined;
}

export function recoverPromotionEvidence({
    stageEvidence,
    runtimeSnapshot,
    hostingSnapshot,
    proof,
    outputDir,
    publish = false,
    lock = loadFrozenRelease(),
    collisionRunner,
    publisher,
}) {
    assertPromotionRecoveryPreconditions(proof);
    if (String(proof.runId) !== String(proof.originalPromotionRunId ?? proof.runId)) {
        throw new Error("Promotion recovery must bind the original promotion run as evidence identity.");
    }
    const evidence = buildRecoveredPromotionEvidence({
        stageEvidence,
        runtimeSnapshot: { ...runtimeSnapshot, serviceAccounts: runtimeSnapshot.serviceAccounts ?? proof.runtimeServiceAccounts },
        hostingSnapshot,
        promotionWorkflow: {
            runId: String(proof.runId),
            runAttempt: String(proof.attempt),
            sha: proof.workflowSha,
            ref: "refs/heads/main",
        },
        references: { stageRunId: String(proof.stageRunId) },
        startedAt: proof.startedAt,
        completedAt: proof.completedAt,
        lock,
    });
    const version = evidenceVersion("promote", proof.runId, lock);
    if (version !== `${lock.sourceSha}-promote-run-${proof.runId}`) {
        throw new Error("Recovered promotion evidence identity does not use the original promote-run pattern.");
    }
    const paths = writeDeploymentEvidence(evidence, outputDir, { operation: "promote", runId: proof.runId, lock });
    if (publish) {
        assertEvidenceVersionAbsent({ operation: "promote", runId: proof.runId, lock, runner: collisionRunner });
        const published = publisher
            ? publisher({ operation: "promote", runId: proof.runId, directory: outputDir, lock })
            : publishEvidence({ operation: "promote", runId: proof.runId, directory: outputDir, lock });
        return { evidence, paths, version, published };
    }
    return { evidence, paths, version, published: null };
}

function main() {
    const command = process.argv[2];
    if (command !== "recover") throw new Error("Evidence recovery accepts only the recover command.");
    if (process.argv.includes("--promote") || process.argv.includes("--rollback") || process.argv.includes("--stage")) {
        throw new Error("Evidence recovery cannot invoke a deployment mutation command.");
    }
    const result = recoverPromotionEvidence({
        stageEvidence: readJson(resolve(option("--stage-evidence"))),
        runtimeSnapshot: readJson(resolve(option("--runtime-snapshot"))),
        hostingSnapshot: readJson(resolve(option("--hosting-snapshot"))),
        proof: readJson(resolve(option("--proof"))),
        outputDir: resolve(option("--output-dir")),
        publish: process.argv.includes("--publish"),
    });
    process.stdout.write(`${JSON.stringify({ version: result.version, published: Boolean(result.published) })}\n`);
}

if (resolve(process.argv[1] ?? "") === resolve(fileURLToPath(import.meta.url))) main();
