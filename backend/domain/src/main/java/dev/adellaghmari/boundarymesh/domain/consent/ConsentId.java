package dev.adellaghmari.boundarymesh.domain.consent;

import dev.adellaghmari.boundarymesh.domain.exception.InvalidInputException;
import java.util.Objects;
import java.util.regex.Pattern;

public record ConsentId(String value) {

    private static final Pattern PATTERN = Pattern.compile("^[a-z0-9][a-z0-9-]{1,63}$");

    public ConsentId {
        Objects.requireNonNull(value, "consent id");
        String trimmed = value.trim();
        if (!PATTERN.matcher(trimmed).matches()) {
            throw new InvalidInputException("CONSENT_ID_INVALID", "consent id must be a lowercase slug");
        }
        value = trimmed;
    }

    @Override
    public String toString() {
        return value;
    }
}
