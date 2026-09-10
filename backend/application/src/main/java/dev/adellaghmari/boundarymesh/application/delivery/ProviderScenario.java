package dev.adellaghmari.boundarymesh.application.delivery;

public enum ProviderScenario {
    SUCCESS,
    STATUS_400,
    STATUS_401,
    STATUS_403,
    STATUS_429,
    STATUS_429_THEN_SUCCESS,
    STATUS_500,
    STATUS_503,
    TIMEOUT,
    CIRCUIT_PREP
}
