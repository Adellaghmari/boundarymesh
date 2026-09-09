package dev.adellaghmari.boundarymesh.domain.identity;

import dev.adellaghmari.boundarymesh.domain.exception.InvalidInputException;
import java.util.Objects;

public record Issuer(String value) {

    public Issuer {
        Objects.requireNonNull(value, "value");
        if (value.isBlank() || value.length() > 256) {
            throw new InvalidInputException("ISSUER_INVALID", "issuer must be 1–256 characters");
        }
    }

    public static Issuer of(String value) {
        return new Issuer(value);
    }
}
