import assert from "node:assert/strict";
import { mkdtempSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import test from "node:test";
import {
    normalizeFrozenHostingConfig,
    verifyHostedVersion,
} from "../cloud-hosting.mjs";

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
    fetchOverride,
} = {}) {
    const state = fixture();
    const request = async (path) => {
        if (path === versionName) return mutateVersion(structuredClone(state.version));
        if (path === `${versionName}/files?pageSize=100`) return { files: mutateInventory(structuredClone(state.inventory)) };
        throw new Error(`Unexpected REST request: ${path}`);
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
    });
    return { ...state, verify };
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
        /inventory differs/,
    );
});

test("extra unexpected Firebase version file fails closed", async () => {
    await assert.rejects(
        () => harness({ mutateInventory: (files) => [...files, { path: "/unexpected.txt", hash: "opaque", status: "ACTIVE" }] }).verify(),
        /inventory differs/,
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

test("decoded compressed transport bytes satisfy the raw content contract", async () => {
    const state = harness({
        fetchOverride: (url, current) => url.pathname === "/assets/split.js"
            ? response(current.files.get("/assets/split.js"), {
                cache: "public,max-age=31536000,immutable",
                headers: { "content-encoding": "gzip" },
            })
            : null,
    });
    assert.equal((await state.verify()).frozenArtifactByteExact, true);
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
    assert.equal((source.match(/AbortSignal\.timeout\(HOSTED_REQUEST_TIMEOUT_MS\)/g) ?? []).length, 2);
    assert.doesNotMatch(source, /AbortSignal\.timeout\(60_000\)/);
});
