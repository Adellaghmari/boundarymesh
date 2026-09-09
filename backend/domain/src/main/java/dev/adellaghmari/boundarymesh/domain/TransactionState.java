package dev.adellaghmari.boundarymesh.domain;

/**
 * Narrow lifecycle for a boundary transaction before async delivery exists.
 *
 * <p>Delivery, attestation, and reprocess states belong to later phases.
 */
public enum TransactionState {
    RECEIVED,
    EVALUATED,
    ALLOWED,
    QUARANTINED,
    REJECTED
}
