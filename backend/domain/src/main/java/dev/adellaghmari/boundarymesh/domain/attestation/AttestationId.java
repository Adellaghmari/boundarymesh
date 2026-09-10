package dev.adellaghmari.boundarymesh.domain.attestation;

import java.util.Objects;
import java.util.UUID;

public record AttestationId(String value) {

    public AttestationId {
        Objects.requireNonNull(value, "attestation id");
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("attestation id");
        }
    }

    public static AttestationId random() {
        return new AttestationId(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return value;
    }
}
