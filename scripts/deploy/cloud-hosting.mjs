import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import {
    canonicalJson,
    extractFrontendArchive,
    inspectFrontendArchive,
    loadFrozenRelease,
    materializeFrozenFirebaseConfig,
    sha256File,
    verifyFrozenReleaseFile,
} from "./contract.mjs";
import { runChecked } from "./process.mjs";

const here = dirname(fileURLToPath(import.meta.url));
const FIREBASE_BIN = join(here, "node_modules", "firebase-tools", "lib", "bin", "firebase.js");
const QUOTA_PROJECT = loadFrozenRelease().project;
const MAX_API_RESPONSE_BYTES = 5 * 1024 * 1024;
const MAX_HOSTED_RESPONSE_BYTES = 20 * 1024 * 1024;
const MAX_DIAGNOSTIC_MESSAGE_LENGTH = 2048;
const MAX_DIAGNOSTIC_FIELD_LENGTH = 1024;
const MAX_ERROR_INFO_ENTRIES = 8;
const ERROR_INFO_TYPE = "type.googleapis.com/google.rpc.ErrorInfo";
const SAFE_ERROR_INFO_METADATA = new Set([
    "service",
    "permission",
    "resource",
    "consumer",
    "quota_project",
    "project",
    "site",
]);
const FORBIDDEN_OPERATIONS = Object.freeze([
    "firebase init",
    "hosting:sites:create",
    "hosting:sites:delete",
    "sites.create",
    "sites.delete",
]);

function accessToken() {
    return runChecked("gcloud", ["auth", "print-access-token", "--quiet"], { capture: true }).stdout.trim();
}

function sanitizedString(value, maximumLength = MAX_DIAGNOSTIC_FIELD_LENGTH) {
    if (typeof value !== "string") return undefined;
    const redacted = value
        .replace(/-----BEGIN (?:RSA |EC )?PRIVATE KEY-----[\s\S]*?-----END (?:RSA |EC )?PRIVATE KEY-----/g, "[REDACTED_PRIVATE_KEY]")
        .replace(/[\u0000-\u001f\u007f]+/g, " ")
        .replace(/\bBearer\s+[^\s,;]+/gi, "Bearer [REDACTED]")
        .replace(/\bya29\.[A-Za-z0-9._-]+\b/g, "[REDACTED_TOKEN]")
        .replace(/\b(?:github_pat_|gh[pousr]_)[A-Za-z0-9_]+\b/gi, "[REDACTED_TOKEN]")
        .replace(/\beyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\b/g, "[REDACTED_TOKEN]")
        .replace(/["']?(?:authorization|private_key|client_email|client_secret|access_token|id_token|refresh_token|github_token|cookie|set-cookie)["']?\s*[:=]\s*(?:"[^"]*"|'[^']*'|[^,;}\s]+)/gi, "[REDACTED]");
    return redacted.replace(/\s+/g, " ").trim().slice(0, maximumLength);
}

export function sanitizeFirebaseHostingError({ httpStatus, payload, networkError } = {}) {
    const googleError = payload && typeof payload === "object" && payload.error && typeof payload.error === "object"
        ? payload.error
        : {};
    const message = sanitizedString(
        typeof googleError.message === "string" ? googleError.message : networkError?.message,
        MAX_DIAGNOSTIC_MESSAGE_LENGTH,
    );
    const diagnostic = {
        httpStatus: Number.isInteger(httpStatus) ? httpStatus : null,
        error: {
            ...(Number.isInteger(googleError.code) ? { code: googleError.code } : {}),
            ...(sanitizedString(googleError.status, 128) ? { status: sanitizedString(googleError.status, 128) } : {}),
            ...(message ? { message } : {}),
        },
    };
    const errorInfo = (Array.isArray(googleError.details) ? googleError.details : [])
        .filter((detail) => detail && typeof detail === "object" && detail["@type"] === ERROR_INFO_TYPE)
        .slice(0, MAX_ERROR_INFO_ENTRIES)
        .map((detail) => {
            const metadata = {};
            if (detail.metadata && typeof detail.metadata === "object" && !Array.isArray(detail.metadata)) {
                for (const key of SAFE_ERROR_INFO_METADATA) {
                    const value = sanitizedString(detail.metadata[key]);
                    if (value) metadata[key] = value;
                }
            }
            return {
                ...(sanitizedString(detail.reason, 256) ? { reason: sanitizedString(detail.reason, 256) } : {}),
                ...(sanitizedString(detail.domain, 256) ? { domain: sanitizedString(detail.domain, 256) } : {}),
                ...(Object.keys(metadata).length ? { metadata } : {}),
            };
        })
        .filter((entry) => Object.keys(entry).length);
    if (errorInfo.length) diagnostic.errorInfo = errorInfo;
    return diagnostic;
}

export function firebaseHostingResponseValue(response) {
    if (response?.ok === true) return response.payload;
    const diagnostic = sanitizeFirebaseHostingError({
        httpStatus: response?.status,
        payload: response?.payload,
        networkError: response?.networkError,
    });
    const error = new Error(`Firebase Hosting request failed: ${JSON.stringify(diagnostic)}`);
    error.name = "FirebaseHostingRequestError";
    error.diagnostic = diagnostic;
    throw error;
}

async function hostingResponse(path, { method = "GET", body } = {}) {
    let response;
    try {
        response = await fetch(`https://firebasehosting.googleapis.com/v1beta1/${path}`, {
            method,
            headers: { Authorization: `Bearer ${accessToken()}`, "x-goog-user-project": QUOTA_PROJECT, ...(body !== undefined ? { "Content-Type": "application/json" } : {}) },
            body: body !== undefined ? JSON.stringify(body) : undefined,
            redirect: "error",
            signal: AbortSignal.timeout(60_000),
        });
    } catch (networkError) {
        return { status: null, ok: false, payload: null, networkError };
    }
    const declaredLength = Number(response.headers.get("content-length") ?? 0);
    if (declaredLength > MAX_API_RESPONSE_BYTES) throw new Error("Firebase Hosting API response exceeded the bounded size limit.");
    const bytes = Buffer.from(await response.arrayBuffer());
    if (bytes.length > MAX_API_RESPONSE_BYTES) throw new Error("Firebase Hosting API response exceeded the bounded size limit.");
    let payload = null;
    if (bytes.length) {
        try { payload = JSON.parse(bytes.toString("utf8")); } catch { throw new Error("Firebase Hosting API returned malformed JSON."); }
    }
    return { status: response.status, ok: response.ok, payload };
}

async function hostingRequest(path, options = {}) {
    const response = await hostingResponse(path, options);
    return firebaseHostingResponseValue(response);
}

export function assertHostingCommand(args, lock = loadFrozenRelease()) {
    const rendered = args.join(" ");
    for (const forbidden of FORBIDDEN_OPERATIONS) if (rendered.includes(forbidden)) throw new Error(`Forbidden Firebase operation: ${forbidden}`);
    if (!args.includes("--project") || args[args.indexOf("--project") + 1] !== lock.firebase.project) throw new Error("Firebase project must be the frozen project.");
    if (/sites?:create|sites?:delete|\binit\b/.test(rendered)) throw new Error("Firebase site lifecycle operations are forbidden.");
}

function firebase(args, { cwd, lock = loadFrozenRelease() } = {}) {
    if (!existsSync(FIREBASE_BIN)) throw new Error("Pinned firebase-tools is not installed in scripts/deploy.");
    assertHostingCommand(args, lock);
    const result = runChecked(process.execPath, [FIREBASE_BIN, ...args], {
        cwd,
        capture: true,
        sanitizeFailureOutput: true,
    });
    try { return JSON.parse(result.stdout); } catch { /* Some CLI releases prefix progress output. */ }
    const lines = result.stdout.trim().split(/\r?\n/).filter(Boolean);
    for (let index = lines.length - 1; index >= 0; index -= 1) {
        try { return JSON.parse(lines[index]); } catch { /* Firebase may emit progress before its JSON result. */ }
    }
    throw new Error("Firebase CLI did not emit the required JSON result.");
}

export async function readHostingBaseline(lock = loadFrozenRelease()) {
    const site = await hostingRequest(`projects/${lock.firebase.project}/sites/${lock.firebase.site}`);
    if (site.name !== `projects/${lock.firebase.project}/sites/${lock.firebase.site}`) throw new Error("Firebase Hosting site identity differs from the frozen lock.");
    const list = await hostingRequest(`sites/${lock.firebase.site}/channels/live/releases?pageSize=1`);
    const latest = list.releases?.[0];
    if (!latest?.version?.name) throw new Error("Firebase live channel has no retained version.");
    return { site: lock.firebase.site, release: latest.name, version: latest.version.name, releaseTime: latest.releaseTime };
}

export async function assertHostingBaseline(stageEvidence, lock = loadFrozenRelease()) {
    const current = await readHostingBaseline(lock);
    if (canonicalJson(current) !== canonicalJson(stageEvidence.hosting.before)) throw new Error("Stale Firebase live baseline; refusing promotion.");
    return current;
}

function previewLookup(site, channel) {
    return { site, channel, resource: `sites/${site}/channels/${channel}` };
}

export function assertPreviewChannelLookupAbsent(response, actualLookup, expectedLookup) {
    if (canonicalJson(actualLookup) !== canonicalJson(expectedLookup)) {
        throw new Error("Preview-channel lookup did not use the exact frozen site and derived channel.");
    }
    if (response.status === 200) {
        if (response.payload?.name !== expectedLookup.resource) throw new Error("Preview-channel lookup returned the wrong project/site resource.");
        throw new Error(`Firebase preview channel already exists: ${expectedLookup.channel}`);
    }
    if (response.status !== 404 || response.payload?.error?.code !== 404 || response.payload?.error?.status !== "NOT_FOUND") {
        throw new Error("Preview-channel collision preflight failed without proving exact absence.");
    }
    return { channel: expectedLookup.channel, absent: true };
}

export async function assertPreviewChannelAbsent(runId, { lock = loadFrozenRelease(), lookup = hostingResponse } = {}) {
    if (!/^[1-9][0-9]*$/.test(runId)) throw new Error("GITHUB_RUN_ID must be numeric.");
    const expected = previewLookup(lock.firebase.site, `bm-${runId}`);
    const response = await lookup(expected.resource);
    return assertPreviewChannelLookupAbsent(response, expected, previewLookup(lock.firebase.site, `bm-${runId}`));
}

export async function preflightHosting({ manifestPath, archivePath, workDirectory, runId, lock = loadFrozenRelease() }) {
    verifyFrozenReleaseFile(manifestPath, lock);
    await inspectFrontendArchive(archivePath);
    if (sha256File(archivePath) !== lock.frontend.archiveSha256) throw new Error("Frontend archive SHA-256 differs from the frozen lock.");
    const configPath = join(workDirectory, "firebase.json");
    materializeFrozenFirebaseConfig({ destination: configPath, lock });
    const before = await readHostingBaseline(lock);
    const preview = await assertPreviewChannelAbsent(runId, { lock });
    return {
        before,
        channel: preview.channel,
        manifestSha256: lock.releaseManifestSha256,
        frontendSha256: lock.frontend.archiveSha256,
        firebaseConfigSha256: sha256File(configPath),
        previewAbsent: true,
    };
}

function findPreview(result) {
    const source = result.result ?? result;
    const channel = source.channel ?? source;
    const url = channel.url ?? source.url;
    const version = channel.version?.name ?? channel.version ?? source.version?.name ?? source.version;
    const release = channel.release?.name ?? source.release?.name ?? source.release;
    if (!url || !version) throw new Error("Firebase preview result omitted its URL or immutable version identity.");
    return { url, version, release: release ?? null, channel: channel.id ?? source.channelId ?? null };
}

async function fetchResponse(url) {
    const response = await fetch(url, { redirect: "error", signal: AbortSignal.timeout(30_000) });
    const declaredLength = Number(response.headers.get("content-length") ?? 0);
    if (declaredLength > MAX_HOSTED_RESPONSE_BYTES) throw new Error("Hosted verification response exceeded the bounded size limit.");
    const bytes = Buffer.from(await response.arrayBuffer());
    if (bytes.length > MAX_HOSTED_RESPONSE_BYTES) throw new Error("Hosted verification response exceeded the bounded size limit.");
    const body = bytes.toString("utf8");
    return { status: response.status, headers: Object.fromEntries(response.headers), body };
}

async function fetchChecked(url) {
    const response = await fetchResponse(url);
    if (response.status < 200 || response.status > 299) throw new Error(`Hosted verification failed with HTTP ${response.status}.`);
    return response;
}

export async function verifyHostedVersion(baseUrl) {
    const root = await fetchChecked(new URL("/", baseUrl));
    const deep = await fetchChecked(new URL("/transactions/demo", baseUrl));
    const cache = (response) => response.headers["cache-control"] ?? "";
    if (!cache(root).includes("no-cache") || !cache(deep).includes("no-cache")) throw new Error("Hosted HTML shell is not protected from stale caching.");
    const assetPaths = [...new Set([...root.body.matchAll(/(?:src|href)=["']([^"']+\.(?:js|css))["']/g)].map((match) => match[1]))];
    if (!assetPaths.length) throw new Error("Hosted root contains no hashed JavaScript/CSS assets.");
    const assets = await Promise.all(assetPaths.map(async (path) => ({ path, response: await fetchChecked(new URL(path, baseUrl)) })));
    for (const { response } of assets) {
        if (!cache(response).includes("max-age=31536000") || !cache(response).includes("immutable")) throw new Error("Hosted hashed asset does not have the frozen immutable cache policy.");
    }
    const session = await fetchResponse(new URL("/session/me", baseUrl));
    let sessionCode;
    try { sessionCode = JSON.parse(session.body).error_code; } catch { /* handled by the assertion below */ }
    if (session.status !== 401 || !["AUTHENTICATION_REQUIRED", "SESSION_INVALID", "SESSION_EXPIRED"].includes(sessionCode)) throw new Error("Preview auth bootstrap rewrite did not fail closed with a structured unauthenticated response.");
    const bundle = `${root.body}\n${deep.body}\n${assets.map(({ response }) => response.body).join("\n")}`;
    for (const forbidden of ["localhost", "run.app/api", "/api/v1/dev/", "PRIVATE KEY", "service_account"]) {
        if (bundle.includes(forbidden)) throw new Error(`Hosted frontend leaks a forbidden production string: ${forbidden}`);
    }
    if (!bundle.includes("boundarymesh-dev")) throw new Error("Hosted frontend does not contain the frozen public Firebase project configuration.");
    return {
        rootHealthy: true,
        deepLinkHealthy: true,
        htmlNoCache: true,
        assetsHealthyAndImmutable: true,
        authBootstrapFailClosed: true,
        noPrivateCoreLeak: true,
    };
}

export async function stageHosting({ manifestPath, archivePath, workDirectory, runId, preflight, lock = loadFrozenRelease() }) {
    verifyFrozenReleaseFile(manifestPath, lock);
    if (!/^[1-9][0-9]*$/.test(runId)) throw new Error("GITHUB_RUN_ID must be numeric.");
    const before = await readHostingBaseline(lock);
    const publicDirectory = join(workDirectory, "frontend", "dist");
    await extractFrontendArchive({ archivePath, destination: publicDirectory, lock });
    materializeFrozenFirebaseConfig({ destination: join(workDirectory, "firebase.json"), lock });
    const channelId = `bm-${runId}`;
    if (!preflight || canonicalJson(before) !== canonicalJson(preflight.before) || preflight.channel !== channelId || preflight.previewAbsent !== true) {
        throw new Error("Firebase Hosting baseline or preview-channel proof changed after global stage preflight.");
    }
    const mutationBaseline = await readHostingBaseline(lock);
    if (canonicalJson(mutationBaseline) !== canonicalJson(preflight.before)) {
        throw new Error("Firebase Hosting live baseline changed immediately before preview mutation.");
    }
    await assertPreviewChannelAbsent(runId, { lock });
    const result = firebase(["hosting:channel:deploy", channelId, "--project", lock.firebase.project, "--expires", "7d", "--non-interactive", "--json"], { cwd: workDirectory, lock });
    const preview = findPreview(result);
    if (preview.channel && preview.channel !== channelId) throw new Error("Firebase deployed an unexpected preview channel.");
    const checks = await verifyHostedVersion(preview.url);
    const afterLive = await readHostingBaseline(lock);
    if (canonicalJson(before) !== canonicalJson(afterLive)) throw new Error("Stage unexpectedly changed the Firebase live release.");
    return { before, result: { ...preview, channel: channelId }, checks: { ...checks, liveUnchanged: true, exactSite: true } };
}

export async function promoteHosting({ stageEvidence, lock = loadFrozenRelease() }) {
    const before = await readHostingBaseline(lock);
    if (canonicalJson(before) !== canonicalJson(stageEvidence.hosting.before)) throw new Error("Stale Firebase live baseline; refusing promotion.");
    const version = stageEvidence.hosting.result.version;
    if (!new RegExp(`^sites/${lock.firebase.site}/versions/[A-Za-z0-9_-]+$`).test(version)) throw new Error("Stage evidence contains an invalid Hosting version.");
    try {
        await hostingRequest(`sites/${lock.firebase.site}/releases?versionName=${encodeURIComponent(version)}`, { method: "POST", body: {} });
        const after = await readHostingBaseline(lock);
        if (after.version !== version) throw new Error("Live Hosting release does not reference the exact staged version.");
        const checks = await verifyHostedVersion(`https://${lock.firebase.site}.web.app`);
        return { before, result: after, checks: { ...checks, exactStagedVersion: true } };
    } catch {
        let observed = before;
        try { observed = await readHostingBaseline(lock); } catch { /* Preserve the last trusted state if observation is unavailable. */ }
        const failure = new Error("Hosting promotion failed closed.");
        failure.operationResult = { before, result: observed, checks: { promotionCompleted: false, observedAfterFailure: observed !== before } };
        throw failure;
    }
}

export async function rollbackHosting({ promotionEvidence, lock = loadFrozenRelease() }) {
    const before = await readHostingBaseline(lock);
    if (canonicalJson(before) !== canonicalJson(promotionEvidence.hosting.result)) throw new Error("Stale promoted Hosting baseline; refusing rollback.");
    const version = promotionEvidence.rollback.firebaseVersion;
    if (!new RegExp(`^sites/${lock.firebase.site}/versions/[A-Za-z0-9_-]+$`).test(version)) throw new Error("Promotion evidence contains an invalid rollback Hosting version.");
    if (before.version === version) return { before, result: before, checks: { exactRollbackVersion: true, alreadyRestored: true } };
    await hostingRequest(`sites/${lock.firebase.site}/releases?versionName=${encodeURIComponent(version)}`, { method: "POST", body: {} });
    const after = await readHostingBaseline(lock);
    if (after.version !== version) throw new Error("Hosting rollback did not restore the recorded version.");
    return { before, result: after, checks: { exactRollbackVersion: true, ...(await verifyHostedVersion(`https://${lock.firebase.site}.web.app`)) } };
}

function option(name) {
    const index = process.argv.indexOf(name);
    return index >= 0 ? process.argv[index + 1] : undefined;
}

async function main() {
    const command = process.argv[2];
    const output = resolve(option("--output"));
    let result;
    try {
        if (command === "stage") {
            result = await stageHosting({
                manifestPath: resolve(option("--manifest")),
                archivePath: resolve(option("--archive")),
                workDirectory: resolve(option("--work-dir")),
                runId: option("--run-id"),
                preflight: JSON.parse(readFileSync(resolve(option("--preflight")), "utf8")),
            });
        } else if (command === "preflight") {
            result = await preflightHosting({
                manifestPath: resolve(option("--manifest")),
                archivePath: resolve(option("--archive")),
                workDirectory: resolve(option("--work-dir")),
                runId: option("--run-id"),
            });
        } else if (command === "promote") {
            result = await promoteHosting({ stageEvidence: JSON.parse(readFileSync(option("--evidence"), "utf8")) });
        } else if (command === "rollback") {
            result = await rollbackHosting({ promotionEvidence: JSON.parse(readFileSync(option("--evidence"), "utf8")) });
        } else if (command === "snapshot") {
            result = await readHostingBaseline();
        } else if (command === "assert-baseline") {
            result = await assertHostingBaseline(JSON.parse(readFileSync(option("--evidence"), "utf8")));
        } else {
            throw new Error(`Unknown Hosting deployment command: ${command}`);
        }
    } catch (error) {
        if (error.operationResult) {
            writeFileSync(output, canonicalJson(error.operationResult), { flag: "wx" });
            process.stderr.write("Hosting promotion failed closed; safe observed state was retained for evidence.\n");
            process.exitCode = 1;
            return;
        }
        throw error;
    }
    writeFileSync(output, canonicalJson(result), { flag: "wx" });
}

if (resolve(process.argv[1] ?? "") === resolve(fileURLToPath(import.meta.url))) await main();
