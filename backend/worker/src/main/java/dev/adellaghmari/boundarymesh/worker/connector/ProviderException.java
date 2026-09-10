package dev.adellaghmari.boundarymesh.worker.connector;

import dev.adellaghmari.boundarymesh.domain.delivery.ProviderFailureCategory;

public class ProviderException extends RuntimeException {

    private final int status;
    private final ProviderFailureCategory category;
    private final boolean retryable;
    private final String retryAfter;

    public ProviderException(
            int status, ProviderFailureCategory category, boolean retryable, String message, String retryAfter) {
        super(message);
        this.status = status;
        this.category = category;
        this.retryable = retryable;
        this.retryAfter = retryAfter;
    }

    public int status() {
        return status;
    }

    public ProviderFailureCategory category() {
        return category;
    }

    public boolean retryable() {
        return retryable;
    }

    public String retryAfter() {
        return retryAfter;
    }
}
