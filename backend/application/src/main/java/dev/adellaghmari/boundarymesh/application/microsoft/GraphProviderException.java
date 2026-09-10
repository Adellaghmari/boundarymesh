package dev.adellaghmari.boundarymesh.application.microsoft;

public final class GraphProviderException extends RuntimeException {

    private final int status;
    private final String retryAfter;

    public GraphProviderException(int status, String message, String retryAfter) {
        super(message);
        this.status = status;
        this.retryAfter = retryAfter;
    }

    public int status() {
        return status;
    }

    public String retryAfter() {
        return retryAfter;
    }

    public boolean retryable() {
        return status == 429 || status >= 500 || status == 0;
    }
}
