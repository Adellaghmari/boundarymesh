package dev.adellaghmari.boundarymesh.api.security;

public enum TokenScenario {
    VALID_MACHINE,
    EXPIRED,
    WRONG_ISSUER,
    WRONG_AUDIENCE,
    UNKNOWN_KID,
    INVALID_SIGNATURE,
    MISSING_PERMISSION,
    FUTURE_NBF,
    VALID_ADMIN
}
