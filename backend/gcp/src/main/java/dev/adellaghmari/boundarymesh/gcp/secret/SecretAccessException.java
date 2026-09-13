package dev.adellaghmari.boundarymesh.gcp.secret;

import java.util.Objects;

/**
 * Fail-closed Secret Manager error. Messages contain only the secret ID and kind, never payload
 * bytes or Google error details that might echo material.
 */
public final class SecretAccessException extends RuntimeException {

    private final SecretAccessFailureKind kind;
    private final String secretId;

    private SecretAccessException(SecretAccessFailureKind kind, String secretId, String message) {
        super(message);
        this.kind = Objects.requireNonNull(kind, "kind");
        this.secretId = secretId;
    }

    public static SecretAccessException invalidReference() {
        return new SecretAccessException(
                SecretAccessFailureKind.INVALID_REFERENCE, null, "Secret reference is invalid");
    }

    public static SecretAccessException notFound(String secretId) {
        return new SecretAccessException(SecretAccessFailureKind.NOT_FOUND, secretId, "Secret was not found");
    }

    public static SecretAccessException denied(String secretId) {
        return new SecretAccessException(
                SecretAccessFailureKind.PERMISSION_DENIED, secretId, "Secret access was denied");
    }

    public static SecretAccessException unavailable(String secretId) {
        return new SecretAccessException(
                SecretAccessFailureKind.PROVIDER_UNAVAILABLE, secretId, "Secret provider is unavailable");
    }

    public SecretAccessFailureKind kind() {
        return kind;
    }

    public String secretId() {
        return secretId;
    }
}
