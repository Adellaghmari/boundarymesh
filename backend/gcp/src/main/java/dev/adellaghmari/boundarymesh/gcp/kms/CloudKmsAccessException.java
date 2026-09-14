package dev.adellaghmari.boundarymesh.gcp.kms;

/**
 * Fail-closed KMS error. Messages never copy provider text that might echo tokens or key material.
 */
public final class CloudKmsAccessException extends RuntimeException {

    public enum Kind {
        UNAVAILABLE,
        PERMISSION_DENIED,
        INVALID_ARGUMENT,
        PROVIDER_ERROR
    }

    private final Kind kind;

    private CloudKmsAccessException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    public static CloudKmsAccessException wrap(Throwable cause) {
        Kind kind = classify(cause);
        return new CloudKmsAccessException(kind, "Cloud KMS call failed (" + kind.name() + ")");
    }

    static Kind classify(Throwable cause) {
        String haystack = classNames(cause);
        if (haystack.contains("permission") || haystack.contains("unauthenticated")) {
            return Kind.PERMISSION_DENIED;
        }
        if (haystack.contains("unavailable") || haystack.contains("deadline") || haystack.contains("timeout")) {
            return Kind.UNAVAILABLE;
        }
        if (haystack.contains("invalidargument") || haystack.contains("invalid_argument")) {
            return Kind.INVALID_ARGUMENT;
        }
        return Kind.PROVIDER_ERROR;
    }

    private static String classNames(Throwable cause) {
        StringBuilder names = new StringBuilder();
        Throwable current = cause;
        int depth = 0;
        while (current != null && depth < 8) {
            names.append(current.getClass().getName()).append(' ');
            current = current.getCause();
            depth++;
        }
        return names.toString().toLowerCase();
    }
}
