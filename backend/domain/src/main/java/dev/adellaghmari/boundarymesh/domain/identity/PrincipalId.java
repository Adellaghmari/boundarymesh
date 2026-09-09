package dev.adellaghmari.boundarymesh.domain.identity;

import dev.adellaghmari.boundarymesh.domain.exception.InvalidInputException;
import java.util.Objects;
import java.util.regex.Pattern;

/** Stable principal reference. Never a raw token. */
public record PrincipalId(String value) {

    private static final Pattern VALUE = Pattern.compile("^[A-Za-z0-9:._@/-]{1,128}$");

    public PrincipalId {
        Objects.requireNonNull(value, "value");
        if (!VALUE.matcher(value).matches()) {
            throw new InvalidInputException("PRINCIPAL_ID_INVALID", "principal id must be a safe reference token");
        }
    }

    public static PrincipalId of(String value) {
        return new PrincipalId(value);
    }
}
