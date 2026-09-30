import { readFileSync, writeFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import {
    assertEtag,
    assertInvariantUnchanged,
    assertStageBaseline,
    canonicalJson,
    loadFrozenRelease,
    normalizeTraffic,
    runtimeInvariant,
    sha256Bytes,
    verifyFrozenReleaseFile,
} from "./contract.mjs";
import { runChecked } from "./process.mjs";

const API_ROOT = "https://run.googleapis.com/v2";
const SERVICES = Object.freeze({ api: "boundarymesh-api", web: "boundarymesh-web" });
const WORKER = "boundarymesh-worker";
const API_SA = "boundarymesh-api@boundarymesh-dev.iam.gserviceaccount.com";
const WEB_SA = "boundarymesh-web@boundarymesh-dev.iam.gserviceaccount.com";
const WORKER_SA = "boundarymesh-worker@boundarymesh-dev.iam.gserviceaccount.com";
const PUBLIC_MEMBERS = new Set(["allUsers", "allAuthenticatedUsers"]);
const MAX_API_RESPONSE_BYTES = 5 * 1024 * 1024;

function accessToken() {
    return runChecked("gcloud", ["auth", "print-access-token", "--quiet"], { capture: true }).stdout.trim();
}

async function request(path, { method = "GET", query = {}, body, expected = [200], token = accessToken() } = {}) {
    const url = new URL(`${API_ROOT}/${path.replace(/^\//, "")}`);
    for (const [key, value] of Object.entries(query)) url.searchParams.set(key, String(value));
    const response = await fetch(url, {
        method,
        headers: { Authorization: `Bearer ${token}`, ...(body ? { "Content-Type": "application/json" } : {}) },
        body: body ? JSON.stringify(body) : undefined,
        redirect: "error",
        signal: AbortSignal.timeout(60_000),
    });
    const declaredLength = Number(response.headers.get("content-length") ?? 0);
    if (declaredLength > MAX_API_RESPONSE_BYTES) throw new Error("Cloud Run API response exceeded the bounded size limit.");
    const bytes = Buffer.from(await response.arrayBuffer());
    if (bytes.length > MAX_API_RESPONSE_BYTES) throw new Error("Cloud Run API response exceeded the bounded size limit.");
    const text = bytes.toString("utf8");
    const value = text ? JSON.parse(text) : null;
    if (!expected.includes(response.status)) throw new Error(`Cloud Run API ${method} failed with HTTP ${response.status}: ${value?.error?.status ?? "UNKNOWN"}`);
    return value;
}

function resourceName(kind, id, lock = loadFrozenRelease()) {
    return `projects/${lock.project}/locations/${lock.region}/${kind}/${id}`;
}

async function waitOperation(operation) {
    for (let count = 0; count < 120; count += 1) {
        const current = await request(operation.name);
        if (current.done) {
            if (current.error) throw new Error(`Cloud Run operation failed: ${current.error.status ?? "UNKNOWN"}`);
            return current.response;
        }
        await new Promise((done) => setTimeout(done, 5000));
    }
    throw new Error("Cloud Run operation did not finish within ten minutes.");
}

async function getService(id, lock) {
    return request(resourceName("services", id, lock));
}

async function getWorker(lock) {
    return request(resourceName("workerPools", WORKER, lock));
}

async function getIam(resource) {
    return request(`${resource}:getIamPolicy`);
}

function membersFor(policy, role) {
    return new Set((policy.bindings ?? []).filter((binding) => binding.role === role).flatMap((binding) => binding.members ?? []));
}

export function assertIamBoundary({ apiIam, webIam, workerIam }) {
    const apiInvokers = membersFor(apiIam, "roles/run.invoker");
    for (const member of PUBLIC_MEMBERS) if (apiInvokers.has(member)) throw new Error("Private API unexpectedly has a public Invoker member.");
    if (!apiInvokers.has(`serviceAccount:${WEB_SA}`)) throw new Error("Web service account is not an API Invoker.");
    const webInvokers = membersFor(webIam, "roles/run.invoker");
    if (!webInvokers.has("allUsers") || webInvokers.has("allAuthenticatedUsers")) throw new Error("Web transport-edge IAM differs from the approved public boundary.");
    for (const binding of workerIam.bindings ?? []) {
        if ((binding.members ?? []).some((member) => PUBLIC_MEMBERS.has(member))) throw new Error("Worker Pool unexpectedly has a public IAM member.");
    }
}

function assertRuntimeServiceAccounts(api, web, worker) {
    if (api.template?.serviceAccount !== API_SA) throw new Error("API runtime service account changed.");
    if (web.template?.serviceAccount !== WEB_SA) throw new Error("Web runtime service account changed.");
    if (worker.template?.serviceAccount !== WORKER_SA) throw new Error("Worker runtime service account changed.");
}

function productionTraffic(resource) {
    const status = resource.trafficStatuses ?? [];
    const entries = status.length ? status : [{ type: "TRAFFIC_TARGET_ALLOCATION_TYPE_REVISION", revision: resource.latestReadyRevision, percent: 100 }];
    const normalized = entries.map((entry) => ({
        type: "TRAFFIC_TARGET_ALLOCATION_TYPE_REVISION",
        revision: revisionId(entry.revision ?? resource.latestReadyRevision),
        percent: Number(entry.percent ?? 0),
        ...(entry.tag ? { tag: entry.tag } : {}),
    })).filter((entry) => entry.percent > 0);
    if (normalized.reduce((sum, entry) => sum + entry.percent, 0) !== 100) throw new Error("Cloud Run production traffic does not total 100 percent.");
    return normalized;
}

function productionSplits(worker) {
    const status = worker.instanceSplitStatuses ?? worker.instanceSplits ?? [];
    const entries = status.length ? status : [{ revision: worker.latestReadyRevision, percent: 100 }];
    const normalized = entries.map((entry) => ({
        type: "INSTANCE_SPLIT_ALLOCATION_TYPE_REVISION",
        revision: revisionId(entry.revision ?? worker.latestReadyRevision),
        percent: Number(entry.percent ?? 0),
    })).filter((entry) => entry.percent > 0);
    if (normalized.reduce((sum, entry) => sum + entry.percent, 0) !== 100) throw new Error("Worker production instance split does not total 100 percent.");
    return normalized;
}

function revisionId(value) {
    return String(value ?? "").split("/").at(-1);
}

function stageRevision(id, runId) {
    if (!/^[1-9][0-9]*$/.test(runId)) throw new Error("GITHUB_RUN_ID must be numeric.");
    const value = `${id}-c-${runId}`;
    if (value.length > 63) throw new Error("Deterministic candidate revision exceeds the Cloud Run name limit.");
    return value;
}

function stageTag(runId) {
    return `candidate-${runId}`;
}

function containersWithImage(resource, image) {
    if ((resource.template?.containers ?? []).length !== 1) throw new Error("Expected exactly one runtime container.");
    const containers = structuredClone(resource.template.containers);
    containers[0].image = image;
    return containers;
}

async function patchResource(resource, body, updateMask) {
    if (!resource.etag) throw new Error("Live resource has no ETag; refusing mutation.");
    const operation = await request(resource.name, {
        method: "PATCH",
        query: { updateMask, allowMissing: false },
        body: { name: resource.name, etag: resource.etag, ...body },
    });
    return waitOperation(operation);
}

async function waitReady(getter, expectedRevision) {
    for (let count = 0; count < 120; count += 1) {
        const resource = await getter();
        if (!resource.reconciling && resource.terminalCondition?.state === "CONDITION_SUCCEEDED" && revisionId(resource.latestCreatedRevision) === expectedRevision) return resource;
        if (!resource.reconciling && resource.terminalCondition?.state === "CONDITION_FAILED") throw new Error("Candidate revision failed Cloud Run reconciliation.");
        await new Promise((done) => setTimeout(done, 5000));
    }
    throw new Error("Candidate revision did not become Ready within ten minutes.");
}

async function waitAllocation(getter, expectedRevision, kind) {
    for (let count = 0; count < 120; count += 1) {
        const resource = await getter();
        if (!resource.reconciling && resource.terminalCondition?.state === "CONDITION_FAILED") throw new Error("Cloud Run allocation update failed reconciliation.");
        if (!resource.reconciling && resource.terminalCondition?.state === "CONDITION_SUCCEEDED") {
            const entries = kind === "worker" ? (resource.instanceSplitStatuses ?? resource.instanceSplits ?? []) : (resource.trafficStatuses ?? resource.traffic ?? []);
            const expected = entries.find((entry) => revisionId(entry.revision) === expectedRevision);
            if (Number(expected?.percent ?? 0) === 100 && entries.filter((entry) => revisionId(entry.revision) !== expectedRevision).every((entry) => Number(entry.percent ?? 0) === 0)) return resource;
        }
        await new Promise((done) => setTimeout(done, 5000));
    }
    throw new Error("Cloud Run allocation did not converge within ten minutes.");
}

async function getRevision(revisionName) {
    return request(revisionName);
}

function assertRevisionImage(revision, expectedImage) {
    if (revision.containers?.[0]?.imageDigest !== expectedImage.split("@")[1] && revision.containers?.[0]?.image !== expectedImage) {
        throw new Error("Candidate revision does not contain the frozen image digest.");
    }
}

async function health(url, { audience, path }) {
    const headers = {};
    if (audience) {
        const token = runChecked("gcloud", ["auth", "print-identity-token", `--audiences=${audience}`, "--quiet"], { capture: true }).stdout.trim();
        headers.Authorization = `Bearer ${token}`;
    }
    const response = await fetch(new URL(path, url), { headers, redirect: "error", signal: AbortSignal.timeout(30_000) });
    if (!response.ok) throw new Error(`Candidate health check failed with HTTP ${response.status}.`);
    return { path, status: response.status };
}

export function activeProductionRevision(allocation) {
    const active = allocation.filter((entry) => entry.percent > 0);
    if (active.length !== 1 || active[0].percent !== 100 || !active[0].revision) {
        throw new Error("Expected exactly one revision at 100 percent production allocation.");
    }
    return revisionId(active[0].revision);
}

async function productionRevisionState(resource, allocation, field) {
    const productionRevision = activeProductionRevision(allocation);
    const revision = await getRevision(`${resource.name}/revisions/${productionRevision}`);
    const image = revision.containers?.[0]?.image;
    if (!/@sha256:[0-9a-f]{64}$/.test(image ?? "")) throw new Error("Production revision is not pinned to an immutable image digest.");
    return { revision: productionRevision, image, [field]: normalizeTraffic(allocation) };
}

async function productionRuntimeState({ api, web, worker }) {
    const apiTraffic = productionTraffic(api);
    const webTraffic = productionTraffic(web);
    const workerSplit = productionSplits(worker);
    const [apiState, webState, workerState] = await Promise.all([
        productionRevisionState(api, apiTraffic, "traffic"),
        productionRevisionState(web, webTraffic, "traffic"),
        productionRevisionState(worker, workerSplit, "split"),
    ]);
    return { api: apiState, web: webState, worker: workerState };
}

async function readAll(lock = loadFrozenRelease()) {
    const [api, web, worker] = await Promise.all([getService(SERVICES.api, lock), getService(SERVICES.web, lock), getWorker(lock)]);
    const [apiIam, webIam, workerIam] = await Promise.all([getIam(api.name), getIam(web.name), getIam(worker.name)]);
    assertIamBoundary({ apiIam, webIam, workerIam });
    assertRuntimeServiceAccounts(api, web, worker);
    return { api, web, worker, apiIam, webIam, workerIam };
}

async function runtimePreflightSnapshot(all) {
    const baseline = await productionRuntimeState(all);
    const invariants = Object.fromEntries(["api", "web", "worker"].map((subject) => [
        subject,
        sha256Bytes(Buffer.from(canonicalJson(runtimeInvariant(all[subject], all[`${subject}Iam`])))),
    ]));
    return {
        baseline,
        etags: { api: all.api.etag, web: all.web.etag, worker: all.worker.etag },
        invariants,
    };
}

export function assertRuntimePreflightMatches(current, approved) {
    if (!approved || canonicalJson(current) !== canonicalJson(approved)) {
        throw new Error("Runtime baseline or ETag changed after global stage preflight.");
    }
}

export async function preflightRuntime({ lock = loadFrozenRelease() } = {}) {
    return runtimePreflightSnapshot(await readAll(lock));
}

export async function stageRuntime({ manifestPath, runId, preflight, lock = loadFrozenRelease() }) {
    const manifest = verifyFrozenReleaseFile(manifestPath, lock);
    const before = await readAll(lock);
    assertRuntimePreflightMatches(await runtimePreflightSnapshot(before), preflight);
    const beforePublic = await productionRuntimeState(before);
    const candidates = {};
    for (const subject of ["api", "web"]) {
        const id = SERVICES[subject];
        const current = before[subject];
        const revision = stageRevision(id, runId);
        const image = `${manifest.artifacts[subject].repository}/${manifest.artifacts[subject].image}@${manifest.artifacts[subject].digest}`;
        const traffic = [...productionTraffic(current), { type: "TRAFFIC_TARGET_ALLOCATION_TYPE_REVISION", revision, percent: 0, tag: stageTag(runId) }];
        await patchResource(current, { template: { revision, containers: containersWithImage(current, image) }, traffic }, "template.revision,template.containers,traffic");
        const after = await waitReady(() => getService(id, lock), revision);
        const iam = await getIam(after.name);
        assertEtag(current.etag, before[subject].etag);
        assertInvariantUnchanged(runtimeInvariant(current, before[`${subject}Iam`]), runtimeInvariant(after, iam), `${subject} stage`);
        const revisionResource = await getRevision(`${after.name}/revisions/${revision}`);
        assertRevisionImage(revisionResource, image);
        const trafficStatus = (after.trafficStatuses ?? []).find((entry) => revisionId(entry.revision) === revision);
        if (Number(trafficStatus?.percent ?? 0) !== 0 || trafficStatus?.tag !== stageTag(runId)) throw new Error(`${subject} candidate received production traffic.`);
        const check = subject === "api"
            ? await health(trafficStatus.uri, { audience: after.uri, path: "/api/v1/health" })
            : await health(trafficStatus.uri, { path: "/actuator/health" });
        candidates[subject] = { revision, image, traffic: normalizeTraffic(traffic), health: check };
    }
    const currentWorker = before.worker;
    const workerRevision = stageRevision(WORKER, runId);
    const workerImage = `${manifest.artifacts.worker.repository}/${manifest.artifacts.worker.image}@${manifest.artifacts.worker.digest}`;
    const instanceSplits = [...productionSplits(currentWorker), { type: "INSTANCE_SPLIT_ALLOCATION_TYPE_REVISION", revision: workerRevision, percent: 0 }];
    await patchResource(currentWorker, { template: { revision: workerRevision, containers: containersWithImage(currentWorker, workerImage) }, instanceSplits }, "template.revision,template.containers,instanceSplits");
    const afterWorker = await waitReady(() => getWorker(lock), workerRevision);
    const workerIam = await getIam(afterWorker.name);
    assertInvariantUnchanged(runtimeInvariant(currentWorker, before.workerIam), runtimeInvariant(afterWorker, workerIam), "worker stage");
    const workerRevisionResource = await getRevision(`${afterWorker.name}/revisions/${workerRevision}`);
    assertRevisionImage(workerRevisionResource, workerImage);
    const workerStatus = (afterWorker.instanceSplitStatuses ?? []).find((entry) => revisionId(entry.revision) === workerRevision);
    if (Number(workerStatus?.percent ?? 0) !== 0) throw new Error("Worker candidate received production instances.");
    candidates.worker = { revision: workerRevision, image: workerImage, split: normalizeTraffic(instanceSplits), ready: true };
    const final = await readAll(lock);
    assertIamBoundary(final);
    assertStageBaseline({ ...(await productionRuntimeState(final)), hosting: null }, { ...beforePublic, hosting: null });
    return { before: beforePublic, candidates, checks: { apiReady: true, webReady: true, workerReady: true, iamUnchanged: true, productionUnchanged: true } };
}

function expectedBaselineFromStage(evidence) {
    return { api: evidence.runtime.before.api, web: evidence.runtime.before.web, worker: evidence.runtime.before.worker, hosting: evidence.hosting.before };
}

async function promoteTraffic(current, candidateRevision, kind) {
    if (kind === "worker") {
        return patchResource(current, { instanceSplits: [{ type: "INSTANCE_SPLIT_ALLOCATION_TYPE_REVISION", revision: candidateRevision, percent: 100 }] }, "instanceSplits");
    }
    return patchResource(current, { traffic: [{ type: "TRAFFIC_TARGET_ALLOCATION_TYPE_REVISION", revision: candidateRevision, percent: 100 }] }, "traffic");
}

export async function promoteRuntime({ stageEvidence, currentHosting, lock = loadFrozenRelease() }) {
    const live = await readAll(lock);
    const before = await productionRuntimeState(live);
    const current = { ...before, hosting: currentHosting };
    assertStageBaseline(current, expectedBaselineFromStage(stageEvidence));
    const result = {};
    for (const subject of ["api", "web", "worker"]) {
        const resource = subject === "worker" ? live.worker : live[subject];
        const candidate = stageEvidence.runtime.result.candidates[subject];
        await promoteTraffic(resource, candidate.revision, subject);
        if (subject === "worker") {
            const ready = await waitAllocation(() => getWorker(lock), candidate.revision, "worker");
            result.worker = { revision: revisionId(ready.latestReadyRevision), image: candidate.image, split: normalizeTraffic(ready.instanceSplitStatuses ?? ready.instanceSplits) };
        } else {
            const ready = await waitAllocation(() => getService(SERVICES[subject], lock), candidate.revision, "service");
            result[subject] = { revision: revisionId(ready.latestReadyRevision), image: candidate.image, traffic: normalizeTraffic(ready.trafficStatuses ?? ready.traffic) };
            const status = ready.trafficStatuses?.find((entry) => revisionId(entry.revision) === candidate.revision);
            await health(status?.uri ?? ready.uri, { audience: subject === "api" ? ready.uri : undefined, path: subject === "api" ? "/api/v1/health" : "/actuator/health" });
        }
    }
    return { before, result, checks: { staleBaselineRejected: false, orderedPromotion: true, health: true } };
}

export async function rollbackRuntime({ promotionEvidence, lock = loadFrozenRelease() }) {
    const live = await readAll(lock);
    const before = await productionRuntimeState(live);
    assertStageBaseline(
        { ...before, hosting: null },
        { ...promotionEvidence.runtime.result, hosting: null },
    );
    const rollback = promotionEvidence.rollback;
    for (const subject of ["worker", "web", "api"]) {
        const revision = subject === "worker" ? rollback.workerRevision : subject === "web" ? rollback.webRevision : rollback.apiRevision;
        if (before[subject].revision === revision) continue;
        const resource = subject === "worker" ? await getWorker(lock) : await getService(SERVICES[subject], lock);
        await promoteTraffic(resource, revision, subject);
        if (subject === "worker") await waitAllocation(() => getWorker(lock), revision, "worker");
        else await waitAllocation(() => getService(SERVICES[subject], lock), revision, "service");
    }
    const restored = await readAll(lock);
    return { before, result: await productionRuntimeState(restored), checks: { explicitRollbackIdentities: true, orderedRollback: true } };
}

function option(name) {
    const index = process.argv.indexOf(name);
    return index >= 0 ? process.argv[index + 1] : undefined;
}

async function main() {
    const command = process.argv[2];
    const output = resolve(option("--output"));
    let result;
    if (command === "stage") {
        result = await stageRuntime({
            manifestPath: resolve(option("--manifest")),
            runId: option("--run-id"),
            preflight: JSON.parse(readFileSync(resolve(option("--preflight")), "utf8")),
        });
    } else if (command === "preflight") {
        result = await preflightRuntime();
    } else if (command === "promote") {
        result = await promoteRuntime({ stageEvidence: JSON.parse(readFileSync(option("--evidence"), "utf8")), currentHosting: JSON.parse(option("--hosting-baseline")) });
    } else if (command === "rollback") {
        result = await rollbackRuntime({ promotionEvidence: JSON.parse(readFileSync(option("--evidence"), "utf8")) });
    } else if (command === "snapshot") {
        const all = await readAll();
        result = { state: await productionRuntimeState(all), checks: { iam: true, serviceAccounts: true } };
    } else {
        throw new Error(`Unknown runtime deployment command: ${command}`);
    }
    writeFileSync(output, canonicalJson(result), { flag: "wx" });
}

if (resolve(process.argv[1] ?? "") === resolve(fileURLToPath(import.meta.url))) await main();
