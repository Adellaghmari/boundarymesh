package dev.adellaghmari.boundarymesh.domain.contract;

import dev.adellaghmari.boundarymesh.domain.exception.InvalidInputException;
import java.util.Objects;
import java.util.regex.Pattern;

public record ContractId(String value) {

    private static final Pattern PATTERN = Pattern.compile("^[a-z0-9][a-z0-9._-]{1,63}$");

    public ContractId {
        Objects.requireNonNull(value, "contract id");
        value = value.trim();
        if (!PATTERN.matcher(value).matches()) {
            throw new InvalidInputException("CONTRACT_ID_INVALID", "contract id must be a lowercase slug");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
