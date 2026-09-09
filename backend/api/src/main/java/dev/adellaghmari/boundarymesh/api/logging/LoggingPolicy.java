package dev.adellaghmari.boundarymesh.api.logging;

/**
 * Central logging redaction policy for Phase 1.
 *
 * <p>Never log: Authorization, JWT, cookies, secret values, webhook signatures, raw credentials.
 *
 * <p>Safe MDC: request_id, transaction_id, boundary_outcome, reason_code, principal_reference,
 * issuer, provider.
 *
 * <p>Spring access logs are disabled. Request bodies are never logged.
 */
public final class LoggingPolicy {

    private LoggingPolicy() {}

    public static final java.util.Set<String> DENIED_HEADERS =
            java.util.Set.of("authorization", "cookie", "set-cookie", "x-velora-signature", "x-api-key");
}
