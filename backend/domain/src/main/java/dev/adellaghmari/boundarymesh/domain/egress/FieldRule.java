package dev.adellaghmari.boundarymesh.domain.egress;

import java.util.Objects;

public record FieldRule(String ruleId, FieldPath path, FieldAction action, DenyMode denyMode) {

    public FieldRule {
        Objects.requireNonNull(ruleId, "ruleId");
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(action, "action");
        if (ruleId.isBlank() || ruleId.length() > 64) {
            throw new IllegalArgumentException("rule id must be 1–64 characters");
        }
        if (action == FieldAction.DENY) {
            Objects.requireNonNull(denyMode, "denyMode");
        }
    }
}
