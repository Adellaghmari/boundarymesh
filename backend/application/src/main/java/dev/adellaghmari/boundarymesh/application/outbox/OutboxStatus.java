package dev.adellaghmari.boundarymesh.application.outbox;

/** Minimal outbox lifecycle. Failed publishes stay {@link #PENDING} and remain retryable. */
public enum OutboxStatus {
    PENDING,
    PUBLISHED
}
