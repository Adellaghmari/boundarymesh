package dev.adellaghmari.boundarymesh.domain.egress;

import dev.adellaghmari.boundarymesh.domain.exception.InvalidInputException;
import java.util.Objects;
import java.util.regex.Pattern;

public record EgressPolicyId(String value) {

    private static final Pattern PATTERN = Pattern.compile("^[a-z0-9][a-z0-9._-]{1,63}$");

    public EgressPolicyId {
        Objects.requireNonNull(value, "policy id");
        value = value.trim();
        if (!PATTERN.matcher(value).matches()) {
            throw new InvalidInputException("EGRESS_POLICY_ID_INVALID", "egress policy id must be a lowercase slug");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
