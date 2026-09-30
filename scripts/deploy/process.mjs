import { spawnSync } from "node:child_process";

export function runChecked(command, args, { cwd, capture = false, allowFailure = false, env = process.env } = {}) {
    const result = spawnSync(command, args, {
        cwd,
        env,
        encoding: "utf8",
        stdio: capture ? ["ignore", "pipe", "pipe"] : "inherit",
        windowsHide: true,
    });
    if (result.error) throw result.error;
    if (!allowFailure && result.status !== 0) {
        const safeError = capture && result.stderr ? `: ${result.stderr.trim()}` : "";
        throw new Error(`${command} failed with exit code ${result.status}${safeError}`);
    }
    return result;
}

export function runJson(command, args, options = {}) {
    const result = runChecked(command, args, { ...options, capture: true });
    return JSON.parse(result.stdout);
}
