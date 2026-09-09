package dev.adellaghmari.boundarymesh.domain.egress;

import java.util.Objects;

public record FieldDecision(FieldPath path, FieldAction action, String ruleId, String note) {

    public FieldDecision {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(action, "action");
        ruleId = ruleId == null ? "" : ruleId;
        note = note == null ? "" : note;
    }
}
