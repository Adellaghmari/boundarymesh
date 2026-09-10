package dev.adellaghmari.boundarymesh.domain.consent;

import dev.adellaghmari.boundarymesh.domain.exception.InvalidInputException;
import java.util.Objects;
import java.util.regex.Pattern;

public record Permission(String value) {

    private static final Pattern PATTERN = Pattern.compile("^[A-Za-z0-9._:-]{1,128}$");

    public Permission {
        Objects.requireNonNull(value, "permission");
        String trimmed = value.trim();
        if (!PATTERN.matcher(trimmed).matches()) {
            throw new InvalidInputException("PERMISSION_INVALID", "permission must be a safe token");
        }
        value = trimmed;
    }

    @Override
    public String toString() {
        return value;
    }
}
