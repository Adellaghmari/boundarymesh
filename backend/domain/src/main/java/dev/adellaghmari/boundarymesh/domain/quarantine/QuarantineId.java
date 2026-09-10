package dev.adellaghmari.boundarymesh.domain.quarantine;

import java.util.Objects;
import java.util.UUID;

public record QuarantineId(String value) {

    public QuarantineId {
        Objects.requireNonNull(value, "quarantine id");
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("quarantine id");
        }
    }

    public static QuarantineId random() {
        return new QuarantineId(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return value;
    }
}
