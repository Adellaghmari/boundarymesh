package dev.adellaghmari.boundarymesh.domain.audit;

/** Structured security/integration events. Not a log of every method call. */
public enum AuditEventType {
    BOUNDARY_DECISION,
    DELIVERY_INSTRUCTION_CREATED,
    DELIVERY_RESULT,
    QUARANTINE_CREATED,
    REPROCESS_STARTED,
    QUARANTINE_RESOLVED,
    CONNECTOR_READ,
    ATTESTATION_CREATED
}
