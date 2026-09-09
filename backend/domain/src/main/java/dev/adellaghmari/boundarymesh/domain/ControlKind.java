package dev.adellaghmari.boundarymesh.domain;

/**
 * Categories on the Phase 0 {@code ControlVector}.
 */
public enum ControlKind {
    IDENTITY,
    AUTHORIZATION,
    AUTHENTICITY,
    FRESHNESS,
    REPLAY,
    CONTRACT,
    EGRESS,
    CREDENTIAL_RESOLVABILITY,
    CONNECTOR_SAFETY
}
