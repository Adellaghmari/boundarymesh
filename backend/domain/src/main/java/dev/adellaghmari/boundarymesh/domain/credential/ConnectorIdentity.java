package dev.adellaghmari.boundarymesh.domain.credential;

import dev.adellaghmari.boundarymesh.domain.exception.InvalidInputException;
import java.util.Objects;
import java.util.regex.Pattern;

public record ConnectorIdentity(String value) {

    private static final Pattern PATTERN = Pattern.compile("^[a-z0-9][a-z0-9-]{1,63}$");

    public ConnectorIdentity {
        Objects.requireNonNull(value, "connector identity");
        String trimmed = value.trim();
        if (!PATTERN.matcher(trimmed).matches()) {
            throw new InvalidInputException(
                    "CONNECTOR_IDENTITY_INVALID", "connector identity must be a lowercase slug");
        }
        value = trimmed;
    }

    @Override
    public String toString() {
        return value;
    }
}
