package dev.adellaghmari.boundarymesh.gcp.secret;

/** Closed-failure classification. Never includes secret payload. */
public enum SecretAccessFailureKind {
    INVALID_REFERENCE,
    NOT_FOUND,
    PERMISSION_DENIED,
    PROVIDER_UNAVAILABLE
}
