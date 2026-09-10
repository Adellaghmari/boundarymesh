package dev.adellaghmari.boundarymesh.domain.attestation;

import dev.adellaghmari.boundarymesh.domain.exception.InvalidInputException;
import java.util.Objects;

public record AttestationVersion(String value) {

    public static final AttestationVersion V1 = new AttestationVersion("attestation/v1");

    public AttestationVersion {
        Objects.requireNonNull(value, "attestation version");
        value = value.trim();
        if (value.isBlank()) {
            throw new InvalidInputException("ATTESTATION_VERSION_INVALID", "attestation version is required");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
