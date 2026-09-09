package dev.adellaghmari.boundarymesh.domain;

import dev.adellaghmari.boundarymesh.domain.exception.InvalidInputException;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Identifier for a configured source→destination integration.
 */
public record IntegrationId(String value) {

    private static final Pattern PATTERN = Pattern.compile("^[a-z][a-z0-9-]{0,62}$");

    public IntegrationId {
        Objects.requireNonNull(value, "integration id");
        String trimmed = value.trim();
        if (!PATTERN.matcher(trimmed).matches()) {
            throw new InvalidInputException(
                    "INTEGRATION_ID_INVALID", "integration id must be a lowercase slug of 1–63 characters");
        }
        value = trimmed;
    }

    public static IntegrationId of(String value) {
        return new IntegrationId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
