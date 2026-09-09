package dev.adellaghmari.boundarymesh.domain.egress;

import dev.adellaghmari.boundarymesh.domain.exception.InvalidInputException;
import java.util.Objects;
import java.util.regex.Pattern;

public record EgressPolicyVersion(String value) {

    private static final Pattern PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{0,31}$");

    public EgressPolicyVersion {
        Objects.requireNonNull(value, "policy version");
        value = value.trim();
        if (!PATTERN.matcher(value).matches()) {
            throw new InvalidInputException("EGRESS_POLICY_VERSION_INVALID", "egress policy version is invalid");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
