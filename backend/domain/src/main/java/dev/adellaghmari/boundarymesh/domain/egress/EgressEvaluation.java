package dev.adellaghmari.boundarymesh.domain.egress;

import dev.adellaghmari.boundarymesh.domain.ControlResult;
import java.util.List;
import java.util.Objects;

public record EgressEvaluation(
        EgressPolicyId policyId,
        EgressPolicyVersion policyVersion,
        String destination,
        ControlResult control,
        List<FieldDecision> fieldDecisions,
        RedactionSummary summary,
        String transformedJson) {

    public EgressEvaluation {
        Objects.requireNonNull(control, "control");
        fieldDecisions = List.copyOf(fieldDecisions == null ? List.of() : fieldDecisions);
        Objects.requireNonNull(summary, "summary");
    }
}
