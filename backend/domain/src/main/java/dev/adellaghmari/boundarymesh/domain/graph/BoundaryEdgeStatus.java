package dev.adellaghmari.boundarymesh.domain.graph;

/** Explicit connection state. Not a trust score or health percentage. */
public enum BoundaryEdgeStatus {
    CONFIGURED,
    ACTIVE,
    BLOCKED,
    QUARANTINED,
    NO_RECENT_ACTIVITY
}
