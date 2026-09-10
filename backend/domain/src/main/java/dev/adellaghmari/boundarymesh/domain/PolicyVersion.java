package dev.adellaghmari.boundarymesh.domain;

import dev.adellaghmari.boundarymesh.domain.exception.InvalidInputException;
import java.util.Objects;
import java.util.regex.Pattern;

public record PolicyVersion(String value) {

    private static final Pattern PATTERN = Pattern.compile("^[a-z0-9][a-z0-9._-]{0,63}$");

    public static final PolicyVersion PHASE_1 = new PolicyVersion("boundary-enforcement.phase1.v1");
    public static final PolicyVersion PHASE_7 = new PolicyVersion("boundary-enforcement.phase7.v1");
    public static final PolicyVersion PHASE_10 = new PolicyVersion("boundary-enforcement.phase10.v1");

    public PolicyVersion {
        Objects.requireNonNull(value, "policy version");
        String trimmed = value.trim();
        if (!PATTERN.matcher(trimmed).matches()) {
            throw new InvalidInputException("POLICY_VERSION_INVALID", "policy version must be a lowercase slug");
        }
        value = trimmed;
    }

    @Override
    public String toString() {
        return value;
    }
}
