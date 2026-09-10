package dev.adellaghmari.boundarymesh.domain.delivery;

public enum DeliveryResultKind {
    DELIVERED,
    DUPLICATE_SUPPRESSED,
    QUARANTINED,
    PERMANENT_FAILURE,
    NOT_ATTEMPTED
}
