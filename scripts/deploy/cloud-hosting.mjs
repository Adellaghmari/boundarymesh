import { existsSync, readFileSync, readdirSync, writeFileSync } from "node:fs";
import { dirname, join, relative, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import {
    canonicalJson,
    extractFrontendArchive,
    inspectFrontendArchive,
    loadFrozenRelease,
    materializeFrozenFirebaseConfig,
    sha256Bytes,
    sha256File,
    verifyFrozenReleaseFile,
} from "./contract.mjs";
import { runChecked } from "./process.mjs";

const here = dirname(fileURLToPath(import.meta.url));
const FIREBASE_BIN = join(here, "node_modules", "firebase-tools", "lib", "bin", "firebase.js");
const QUOTA_PROJECT = loadFrozenRelease().project;
const MAX_API_RESPONSE_BYTES = 5 * 1024 * 1024;
// Hard ceiling for a Hosting inventory. It is not the expected file list.
// Publication 7352538198f2d3d372130977d311c48011fa85eb contains 77 frozen files
// and 427,340 bytes. Publication 830b34190e0b8ae4bedb2c8690638d21a7c48516 contained
// 77 files and 411,842 bytes. Publication 685db5200b7bb4b8042de35dd2281d8d40149c7f contained
// 77 files and 404,071 bytes. Publication f307c9009f39f60f3fe4c036846ba1c0c3c42a7f contained
// 77 files and 391,439 bytes. Stage 37238811079 failed only because the previous
// ceiling of 16 rejected that count before path or byte comparison. 128 is the
// smallest power of two above 77 that still leaves room for additional per-route
// script and stylesheet chunks. Exact equality with the frozen archive remains authoritative.
// Version.fileCount is never that authority.
// The reviewed public Vue DevTools archive is 115 files and 6,006,191 bytes.
// 115 remains under the 128 file ceiling. The old 512 KiB frozen aggregate and the
// old 2 MiB hosted response budget both reject that archive before Firebase Hosting
// is involved. 6 MiB is the smallest mebibyte ceiling above the reviewed archive.
// Stage verification downloads every frozen file and then re-reads the HTML shell,
// so the hosted budget is that ceiling plus a bounded 512 KiB shell reread.
export const MAX_HOSTED_FILE_COUNT = 128;
const MAX_HOSTED_FILE_BYTES = 5 * 1024 * 1024;
export const MAX_FROZEN_TOTAL_BYTES = 6 * 1024 * 1024;
export const MAX_HOSTED_SHELL_REREAD_BYTES = 512 * 1024;
export const MAX_HOSTED_TOTAL_BYTES = MAX_FROZEN_TOTAL_BYTES + MAX_HOSTED_SHELL_REREAD_BYTES;
const MAX_HOSTED_REQUEST_COUNT = (MAX_HOSTED_FILE_COUNT * 2) + 3;
const MAX_HTML_PROBE_BYTES = 256 * 1024;
const MAX_ASSET_PROBE_BYTES = 1024 * 1024;
const MAX_SESSION_PROBE_BYTES = 16 * 1024;
const MAX_VERSION_FILE_PAGES = 4;
const HOSTED_REQUEST_TIMEOUT_MS = 30_000;
const HOSTED_VERIFICATION_DEADLINE_MS = 120_000;
const FIREBASE_GENERATED_VERSION_PATHS = Object.freeze([
    "/__/firebase/init.js",
    "/__/firebase/init.json",
]);
// Firebase-generated init files are inventory-only. They are not frozen static bytes and are not fetched.
// Version.fileCount is output-only int64, calculated after FINALIZED. It is not a
// same-instant sites.versions.files.list length and must not be equality-checked
// against that list. Hosting FAQ extra metadata vs the local project is bounded
// here to the two reserved init files plus a tiny unlisted slack; unknown paths fail closed.
const MAX_UNLISTED_VERSION_METADATA_FILES = 8;
export const MAX_VERSION_FILE_COUNT = MAX_HOSTED_FILE_COUNT + FIREBASE_GENERATED_VERSION_PATHS.length + MAX_UNLISTED_VERSION_METADATA_FILES;
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

function hostedVerificationError(code, message) {
    const error = new Error(`${code}: ${message}`);
    error.code = code;
    return error;
}

export function createHostedVerificationBudget({
    maximumBytes = MAX_HOSTED_TOTAL_BYTES,
    maximumRequests = MAX_HOSTED_REQUEST_COUNT,
    deadlineMs = HOSTED_VERIFICATION_DEADLINE_MS,
} = {}) {
    if (!Number.isSafeInteger(maximumBytes) || maximumBytes <= 0 || maximumBytes > MAX_HOSTED_TOTAL_BYTES) {
        throw new Error("Hosted verification byte budget is invalid.");
    }
    if (!Number.isSafeInteger(maximumRequests) || maximumRequests <= 0 || maximumRequests > MAX_HOSTED_REQUEST_COUNT) {
        throw new Error("Hosted verification request budget is invalid.");
    }
    if (!Number.isSafeInteger(deadlineMs) || deadlineMs <= 0 || deadlineMs > HOSTED_VERIFICATION_DEADLINE_MS) {
        throw new Error("Hosted verification deadline is invalid.");
    }
    let usedBytes = 0;
    let usedRequests = 0;
    const deadlineAt = Date.now() + deadlineMs;
    return Object.freeze({
        get maximumBytes() { return maximumBytes; },
        get maximumRequests() { return maximumRequests; },
        get usedBytes() { return usedBytes; },
        get usedRequests() { return usedRequests; },
        remainingBytes() { return maximumBytes - usedBytes; },
        beginRequest() {
            if (Date.now() > deadlineAt) {
                throw hostedVerificationError("REQUEST_TIMEOUT", "hosted verification exceeded its overall deadline.");
            }
            if (usedRequests + 1 > maximumRequests) {
                throw hostedVerificationError("REQUEST_COUNT_EXCEEDED", "hosted verification request count exceeded its limit.");
            }
            usedRequests += 1;
        },
        chargeBytes(byteCount) {
            if (!Number.isSafeInteger(byteCount) || byteCount < 0) throw new Error("Hosted verification byte charge is invalid.");
            if (usedBytes + byteCount > maximumBytes) {
                throw hostedVerificationError("TOTAL_BUDGET_EXCEEDED", "hosted verification exceeded its aggregate response-byte budget.");
            }
            usedBytes += byteCount;
        },
    });
}

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

async function readBoundedResponseBody(response, {
    maximumBytes,
    remainingBytes,
    chargeBytes = () => {},
    controller,
    tooLargeError,
} = {}) {
    const contentLength = responseHeader(response, "content-length");
    if (typeof contentLength === "string" && /^[0-9]+$/.test(contentLength.trim())) {
        const declaredLength = Number(contentLength);
        if (Number.isSafeInteger(declaredLength) && declaredLength > maximumBytes) {
            stopResponse(response, null, controller);
            throw tooLargeError();
        }
        if (Number.isSafeInteger(declaredLength) && Number.isSafeInteger(remainingBytes) && declaredLength > remainingBytes) {
            stopResponse(response, null, controller);
            throw hostedVerificationError("TOTAL_BUDGET_EXCEEDED", "hosted verification exceeded its aggregate response-byte budget.");
        }
    }
    const reader = response.body?.getReader?.();
    if (!reader) {
        if (response.body == null) return Buffer.alloc(0);
        throw new Error("Response body is not a readable stream.");
    }
    const chunks = [];
    let receivedBytes = 0;
    while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        if (!(value instanceof Uint8Array)) {
            stopResponse(response, reader, controller);
            throw new Error("Response stream returned a malformed chunk.");
        }
        const chunkBytes = value.byteLength;
        if (receivedBytes + chunkBytes > maximumBytes) {
            stopResponse(response, reader, controller);
            throw tooLargeError();
        }
        try {
            chargeBytes(chunkBytes);
        } catch (error) {
            stopResponse(response, reader, controller);
            throw error;
        }
        receivedBytes += chunkBytes;
        chunks.push(Buffer.from(value));
    }
    return Buffer.concat(chunks, receivedBytes);
}

async function hostingResponse(path, { method = "GET", body } = {}) {
    let response;
    try {
        response = await fetch(`https://firebasehosting.googleapis.com/v1beta1/${path}`, {
            method,
            headers: { Authorization: `Bearer ${accessToken()}`, "x-goog-user-project": QUOTA_PROJECT, ...(body !== undefined ? { "Content-Type": "application/json" } : {}) },
            body: body !== undefined ? JSON.stringify(body) : undefined,
            redirect: "error",
            signal: AbortSignal.timeout(HOSTED_REQUEST_TIMEOUT_MS),
        });
    } catch (networkError) {
        return { status: null, ok: false, payload: null, networkError };
    }
    const bytes = await readBoundedResponseBody(response, {
        maximumBytes: MAX_API_RESPONSE_BYTES,
        tooLargeError: () => new Error("Firebase Hosting API response exceeded the bounded size limit."),
    });
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
    runChecked(process.execPath, [FIREBASE_BIN, ...args], {
        cwd,
        capture: true,
        sanitizeFailureOutput: true,
    });
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

export function assertAuthoritativePreviewResult({ channel, version, runId, site }) {
    const channelId = `bm-${runId}`;
    const channelName = `sites/${site}/channels/${channelId}`;
    if (!channel || channel.name !== channelName) throw new Error("Firebase REST returned the wrong preview channel.");
    let url;
    try { url = new URL(channel.url); } catch { throw new Error("Firebase preview channel omitted a valid URL."); }
    if (url.protocol !== "https:" || !url.hostname.startsWith(`${site}--${channelId}-`) || !url.hostname.endsWith(".web.app")) {
        throw new Error("Firebase preview channel URL does not match the exact site and channel.");
    }
    const releaseName = channel.release?.name;
    const versionName = channel.release?.version?.name;
    if (!new RegExp(`^sites/${site}/channels/${channelId}/releases/[A-Za-z0-9_-]+$`).test(releaseName ?? "")) {
        throw new Error("Firebase preview channel has no exact immutable release.");
    }
    if (!new RegExp(`^sites/${site}/versions/[A-Za-z0-9_-]+$`).test(versionName ?? "")) {
        throw new Error("Firebase preview release has no exact site version.");
    }
    if (!version || version.name !== versionName) throw new Error("Firebase REST returned the wrong released version.");
    if (version.status !== "FINALIZED" || typeof version.finalizeTime !== "string" || !version.finalizeTime) {
        throw new Error("Firebase preview version is not FINALIZED.");
    }
    return { url: channel.url, version: versionName, release: releaseName, channel: channelId };
}

export async function readAuthoritativePreview(runId, {
    lock = loadFrozenRelease(),
    request = hostingRequest,
} = {}) {
    if (!/^[1-9][0-9]*$/.test(runId)) throw new Error("GITHUB_RUN_ID must be numeric.");
    const channelId = `bm-${runId}`;
    const channel = await request(`sites/${lock.firebase.site}/channels/${channelId}`);
    const versionName = channel?.release?.version?.name;
    if (!new RegExp(`^sites/${lock.firebase.site}/versions/[A-Za-z0-9_-]+$`).test(versionName ?? "")) {
        throw new Error("Firebase preview channel has no exact released version.");
    }
    const version = await request(versionName);
    return assertAuthoritativePreviewResult({ channel, version, runId, site: lock.firebase.site });
}

export async function deployPreview(runId, {
    workDirectory,
    lock = loadFrozenRelease(),
    cliDeploy = firebase,
    request = hostingRequest,
} = {}) {
    const channelId = `bm-${runId}`;
    cliDeploy([
        "hosting:channel:deploy",
        channelId,
        "--project", lock.firebase.project,
        "--expires", "7d",
        "--non-interactive",
        "--json",
    ], { cwd: workDirectory, lock });
    return readAuthoritativePreview(runId, { lock, request });
}

function responseHeader(response, name) {
    return response.headers?.get?.(name) ?? response.headers?.[name.toLowerCase()] ?? response.headers?.[name] ?? null;
}

function stopResponse(response, reader, controller) {
    controller?.abort();
    const cancellation = reader?.cancel?.() ?? response?.body?.cancel?.();
    if (cancellation?.catch) void cancellation.catch(() => {});
}

export async function readHostedResponse(url, {
    fetchImpl = fetch,
    maximumBytes = MAX_HOSTED_FILE_BYTES,
    expectedOrigin,
    expectedStatuses = [200],
    budget,
    timeoutMs = HOSTED_REQUEST_TIMEOUT_MS,
} = {}) {
    if (!budget || typeof budget.beginRequest !== "function" || typeof budget.chargeBytes !== "function" || typeof budget.remainingBytes !== "function") {
        throw new Error("Hosted verification requires a shared network budget.");
    }
    if (!Number.isSafeInteger(maximumBytes) || maximumBytes < 0 || maximumBytes > MAX_HOSTED_FILE_BYTES) {
        throw new Error("Hosted verification per-response byte limit is invalid.");
    }
    if (!Number.isSafeInteger(timeoutMs) || timeoutMs <= 0 || timeoutMs > HOSTED_REQUEST_TIMEOUT_MS) {
        throw new Error("Hosted verification request timeout is invalid.");
    }
    const requested = new URL(url);
    if (requested.protocol !== "https:") throw new Error("Hosted verification requires HTTPS.");
    if (expectedOrigin && requested.origin !== expectedOrigin) throw new Error("Hosted verification rejected a cross-host request.");
    const controller = new AbortController();
    let timedOut = false;
    const timeout = setTimeout(() => {
        timedOut = true;
        controller.abort();
    }, timeoutMs);
    let response;
    try {
        budget.beginRequest();
        response = await fetchImpl(requested, {
            redirect: "manual",
            headers: { "Accept-Encoding": "identity" },
            signal: controller.signal,
        });
        const location = responseHeader(response, "location");
        if (response.status >= 300 && response.status <= 399) {
            let target = null;
            try { target = location ? new URL(location, requested) : null; } catch { /* Redirects are always rejected below. */ }
            stopResponse(response, null, controller);
            throw hostedVerificationError(
                "REDIRECT_FORBIDDEN",
                target && target.origin !== requested.origin
                    ? "hosted verification rejected a cross-host redirect."
                    : "hosted verification rejected a redirect.",
            );
        }
        if (response.redirected === true || (response.url && new URL(response.url).origin !== requested.origin)) {
            stopResponse(response, null, controller);
            throw hostedVerificationError("REDIRECT_FORBIDDEN", "hosted verification rejected a cross-host redirect.");
        }
        if (!expectedStatuses.includes(response.status)) {
            stopResponse(response, null, controller);
            throw hostedVerificationError("HTTP_STATUS_INVALID", `hosted verification received HTTP ${response.status}.`);
        }
        const contentEncoding = responseHeader(response, "content-encoding");
        if (contentEncoding && contentEncoding.trim().toLowerCase() !== "identity") {
            stopResponse(response, null, controller);
            throw hostedVerificationError("BYTE_MISMATCH", "hosted verification rejected an encoded response.");
        }
        const bytes = await readBoundedResponseBody(response, {
            maximumBytes,
            remainingBytes: budget.remainingBytes(),
            chargeBytes: (byteCount) => budget.chargeBytes(byteCount),
            controller,
            tooLargeError: () => hostedVerificationError("RESPONSE_TOO_LARGE", "hosted response exceeded its per-response byte limit."),
        });
        return {
            status: response.status,
            headers: response.headers?.entries ? Object.fromEntries(response.headers) : response.headers ?? {},
            body: bytes.toString("utf8"),
            bytes,
        };
    } catch (error) {
        if (error?.code && [
            "RESPONSE_TOO_LARGE",
            "TOTAL_BUDGET_EXCEEDED",
            "REQUEST_COUNT_EXCEEDED",
            "REQUEST_TIMEOUT",
            "REDIRECT_FORBIDDEN",
            "HTTP_STATUS_INVALID",
            "BYTE_MISMATCH",
        ].includes(error.code)) {
            throw error;
        }
        if (timedOut) {
            stopResponse(response, null, controller);
            throw hostedVerificationError("REQUEST_TIMEOUT", "hosted verification request timed out.");
        }
        stopResponse(response, null, controller);
        throw hostedVerificationError("HOSTED_STREAM_READ_FAILED", "hosted verification could not read the response stream.");
    } finally {
        clearTimeout(timeout);
    }
}

export function assertFrozenFileCount(fileCount, ceiling = MAX_HOSTED_FILE_COUNT) {
    if (!Number.isSafeInteger(fileCount) || fileCount < 1 || fileCount > ceiling) {
        throw new Error("Frozen frontend file count exceeded the verification limit.");
    }
}

export function frozenFrontendFiles(publicDirectory) {
    return expectedFrozenFiles(publicDirectory);
}

function expectedFrozenFiles(publicDirectory) {
    const files = [];
    function visit(directory) {
        for (const entry of readdirSync(directory, { withFileTypes: true })) {
            const path = join(directory, entry.name);
            if (entry.isDirectory()) visit(path);
            else if (entry.isFile()) {
                const bytes = readFileSync(path);
                const hostedPath = `/${relative(publicDirectory, path).replaceAll("\\", "/")}`;
                if (bytes.length > MAX_HOSTED_FILE_BYTES) throw new Error("Frozen frontend file exceeded the per-file verification limit.");
                files.push({ path: hostedPath, bytes, sha256: sha256Bytes(bytes) });
            } else {
                throw new Error("Frozen frontend contains a link or special file.");
            }
        }
    }
    visit(publicDirectory);
    files.sort((a, b) => a.path.localeCompare(b.path));
    const totalBytes = files.reduce((sum, file) => sum + file.bytes.length, 0);
    assertFrozenFileCount(files.length);
    if (totalBytes > MAX_FROZEN_TOTAL_BYTES) throw new Error("Frozen frontend exceeded the total verification size limit.");
    if (new Set(files.map((file) => file.path)).size !== files.length) throw new Error("Frozen frontend contains duplicate hosted paths.");
    return files;
}

export function normalizeFrozenHostingConfig(firebaseConfig) {
    const hosting = firebaseConfig?.hosting;
    if (!hosting || typeof hosting !== "object") throw new Error("Frozen Firebase config has no Hosting configuration.");
    const normalized = {
        headers: (hosting.headers ?? []).map((entry) => ({
            glob: entry.source,
            headers: Object.fromEntries((entry.headers ?? []).map((header) => [header.key, header.value])),
        })),
        rewrites: (hosting.rewrites ?? []).map((entry) => ({
            glob: entry.source,
            ...(entry.destination
                ? { path: entry.destination }
                : { run: { serviceId: entry.run?.serviceId, region: entry.run?.region } }),
        })),
    };
    if (Array.isArray(hosting.redirects) && hosting.redirects.length > 0) {
        normalized.redirects = hosting.redirects.map((entry) => ({
            glob: entry.source,
            location: entry.destination,
            statusCode: entry.type,
        }));
    }
    return normalized;
}

export function parseVersionFileCount(fileCount) {
    if (fileCount === undefined || fileCount === null) {
        return { available: false, value: null };
    }
    if (typeof fileCount === "number") {
        if (!Number.isInteger(fileCount) || fileCount < 0 || fileCount > MAX_VERSION_FILE_COUNT) {
            throw new Error("Firebase version fileCount is not a safe bounded integer.");
        }
        return { available: true, value: fileCount };
    }
    if (typeof fileCount !== "string" || !/^(0|[1-9][0-9]{0,3})$/.test(fileCount)) {
        throw new Error("Firebase version fileCount is not a safe integer.");
    }
    const value = Number(fileCount);
    if (!Number.isInteger(value) || value < 0 || value > MAX_VERSION_FILE_COUNT) {
        throw new Error("Firebase version fileCount exceeded the verification limit.");
    }
    return { available: true, value };
}

export function assertHostedVersionInventory({ version, files, expectedFiles }) {
    if (version?.status !== "FINALIZED" || typeof version.finalizeTime !== "string" || !version.finalizeTime) {
        throw new Error("Hosted verification requires the exact FINALIZED version.");
    }
    if (!Array.isArray(files) || !Array.isArray(expectedFiles)) throw new Error("Hosted version file inventory is missing.");
    const maxInspectableFiles = MAX_HOSTED_FILE_COUNT + FIREBASE_GENERATED_VERSION_PATHS.length;
    if (files.length > maxInspectableFiles) {
        throw new Error("Firebase version file count exceeded the verification limit.");
    }
    const actualPaths = files.map((file) => file?.path);
    if (actualPaths.some((path) => typeof path !== "string" || !path.startsWith("/") || path.includes("//") || path.includes("\\"))) {
        throw new Error("Firebase version file inventory contains a malformed path.");
    }
    if (new Set(actualPaths).size !== actualPaths.length) throw new Error("Firebase version file inventory contains duplicate paths.");
    for (const file of files) {
        if (file?.status !== "ACTIVE" || typeof file.hash !== "string" || !file.hash) {
            throw new Error("Firebase version file inventory contains a non-ACTIVE or unhashed file.");
        }
    }
    const expectedFrozenPaths = expectedFiles.map((file) => file.path);
    const firebaseOwned = [];
    const deployOwned = [];
    for (const path of actualPaths) {
        if (FIREBASE_GENERATED_VERSION_PATHS.includes(path)) firebaseOwned.push(path);
        else if (path === "/__" || path.startsWith("/__/")) {
            throw new Error("Firebase version file inventory contains an unexpected reserved path.");
        } else {
            deployOwned.push(path);
        }
    }
    if (canonicalJson([...deployOwned].sort()) !== canonicalJson([...expectedFrozenPaths].sort())) {
        const missingFrozen = expectedFrozenPaths.some((path) => !deployOwned.includes(path));
        throw new Error(missingFrozen
            ? "Firebase version file inventory is missing a frozen frontend path."
            : "Firebase version file inventory contains unexpected user-deployed content.");
    }
    if (canonicalJson([...firebaseOwned].sort()) !== canonicalJson([...FIREBASE_GENERATED_VERSION_PATHS].sort())) {
        throw new Error("Firebase version file inventory is missing an expected Firebase-owned reserved path.");
    }
    const parsedFileCount = parseVersionFileCount(version.fileCount);
    if (parsedFileCount.available && parsedFileCount.value > 0 && parsedFileCount.value < expectedFrozenPaths.length) {
        throw new Error("Firebase version fileCount is below the frozen deploy-owned file count.");
    }
    return true;
}

async function readHostedVersionFiles(versionName, request) {
    const files = [];
    let pageToken = null;
    for (let page = 0; page < MAX_VERSION_FILE_PAGES; page += 1) {
        const suffix = pageToken ? `&pageToken=${encodeURIComponent(pageToken)}` : "";
        const response = await request(`${versionName}/files?pageSize=100${suffix}`);
        if (response?.files !== undefined && !Array.isArray(response.files)) throw new Error("Firebase version file inventory is malformed.");
        files.push(...(response?.files ?? []));
        if (files.length > MAX_HOSTED_FILE_COUNT + FIREBASE_GENERATED_VERSION_PATHS.length) {
            throw new Error("Firebase version file count exceeded the verification limit.");
        }
        pageToken = response?.nextPageToken ?? null;
        if (!pageToken) return files;
    }
    throw new Error("Firebase version file inventory exceeded the pagination limit.");
}

function assertExactPreviewUrl(baseUrl, expectedUrl) {
    let actual;
    let expected;
    try {
        actual = new URL(baseUrl);
        expected = new URL(expectedUrl);
    } catch {
        throw new Error("Hosted verification received an invalid preview URL.");
    }
    if (actual.href !== expected.href || actual.protocol !== "https:" || !actual.hostname.endsWith(".web.app")) {
        throw new Error("Hosted verification URL differs from the exact run-specific preview.");
    }
    return actual;
}

export async function verifyHostedServingBehavior(baseUrl, {
    fetchImpl = fetch,
    budget = createHostedVerificationBudget(),
} = {}) {
    const origin = new URL(baseUrl).origin;
    const root = await readHostedResponse(new URL("/", baseUrl), {
        fetchImpl,
        expectedOrigin: origin,
        maximumBytes: MAX_HTML_PROBE_BYTES,
        budget,
    });
    const deep = await readHostedResponse(new URL("/transactions/demo", baseUrl), {
        fetchImpl,
        expectedOrigin: origin,
        maximumBytes: MAX_HTML_PROBE_BYTES,
        budget,
    });
    const cache = (response) => response.headers["cache-control"] ?? "";
    if (!cache(root).includes("no-cache") || !cache(deep).includes("no-cache")) throw new Error("Hosted HTML shell is not protected from stale caching.");
    const assetPaths = [...new Set([...root.body.matchAll(/(?:src|href)=["']([^"']+\.(?:js|css))["']/g)].map((match) => match[1]))];
    if (!assetPaths.length) throw new Error("Hosted root contains no hashed JavaScript/CSS assets.");
    if (assetPaths.length > MAX_HOSTED_FILE_COUNT) throw new Error("Hosted root referenced too many static assets.");
    const assets = [];
    for (const path of assetPaths) {
        assets.push({
            path,
            response: await readHostedResponse(new URL(path, baseUrl), {
                fetchImpl,
                expectedOrigin: origin,
                maximumBytes: MAX_ASSET_PROBE_BYTES,
                budget,
            }),
        });
    }
    for (const { response } of assets) {
        if (!cache(response).includes("max-age=31536000") || !cache(response).includes("immutable")) throw new Error("Hosted hashed asset does not have the frozen immutable cache policy.");
    }
    const session = await readHostedResponse(new URL("/session/me", baseUrl), {
        fetchImpl,
        expectedOrigin: origin,
        expectedStatuses: [401],
        maximumBytes: MAX_SESSION_PROBE_BYTES,
        budget,
    });
    let sessionCode;
    try { sessionCode = JSON.parse(session.body).error_code; } catch { /* handled by the assertion below */ }
    if (session.status !== 401 || !["AUTHENTICATION_REQUIRED", "SESSION_INVALID", "SESSION_EXPIRED"].includes(sessionCode)) throw new Error("Preview auth bootstrap rewrite did not fail closed with a structured unauthenticated response.");
    const bundle = `${root.body}\n${deep.body}\n${assets.map(({ response }) => response.body).join("\n")}`;
    for (const forbidden of ["localhost", "run.app/api", "/api/v1/dev/", "PRIVATE KEY", "service_account"]) {
        if (bundle.includes(forbidden)) throw new Error(`Hosted frontend leaks a forbidden production string: ${forbidden}`);
    }
    return {
        rootHealthy: true,
        deepLinkHealthy: true,
        htmlNoCache: true,
        assetsHealthyAndImmutable: true,
        authBootstrapFailClosed: true,
        noPrivateCoreLeak: true,
    };
}

export async function verifyHostedVersion(baseUrl, {
    expectedUrl,
    versionName,
    publicDirectory,
    firebaseConfigPath,
    lock = loadFrozenRelease(),
    request = hostingRequest,
    fetchImpl = fetch,
    budget = createHostedVerificationBudget(),
} = {}) {
    const previewUrl = assertExactPreviewUrl(baseUrl, expectedUrl);
    if (!new RegExp(`^sites/${lock.firebase.site}/versions/[A-Za-z0-9_-]+$`).test(versionName ?? "")) {
        throw new Error("Hosted verification received the wrong Firebase version.");
    }
    const expectedFiles = expectedFrozenFiles(publicDirectory);
    const expectedConfig = normalizeFrozenHostingConfig(JSON.parse(readFileSync(firebaseConfigPath, "utf8")));
    const version = await request(versionName);
    if (version?.name !== versionName) throw new Error("Hosted verification received the wrong Firebase version.");
    if (canonicalJson(version.config) !== canonicalJson(expectedConfig)) {
        throw new Error("Firebase Hosting serving config differs from the frozen configuration.");
    }
    const versionFiles = await readHostedVersionFiles(versionName, request);
    assertHostedVersionInventory({ version, files: versionFiles, expectedFiles });
    for (const expectedFile of expectedFiles) {
        const url = new URL(previewUrl);
        url.pathname = expectedFile.path;
        const response = await readHostedResponse(url, {
            fetchImpl,
            expectedOrigin: previewUrl.origin,
            maximumBytes: MAX_HOSTED_FILE_BYTES,
            budget,
        });
        if (response.bytes.length !== expectedFile.bytes.length || sha256Bytes(response.bytes) !== expectedFile.sha256 || !response.bytes.equals(expectedFile.bytes)) {
            throw hostedVerificationError("BYTE_MISMATCH", `hosted file differs from the frozen frontend: ${expectedFile.path}`);
        }
    }
    const behavior = await verifyHostedServingBehavior(previewUrl, { fetchImpl, budget });
    return {
        ...behavior,
        exactVersionInventory: true,
        exactHostingConfig: true,
        frozenArtifactByteExact: true,
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
    const preview = await deployPreview(runId, { workDirectory, lock });
    const checks = await verifyHostedVersion(preview.url, {
        expectedUrl: preview.url,
        versionName: preview.version,
        publicDirectory,
        firebaseConfigPath: join(workDirectory, "firebase.json"),
    });
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
        const checks = await verifyHostedServingBehavior(`https://${lock.firebase.site}.web.app`);
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
    return { before, result: after, checks: { exactRollbackVersion: true, ...(await verifyHostedServingBehavior(`https://${lock.firebase.site}.web.app`)) } };
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
