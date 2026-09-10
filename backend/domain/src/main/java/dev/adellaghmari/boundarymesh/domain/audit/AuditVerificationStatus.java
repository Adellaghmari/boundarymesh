package dev.adellaghmari.boundarymesh.domain.audit;

public enum AuditVerificationStatus {
    VALID,
    HASH_MISMATCH,
    LINK_MISMATCH,
    SEQUENCE_GAP,
    REORDERED,
    FOREIGN_CHAIN,
    EMPTY
}
