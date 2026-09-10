package dev.adellaghmari.boundarymesh.domain.delivery;

public enum ProviderFailureCategory {
    NONE,
    SUCCESS,
    PERMANENT_REJECTION,
    CREDENTIAL_OR_CONSENT,
    RATE_LIMITED,
    TEMPORARY_FAILURE,
    TIMEOUT,
    CIRCUIT_OPEN,
    UNKNOWN_DESTINATION,
    DUPLICATE
}
