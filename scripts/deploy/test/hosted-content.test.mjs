import assert from "node:assert/strict";
import { mkdtempSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import test from "node:test";
import {
    MAX_FROZEN_TOTAL_BYTES,
    MAX_HOSTED_FILE_COUNT,
    MAX_HOSTED_SHELL_REREAD_BYTES,
    MAX_HOSTED_TOTAL_BYTES,
    MAX_VERSION_FILE_COUNT,
    assertFrozenFileCount,
    assertHostedVersionInventory,
    createHostedVerificationBudget,
    frozenFrontendFiles,
    normalizeFrozenHostingConfig,
    parseVersionFileCount,
    readHostedResponse,
    verifyHostedServingBehavior,
    verifyHostedVersion,
} from "../cloud-hosting.mjs";

const PUBLISHED_FULL_CONSOLE_FILE_COUNT = 77;
const PREVIOUS_HOSTED_FILE_CAP = 16;
const REVIEWED_DEVTOOLS_ARCHIVE_FILES = 115;
const REVIEWED_DEVTOOLS_ARCHIVE_BYTES = 6_006_191;

const previewUrl = "https://boundarymesh-dev--bm-100-abc.web.app/";
const versionName = "sites/boundarymesh-dev/versions/preview100";

function fixture() {
    const directory = mkdtempSync(join(tmpdir(), "bm-hosted-content-"));
    const publicDirectory = join(directory, "public");
    mkdirSync(join(publicDirectory, "assets"), { recursive: true });
    const files = new Map([
        ["/index.html", Buffer.from('<script type="module" src="/assets/app.js"></script>')],
        ["/assets/app.js", Buffer.from('import("./split.js");')],
        ["/assets/split.js", Buffer.from('const firebaseConfig={projectId:"boundarymesh-dev"};')],
    ]);
    for (const [path, bytes] of files) writeFileSync(join(publicDirectory, path.slice(1)), bytes);
    const firebaseConfig = {
        hosting: {
            public: "frontend/dist",
            ignore: ["firebase.json"],
            headers: [
                { source: "**", headers: [{ key: "Cache-Control", value: "no-cache" }] },
                { source: "/assets/**", headers: [{ key: "Cache-Control", value: "public,max-age=31536000,immutable" }] },
            ],
            rewrites: [
                { source: "/session/**", run: { serviceId: "boundarymesh-web", region: "europe-west4" } },
                { source: "**", destination: "/index.html" },
            ],
        },
    };
    const firebaseConfigPath = join(directory, "firebase.json");
    writeFileSync(firebaseConfigPath, JSON.stringify(firebaseConfig));
    const version = {
        name: versionName,
        status: "FINALIZED",
        finalizeTime: "2026-10-01T19:13:00Z",
        fileCount: "5",
        config: normalizeFrozenHostingConfig(firebaseConfig),
    };
    const inventory = [
        ...files.keys(),
        "/__/firebase/init.js",
        "/__/firebase/init.json",
    ].map((path) => ({ path, hash: `hosting-hash-${path}`, status: "ACTIVE" }));
    return { directory, publicDirectory, firebaseConfigPath, files, version, inventory };
}

function response(body, { status = 200, cache = "no-cache", headers = {} } = {}) {
    return new Response(body, { status, headers: { "cache-control": cache, ...headers } });
}

function harness({
    mutateVersion = (value) => value,
    mutateInventory = (value) => value,
    inventoryPages,
    fetchOverride,
    budget,
} = {}) {
    const state = fixture();
    const request = async (path) => {
        if (path === versionName) return mutateVersion(structuredClone(state.version));
        if (!path.startsWith(`${versionName}/files?pageSize=100`)) throw new Error(`Unexpected REST request: ${path}`);
        if (inventoryPages) {
            const token = new URLSearchParams(path.slice(path.indexOf("?") + 1)).get("pageToken");
            const page = token
                ? inventoryPages.find((entry) => entry.token === token)
                : inventoryPages[0];
            if (!page) throw new Error(`Unexpected page token: ${token}`);
            return { files: page.files, nextPageToken: page.nextPageToken };
        }
        return { files: mutateInventory(structuredClone(state.inventory)) };
    };
    const fetchImpl = async (input, options) => {
        const url = new URL(input);
        assert.equal(options.redirect, "manual");
        assert.equal(options.headers["Accept-Encoding"], "identity");
        const overridden = fetchOverride?.(url, state);
        if (overridden) return overridden;
        if (url.pathname === "/transactions/demo") return response(state.files.get("/index.html"));
        if (url.pathname === "/session/me") return response(JSON.stringify({ error_code: "AUTHENTICATION_REQUIRED" }), { status: 401 });
        const path = url.pathname === "/" ? "/index.html" : url.pathname;
        const bytes = state.files.get(path);
        if (!bytes) return response("missing", { status: 404 });
        return response(bytes, { cache: path.startsWith("/assets/") ? "public,max-age=31536000,immutable" : "no-cache" });
    };
    const verify = () => verifyHostedVersion(previewUrl, {
        expectedUrl: previewUrl,
        versionName,
        publicDirectory: state.publicDirectory,
        firebaseConfigPath: state.firebaseConfigPath,
        request,
        fetchImpl,
        ...(budget ? { budget } : {}),
    });
    return { ...state, fetchImpl, verify };
}

function controlledResponse(chunks, {
    status = 200,
    headers = {},
    url = previewUrl,
    throwAtRead,
} = {}) {
    const state = { reads: 0, cancelled: false };
    const encoded = chunks.map((chunk) => typeof chunk === "symbol" ? chunk : Buffer.from(chunk));
    const reader = {
        async read() {
            state.reads += 1;
            if (throwAtRead === state.reads) throw new Error("synthetic stream failure");
            const chunk = encoded[state.reads - 1];
            return chunk === undefined ? { done: true } : { done: false, value: chunk };
        },
        async cancel() {
            state.cancelled = true;
        },
    };
    return {
        state,
        response: {
            status,
            redirected: false,
            url,
            headers: new Headers(headers),
            body: { getReader: () => reader, cancel: () => reader.cancel() },
        },
    };
}

async function readControlled(controlled, {
    maximumBytes = 1024,
    budget = createHostedVerificationBudget({ maximumBytes: 4096 }),
    timeoutMs = 1_000,
} = {}) {
    return readHostedResponse(new URL("/bounded", previewUrl), {
        fetchImpl: async () => controlled.response,
        expectedOrigin: new URL(previewUrl).origin,
        maximumBytes,
        budget,
        timeoutMs,
    });
}

test("exact frozen file set and split-chunk Firebase config pass byte-exact verification", async () => {
    const result = await harness().verify();
    assert.equal(result.frozenArtifactByteExact, true);
    assert.equal(result.exactVersionInventory, true);
    assert.equal(result.exactHostingConfig, true);
});

test("missing expected Firebase version file fails closed", async () => {
    await assert.rejects(
        () => harness({ mutateInventory: (files) => files.filter((file) => file.path !== "/assets/split.js") }).verify(),
        /missing a frozen frontend path/,
    );
});

test("extra unexpected Firebase version file fails closed", async () => {
    await assert.rejects(
        () => harness({ mutateInventory: (files) => [...files, { path: "/unexpected.txt", hash: "opaque", status: "ACTIVE" }] }).verify(),
        /unexpected user-deployed content/,
    );
});

function inventoryArgs(overrides = {}) {
    const state = fixture();
    const version = { ...state.version };
    if ("fileCount" in overrides) version.fileCount = overrides.fileCount;
    return {
        version,
        files: overrides.files ?? state.inventory,
        expectedFiles: overrides.expectedFiles ?? [...state.files.keys()].map((path) => ({ path })),
    };
}

test("inspectable inventory is the files.list path set, not Version.fileCount equality", () => {
    const args = inventoryArgs({ fileCount: "11" });
    assert.equal(args.files.length, 5);
    assert.equal(assertHostedVersionInventory(args), true);
    assert.equal(assertHostedVersionInventory(inventoryArgs({ fileCount: undefined })), true);
    assert.equal(assertHostedVersionInventory(inventoryArgs({ fileCount: "0" })), true);
    assert.deepEqual(parseVersionFileCount("9"), { available: true, value: 9 });
    assert.equal(parseVersionFileCount(undefined).available, false);
});

test("fileCount below the frozen deploy-owned count fails closed", () => {
    assert.throws(
        () => assertHostedVersionInventory(inventoryArgs({ fileCount: "2" })),
        /fileCount is below the frozen deploy-owned file count/,
    );
});

test("invalid and unbounded fileCount values fail closed", () => {
    for (const fileCount of ["", "09", "-1", "1.5", "1e2", "NaN", 1.5, MAX_VERSION_FILE_COUNT + 1, String(MAX_VERSION_FILE_COUNT + 1), "10000", true, {}, []]) {
        assert.throws(
            () => assertHostedVersionInventory(inventoryArgs({ fileCount })),
            /fileCount/,
        );
    }
});

test("Version.fileCount is not the frozen inventory authority", () => {
    assert.deepEqual(parseVersionFileCount("27"), { available: true, value: 27 });
    const args = inventoryArgs({ fileCount: "27" });
    assert.equal(args.files.length, 5);
    assert.notEqual(args.files.length, 27);
    assert.equal(assertHostedVersionInventory(args), true);
});

function writeInventory(count) {
    const directory = mkdtempSync(join(tmpdir(), "bm-frozen-inventory-"));
    for (let index = 0; index < count; index += 1) {
        writeFileSync(join(directory, `asset-${index}.txt`), Buffer.from("synthetic"));
    }
    return directory;
}

test("the published 77 file console passes the reviewed ceiling", () => {
    assert.equal(frozenFrontendFiles(writeInventory(PUBLISHED_FULL_CONSOLE_FILE_COUNT)).length, PUBLISHED_FULL_CONSOLE_FILE_COUNT);
    assertFrozenFileCount(PUBLISHED_FULL_CONSOLE_FILE_COUNT);
    assert.ok(PUBLISHED_FULL_CONSOLE_FILE_COUNT <= MAX_HOSTED_FILE_COUNT);
});

test("stage 37238811079 fails the old 16 file cap and passes the reviewed ceiling", () => {
    assert.throws(
        () => assertFrozenFileCount(PUBLISHED_FULL_CONSOLE_FILE_COUNT, PREVIOUS_HOSTED_FILE_CAP),
        /Frozen frontend file count exceeded the verification limit/,
    );
    assert.doesNotThrow(() => assertFrozenFileCount(PUBLISHED_FULL_CONSOLE_FILE_COUNT));
});

test("one file above the hard safety ceiling fails closed", () => {
    assert.throws(
        () => frozenFrontendFiles(writeInventory(MAX_HOSTED_FILE_COUNT + 1)),
        /Frozen frontend file count exceeded the verification limit/,
    );
});

function writeByteInventory(count, totalBytes) {
    const directory = mkdtempSync(join(tmpdir(), "bm-frozen-bytes-"));
    const perFileLimit = 5 * 1024 * 1024;
    let remaining = totalBytes;
    for (let index = 0; index < count; index += 1) {
        const filesLeft = count - index;
        const size = Math.min(perFileLimit, Math.max(0, remaining - (filesLeft - 1)));
        writeFileSync(join(directory, `asset-${index}.bin`), Buffer.alloc(size));
        remaining -= size;
    }
    if (remaining !== 0) throw new Error("test inventory could not place the requested byte count");
    return directory;
}

test("the reviewed Vue DevTools archive passes the frozen byte and file ceilings", () => {
    assert.ok(REVIEWED_DEVTOOLS_ARCHIVE_FILES <= MAX_HOSTED_FILE_COUNT);
    assert.ok(REVIEWED_DEVTOOLS_ARCHIVE_BYTES <= MAX_FROZEN_TOTAL_BYTES);
    assert.equal(MAX_HOSTED_TOTAL_BYTES, MAX_FROZEN_TOTAL_BYTES + MAX_HOSTED_SHELL_REREAD_BYTES);
    const files = frozenFrontendFiles(writeByteInventory(REVIEWED_DEVTOOLS_ARCHIVE_FILES, REVIEWED_DEVTOOLS_ARCHIVE_BYTES));
    assert.equal(files.length, REVIEWED_DEVTOOLS_ARCHIVE_FILES);
    assert.equal(files.reduce((sum, file) => sum + file.bytes.length, 0), REVIEWED_DEVTOOLS_ARCHIVE_BYTES);
});

test("one byte above the reviewed frozen aggregate fails closed", () => {
    assert.throws(
        () => frozenFrontendFiles(writeByteInventory(2, MAX_FROZEN_TOTAL_BYTES + 1)),
        /Frozen frontend exceeded the total verification size limit/,
    );
});

test("the hosted aggregate budget stays enforced above the reviewed archive", () => {
    const budget = createHostedVerificationBudget();
    assert.equal(budget.maximumBytes, MAX_HOSTED_TOTAL_BYTES);
    assert.doesNotThrow(() => budget.chargeBytes(REVIEWED_DEVTOOLS_ARCHIVE_BYTES));
    assert.throws(
        () => budget.chargeBytes(MAX_HOSTED_TOTAL_BYTES - REVIEWED_DEVTOOLS_ARCHIVE_BYTES + 1),
        (error) => error.code === "TOTAL_BUDGET_EXCEEDED",
    );
    assert.throws(
        () => createHostedVerificationBudget({ maximumBytes: MAX_HOSTED_TOTAL_BYTES + 1 }),
        /byte budget is invalid/,
    );
});

test("unexpected Firebase-reserved paths fail unless explicitly allowed", () => {
    const extraReserved = [...fixture().inventory, { path: "/__/firebase/unknown.json", hash: "opaque", status: "ACTIVE" }];
    assert.throws(
        () => assertHostedVersionInventory(inventoryArgs({ files: extraReserved })),
        /unexpected reserved path/,
    );
    const missingReserved = fixture().inventory.filter((file) => file.path !== "/__/firebase/init.json");
    assert.throws(
        () => assertHostedVersionInventory(inventoryArgs({ files: missingReserved })),
        /missing an expected Firebase-owned reserved path/,
    );
});

test("duplicate normalized inventory paths fail closed", () => {
    const files = [...fixture().inventory, { path: "/index.html", hash: "duplicate", status: "ACTIVE" }];
    assert.throws(
        () => assertHostedVersionInventory(inventoryArgs({ files })),
        /duplicate paths/,
    );
});

test("paginated files.list responses are fully consumed", async () => {
    const state = fixture();
    const [first, ...rest] = state.inventory;
    const result = await harness({
        inventoryPages: [
            { files: [first], nextPageToken: "page-2" },
            { token: "page-2", files: rest },
        ],
    }).verify();
    assert.equal(result.exactVersionInventory, true);
    assert.equal(result.frozenArtifactByteExact, true);
});

test("nextPageToken is required until absent and pagination is bounded", async () => {
    await assert.rejects(
        () => harness({
            inventoryPages: [
                { files: fixture().inventory.slice(0, 1), nextPageToken: "page-2" },
                { token: "page-2", files: fixture().inventory.slice(1, 2), nextPageToken: "page-3" },
                { token: "page-3", files: fixture().inventory.slice(2, 3), nextPageToken: "page-4" },
                { token: "page-4", files: fixture().inventory.slice(3, 4), nextPageToken: "page-5" },
                { token: "page-5", files: fixture().inventory.slice(4) },
            ],
        }).verify(),
        /pagination limit/,
    );
});

test("wrong served bytes fail closed", async () => {
    await assert.rejects(
        () => harness({
            fetchOverride: (url) => url.pathname === "/assets/split.js"
                ? response("wrong", { cache: "public,max-age=31536000,immutable" })
                : null,
        }).verify(),
        /differs from the frozen frontend/,
    );
});

test("wrong preview host fails before network verification", async () => {
    const state = harness();
    await assert.rejects(
        () => verifyHostedVersion("https://other.web.app/", {
            expectedUrl: previewUrl,
            versionName,
            publicDirectory: state.publicDirectory,
            firebaseConfigPath: state.firebaseConfigPath,
            request: async () => { throw new Error("must not be called"); },
            fetchImpl: async () => { throw new Error("must not be called"); },
        }),
        /exact run-specific preview/,
    );
});

test("cross-host redirects fail closed", async () => {
    await assert.rejects(
        () => harness({
            fetchOverride: (url) => url.pathname === "/assets/app.js"
                ? response(null, { status: 302, headers: { location: "https://evil.example/app.js" } })
                : null,
        }).verify(),
        /cross-host redirect/,
    );
});

test("every non-200 frozen file response fails closed", async () => {
    for (const status of [204, 206, 503]) {
        await assert.rejects(
            () => harness({
                fetchOverride: (url, state) => url.pathname === "/assets/app.js"
                    ? response(status === 204 ? null : state.files.get("/assets/app.js"), { status })
                    : null,
            }).verify(),
            new RegExp(`HTTP ${status}`),
        );
    }
});

test("unexpected content encoding is rejected instead of changing the raw byte contract", async () => {
    const state = harness({
        fetchOverride: (url, current) => url.pathname === "/assets/split.js"
            ? response(current.files.get("/assets/split.js"), {
                cache: "public,max-age=31536000,immutable",
                headers: { "content-encoding": "gzip" },
            })
            : null,
    });
    await assert.rejects(() => state.verify(), /BYTE_MISMATCH.*encoded response/);
});

test("non-FINALIZED versions and inactive files fail closed", async () => {
    await assert.rejects(
        () => harness({ mutateVersion: (version) => ({ ...version, status: "CREATED", finalizeTime: undefined }) }).verify(),
        /FINALIZED/,
    );
    await assert.rejects(
        () => harness({ mutateInventory: (files) => files.map((file, index) => index ? file : { ...file, status: "PROCESSING" }) }).verify(),
        /non-ACTIVE/,
    );
});

test("wrong version identity and Hosting config fail closed", async () => {
    const state = harness();
    await assert.rejects(
        () => verifyHostedVersion(previewUrl, {
            expectedUrl: previewUrl,
            versionName: "sites/other/versions/preview100",
            publicDirectory: state.publicDirectory,
            firebaseConfigPath: state.firebaseConfigPath,
            request: async () => { throw new Error("must not be called"); },
            fetchImpl: async () => { throw new Error("must not be called"); },
        }),
        /wrong Firebase version/,
    );
    await assert.rejects(
        () => harness({ mutateVersion: (version) => ({ ...version, config: { rewrites: [] } }) }).verify(),
        /serving config differs/,
    );
});

test("frozen Hosting redirects are part of the serving config identity", () => {
    const withRedirects = normalizeFrozenHostingConfig({
        hosting: {
            headers: [],
            redirects: [
                { source: "/explore", destination: "/", type: 301 },
                { source: "/explore/**", destination: "/", type: 301 },
            ],
            rewrites: [{ source: "**", destination: "/index.html" }],
        },
    });
    assert.deepEqual(withRedirects.redirects, [
        { glob: "/explore", location: "/", statusCode: 301 },
        { glob: "/explore/**", location: "/", statusCode: 301 },
    ]);
    const withoutRedirects = normalizeFrozenHostingConfig({
        hosting: {
            headers: [],
            rewrites: [{ source: "**", destination: "/index.html" }],
        },
    });
    assert.equal(Object.hasOwn(withoutRedirects, "redirects"), false);
});

test("old single-bundle substring assumption is not authoritative", async () => {
    const state = harness();
    assert.doesNotMatch(readFileSync(join(state.publicDirectory, "index.html"), "utf8"), /boundarymesh-dev/);
    assert.doesNotMatch(readFileSync(join(state.publicDirectory, "assets", "app.js"), "utf8"), /boundarymesh-dev/);
    assert.match(readFileSync(join(state.publicDirectory, "assets", "split.js"), "utf8"), /boundarymesh-dev/);
    assert.equal((await state.verify()).frozenArtifactByteExact, true);
});

test("Hosting REST and served-byte requests share the exact 30-second timeout bound", () => {
    const source = readFileSync(join(import.meta.dirname, "..", "cloud-hosting.mjs"), "utf8");
    assert.match(source, /const HOSTED_REQUEST_TIMEOUT_MS = 30_000;/);
    assert.equal((source.match(/AbortSignal\.timeout\(HOSTED_REQUEST_TIMEOUT_MS\)/g) ?? []).length, 1);
    assert.match(source, /timeoutMs = HOSTED_REQUEST_TIMEOUT_MS/);
    assert.doesNotMatch(source, /AbortSignal\.timeout\(60_000\)/);
});

test("small chunked responses and exact-limit bodies pass streaming verification", async () => {
    const small = controlledResponse(["ab", "cd"]);
    const result = await readControlled(small, { maximumBytes: 4 });
    assert.equal(result.body, "abcd");
    assert.equal(small.state.reads, 3);
    assert.equal(small.state.cancelled, false);
});

test("streaming cap stops before later chunks are consumed", async () => {
    const oversized = controlledResponse(["ab", "cd", "must-not-be-read"]);
    const budget = createHostedVerificationBudget({ maximumBytes: 100 });
    await assert.rejects(
        () => readControlled(oversized, { maximumBytes: 3, budget }),
        (error) => error.code === "RESPONSE_TOO_LARGE",
    );
    assert.equal(oversized.state.reads, 2);
    assert.equal(oversized.state.cancelled, true);
    assert.equal(budget.usedBytes, 2);
});

test("Content-Length is only an early rejection optimization", async () => {
    const declaredOversized = controlledResponse(["unread"], { headers: { "content-length": "9" } });
    await assert.rejects(
        () => readControlled(declaredOversized, { maximumBytes: 8 }),
        (error) => error.code === "RESPONSE_TOO_LARGE",
    );
    assert.equal(declaredOversized.state.reads, 0);
    assert.equal(declaredOversized.state.cancelled, true);

    for (const headers of [{}, { "content-length": "1" }]) {
        const streamedOversized = controlledResponse(["1234", "5"], { headers });
        await assert.rejects(
            () => readControlled(streamedOversized, { maximumBytes: 4 }),
            (error) => error.code === "RESPONSE_TOO_LARGE",
        );
        assert.equal(streamedOversized.state.reads, 2);
        assert.equal(streamedOversized.state.cancelled, true);
    }
});

test("limit plus one fails while an exact-limit body succeeds", async () => {
    assert.equal((await readControlled(controlledResponse(["1234"]), { maximumBytes: 4 })).body, "1234");
    await assert.rejects(
        () => readControlled(controlledResponse(["1234", "5"]), { maximumBytes: 4 }),
        (error) => error.code === "RESPONSE_TOO_LARGE",
    );
});

test("shared aggregate budget accepts cumulative valid responses under its limit", async () => {
    const budget = createHostedVerificationBudget({ maximumBytes: 7 });
    await readControlled(controlledResponse(["abc"]), { budget });
    await readControlled(controlledResponse(["defg"]), { budget });
    assert.equal(budget.usedBytes, 7);
    assert.equal(budget.usedRequests, 2);
});

test("shared aggregate budget rejects before accepting the over-budget chunk", async () => {
    const budget = createHostedVerificationBudget({ maximumBytes: 7 });
    await readControlled(controlledResponse(["abcd"]), { budget });
    const overBudget = controlledResponse(["efgh", "must-not-be-read"]);
    await assert.rejects(
        () => readControlled(overBudget, { budget }),
        (error) => error.code === "TOTAL_BUDGET_EXCEEDED",
    );
    assert.equal(budget.usedBytes, 4);
    assert.equal(overBudget.state.reads, 1);
    assert.equal(overBudget.state.cancelled, true);
});

test("static verification and behavior probes share one aggregate budget", async () => {
    const sample = fixture();
    const staticBytes = [...sample.files.values()].reduce((sum, bytes) => sum + bytes.length, 0);
    const rootBytes = sample.files.get("/index.html").length;
    const budget = createHostedVerificationBudget({ maximumBytes: staticBytes + rootBytes - 1 });
    const state = harness({ budget });
    await assert.rejects(
        () => state.verify(),
        (error) => error.code === "TOTAL_BUDGET_EXCEEDED",
    );
    assert.equal(budget.usedBytes, staticBytes);
});

test("a budget consumed by behavior probes cannot reset before a later frozen-file read", async () => {
    const state = harness();
    const measuringBudget = createHostedVerificationBudget({ maximumBytes: 4096 });
    await verifyHostedServingBehavior(previewUrl, { fetchImpl: state.fetchImpl, budget: measuringBudget });
    const sharedBudget = createHostedVerificationBudget({ maximumBytes: measuringBudget.usedBytes + 1 });
    await verifyHostedServingBehavior(previewUrl, { fetchImpl: state.fetchImpl, budget: sharedBudget });
    const frozenRead = controlledResponse(["xx"]);
    await assert.rejects(
        () => readControlled(frozenRead, { budget: sharedBudget }),
        (error) => error.code === "TOTAL_BUDGET_EXCEEDED",
    );
    assert.equal(sharedBudget.usedBytes, measuringBudget.usedBytes);
    assert.equal(frozenRead.state.cancelled, true);
});

test("request count is shared and bounded without retries", async () => {
    const budget = createHostedVerificationBudget({ maximumBytes: 100, maximumRequests: 1 });
    let fetchCalls = 0;
    const fetchImpl = async () => {
        fetchCalls += 1;
        return controlledResponse(["ok"]).response;
    };
    await readHostedResponse(new URL("/one", previewUrl), {
        fetchImpl,
        expectedOrigin: new URL(previewUrl).origin,
        budget,
    });
    await assert.rejects(
        () => readHostedResponse(new URL("/two", previewUrl), {
            fetchImpl,
            expectedOrigin: new URL(previewUrl).origin,
            budget,
        }),
        (error) => error.code === "REQUEST_COUNT_EXCEEDED",
    );
    assert.equal(fetchCalls, 1);
});

test("timeout aborts the request and is not retried", async () => {
    let fetchCalls = 0;
    const fetchImpl = async (_url, { signal }) => {
        fetchCalls += 1;
        return new Promise((_resolve, reject) => {
            signal.addEventListener("abort", () => reject(new DOMException("aborted", "AbortError")), { once: true });
        });
    };
    await assert.rejects(
        () => readHostedResponse(new URL("/timeout", previewUrl), {
            fetchImpl,
            expectedOrigin: new URL(previewUrl).origin,
            timeoutMs: 5,
            budget: createHostedVerificationBudget({ maximumBytes: 100 }),
        }),
        (error) => error.code === "REQUEST_TIMEOUT",
    );
    assert.equal(fetchCalls, 1);
});

test("aborted or malformed stream reads fail closed without retry", async () => {
    for (const controlled of [
        controlledResponse(["unused"], { throwAtRead: 1 }),
        controlledResponse([Symbol("invalid")]),
    ]) {
        let fetchCalls = 0;
        await assert.rejects(
            () => readHostedResponse(new URL("/broken", previewUrl), {
                fetchImpl: async () => {
                    fetchCalls += 1;
                    return controlled.response;
                },
                expectedOrigin: new URL(previewUrl).origin,
                budget: createHostedVerificationBudget({ maximumBytes: 100 }),
            }),
            (error) => error.code === "HOSTED_STREAM_READ_FAILED",
        );
        assert.equal(fetchCalls, 1);
        assert.equal(controlled.state.cancelled, true);
    }
});

test("same-host redirects are forbidden", async () => {
    await assert.rejects(
        () => harness({
            fetchOverride: (url) => url.pathname === "/assets/app.js"
                ? response(null, { status: 302, headers: { location: `${previewUrl}other` } })
                : null,
        }).verify(),
        (error) => error.code === "REDIRECT_FORBIDDEN" && /rejected a redirect/.test(error.message),
    );
});

test("preview requests without a shared budget fail closed", async () => {
    await assert.rejects(
        () => readHostedResponse(new URL("/no-budget", previewUrl), {
            fetchImpl: async () => {
                throw new Error("must not be called");
            },
            expectedOrigin: new URL(previewUrl).origin,
        }),
        /shared network budget/,
    );
});

test("declared Content-Length cannot consume remaining aggregate budget before the body is read", async () => {
    const budget = createHostedVerificationBudget({ maximumBytes: 7 });
    await readControlled(controlledResponse(["abcd"]), { budget });
    const blocked = controlledResponse(["efgh"], { headers: { "content-length": "4" } });
    await assert.rejects(
        () => readControlled(blocked, { budget }),
        (error) => error.code === "TOTAL_BUDGET_EXCEEDED",
    );
    assert.equal(budget.usedBytes, 4);
    assert.equal(blocked.state.reads, 0);
    assert.equal(blocked.state.cancelled, true);
});

test("overall verification deadline fails before another network read", async () => {
    const budget = createHostedVerificationBudget({ maximumBytes: 100, deadlineMs: 1 });
    await new Promise((resolve) => setTimeout(resolve, 10));
    let fetchCalls = 0;
    await assert.rejects(
        () => readHostedResponse(new URL("/late", previewUrl), {
            fetchImpl: async () => {
                fetchCalls += 1;
                return controlledResponse(["ok"]).response;
            },
            expectedOrigin: new URL(previewUrl).origin,
            budget,
        }),
        (error) => error.code === "REQUEST_TIMEOUT",
    );
    assert.equal(fetchCalls, 0);
    assert.equal(budget.usedRequests, 0);
});

test("unreadable response bodies fail closed without retry", async () => {
    let fetchCalls = 0;
    await assert.rejects(
        () => readHostedResponse(new URL("/unreadable", previewUrl), {
            fetchImpl: async () => {
                fetchCalls += 1;
                return {
                    status: 200,
                    redirected: false,
                    url: previewUrl,
                    headers: new Headers(),
                    body: {},
                };
            },
            expectedOrigin: new URL(previewUrl).origin,
            budget: createHostedVerificationBudget({ maximumBytes: 100 }),
        }),
        (error) => error.code === "HOSTED_STREAM_READ_FAILED",
    );
    assert.equal(fetchCalls, 1);
});

test("hosted verifier never buffers a full body before enforcing size limits", () => {
    const source = readFileSync(join(import.meta.dirname, "..", "cloud-hosting.mjs"), "utf8");
    assert.doesNotMatch(source, /arrayBuffer\s*\(/);
    assert.match(source, /createHostedVerificationBudget/);
    assert.match(source, /readBoundedResponseBody/);
    assert.match(source, /HOSTED_VERIFICATION_DEADLINE_MS = 120_000/);
    assert.match(source, /MAX_HTML_PROBE_BYTES = 256 \* 1024/);
    assert.match(source, /MAX_ASSET_PROBE_BYTES = 1024 \* 1024/);
    assert.match(source, /MAX_SESSION_PROBE_BYTES = 16 \* 1024/);
    assert.doesNotMatch(source, /Number\(version\.fileCount\) !== files\.length/);
    assert.match(source, /parseVersionFileCount/);
    assert.match(source, /MAX_VERSION_FILE_COUNT/);
    assert.match(source, /MAX_HOSTED_FILE_COUNT = 128/);
    assert.match(source, /MAX_FROZEN_TOTAL_BYTES = 6 \* 1024 \* 1024/);
    assert.match(source, /MAX_HOSTED_SHELL_REREAD_BYTES = 512 \* 1024/);
    assert.doesNotMatch(source, /MAX_FROZEN_TOTAL_BYTES = 512 \* 1024/);
    assert.doesNotMatch(source, /MAX_HOSTED_TOTAL_BYTES = 2 \* 1024 \* 1024/);
    assert.doesNotMatch(source, /MAX_HOSTED_FILE_COUNT = 16/);
    assert.doesNotMatch(source, /version\.fileCount\) !== files\.length/);
    assert.doesNotMatch(source, /\/__\/\*\*/);
});
