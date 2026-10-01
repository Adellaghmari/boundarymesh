import { spawnSync } from "node:child_process";

const MAX_DIAGNOSTIC_STREAM_LENGTH = 4096;
const MAX_CAPTURE_BUFFER_BYTES = 64 * 1024;

export function sanitizeChildProcessOutput(value, maximumLength = MAX_DIAGNOSTIC_STREAM_LENGTH) {
    if (typeof value !== "string") return "";
    const redacted = value
        .replace(/-----BEGIN (?:RSA |EC )?PRIVATE KEY-----[\s\S]*?-----END (?:RSA |EC )?PRIVATE KEY-----/g, "[REDACTED_PRIVATE_KEY]")
        .replace(/[\u0000-\u001f\u007f]+/g, " ")
        .replace(/\bBearer\s+[^\s,;]+/gi, "Bearer [REDACTED]")
        .replace(/\bya29\.[A-Za-z0-9._-]+\b/g, "[REDACTED_TOKEN]")
        .replace(/\b(?:github_pat_|gh[pousr]_)[A-Za-z0-9_]+\b/gi, "[REDACTED_TOKEN]")
        .replace(/\beyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\b/g, "[REDACTED_TOKEN]")
        .replace(/\bAIza[0-9A-Za-z_-]{20,}\b/g, "[REDACTED_API_KEY]")
        .replace(/["']?(?:authorization|private_key|client_email|client_secret|access_token|id_token|refresh_token|github_token|api[_-]?key|cookie|set-cookie)["']?\s*[:=]\s*(?:"[^"]*"|'[^']*'|[^,;}\s]+)/gi, "[REDACTED]")
        .replace(/\s+/g, " ")
        .trim();
    return redacted.slice(0, maximumLength);
}

export function sanitizeChildProcessDiagnostics({ stdout, stderr } = {}) {
    const safeStdout = sanitizeChildProcessOutput(stdout);
    const safeStderr = sanitizeChildProcessOutput(stderr);
    return {
        ...(safeStdout ? { stdout: safeStdout } : {}),
        ...(safeStderr ? { stderr: safeStderr } : {}),
    };
}

export function runChecked(
    command,
    args,
    { cwd, capture = false, allowFailure = false, sanitizeFailureOutput = false, env = process.env } = {},
) {
    const result = spawnSync(command, args, {
        cwd,
        env,
        encoding: "utf8",
        stdio: capture ? ["ignore", "pipe", "pipe"] : "inherit",
        ...(sanitizeFailureOutput ? { maxBuffer: MAX_CAPTURE_BUFFER_BYTES } : {}),
        windowsHide: true,
    });
    if (result.error) throw result.error;
    if (!allowFailure && result.status !== 0) {
        const diagnostic = sanitizeFailureOutput
            ? sanitizeChildProcessDiagnostics({ stdout: result.stdout, stderr: result.stderr })
            : null;
        const safeError = diagnostic && Object.keys(diagnostic).length
            ? `: ${JSON.stringify(diagnostic)}`
            : capture && result.stderr
                ? `: ${result.stderr.trim()}`
                : "";
        throw new Error(`${command} failed with exit code ${result.status}${safeError}`);
    }
    return result;
}

export function runJson(command, args, options = {}) {
    const result = runChecked(command, args, { ...options, capture: true });
    return JSON.parse(result.stdout);
}
