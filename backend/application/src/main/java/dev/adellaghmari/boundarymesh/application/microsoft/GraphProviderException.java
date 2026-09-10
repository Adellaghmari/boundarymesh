package dev.adellaghmari.boundarymesh.application.microsoft;

public final class GraphProviderException extends RuntimeException {

    private final int status;
    private final String retryAfter;
    private final String errorCode;
    private final String errorCategory;
    private final boolean wwwAuthenticatePresent;

    public GraphProviderException(int status, String message, String retryAfter) {
        this(status, message, retryAfter, null, null, false);
    }

    public GraphProviderException(
            int status,
            String message,
            String retryAfter,
            String errorCode,
            String errorCategory,
            boolean wwwAuthenticatePresent) {
        super(message);
        this.status = status;
        this.retryAfter = retryAfter;
        this.errorCode = errorCode;
        this.errorCategory = errorCategory;
        this.wwwAuthenticatePresent = wwwAuthenticatePresent;
    }

    public int status() {
        return status;
    }

    public String retryAfter() {
        return retryAfter;
    }

    public String errorCode() {
        return errorCode;
    }

    public String errorCategory() {
        return errorCategory;
    }

    public boolean wwwAuthenticatePresent() {
        return wwwAuthenticatePresent;
    }

    public boolean retryable() {
        return status == 429 || status >= 500 || status == 0;
    }
}
