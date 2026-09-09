package dev.adellaghmari.boundarymesh.domain.contract;

import dev.adellaghmari.boundarymesh.domain.exception.InvalidInputException;
import java.util.Objects;
import java.util.regex.Pattern;

public record ContractVersion(String value) {

    private static final Pattern PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{0,31}$");

    public ContractVersion {
        Objects.requireNonNull(value, "contract version");
        value = value.trim();
        if (!PATTERN.matcher(value).matches()) {
            throw new InvalidInputException("CONTRACT_VERSION_INVALID", "contract version must be a short token");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
