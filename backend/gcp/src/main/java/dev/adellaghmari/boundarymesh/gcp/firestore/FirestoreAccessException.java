package dev.adellaghmari.boundarymesh.gcp.firestore;

/**
 * Fail-closed Firestore error. Messages never copy provider text that might echo tokens, document
 * payloads, or ADC material.
 */
public final class FirestoreAccessException extends RuntimeException {

    public enum Kind {
        NOT_FOUND,
        PERMISSION_DENIED,
        PROVIDER_UNAVAILABLE,
        PROVIDER_ERROR
    }

    private final Kind kind;

    private FirestoreAccessException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    public static FirestoreAccessException wrap(Throwable cause) {
        Kind kind = classify(cause);
        return new FirestoreAccessException(kind, "Firestore call failed (" + kind.name() + ")");
    }

    static Kind classify(Throwable cause) {
        String haystack = classNames(cause);
        if (haystack.contains("notfound") || haystack.contains("not_found")) {
            return Kind.NOT_FOUND;
        }
        if (haystack.contains("permission") || haystack.contains("unauthenticated")) {
            return Kind.PERMISSION_DENIED;
        }
        if (haystack.contains("unavailable") || haystack.contains("deadline") || haystack.contains("timeout")) {
            return Kind.PROVIDER_UNAVAILABLE;
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
