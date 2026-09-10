package dev.adellaghmari.boundarymesh.domain.credential;

import dev.adellaghmari.boundarymesh.domain.exception.InvalidInputException;
import java.util.Objects;
import java.util.regex.Pattern;

public record CredentialVersion(String value) {

    private static final Pattern PATTERN = Pattern.compile("^[0-9]{1,8}$");

    public CredentialVersion {
        Objects.requireNonNull(value, "credential version");
        String trimmed = value.trim();
        if (!PATTERN.matcher(trimmed).matches()) {
            throw new InvalidInputException("CREDENTIAL_VERSION_INVALID", "credential version must be a numeric id");
        }
        value = trimmed;
    }

    @Override
    public String toString() {
        return value;
    }
}
