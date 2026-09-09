package dev.adellaghmari.boundarymesh.domain;

import java.util.List;
import java.util.Objects;

/** Ordered deterministic control evidence. Not reasoning and not chain-of-thought. */
public record EnforcementTrace(List<Step> steps) {

    public EnforcementTrace {
        steps = List.copyOf(Objects.requireNonNull(steps, "steps"));
    }

    public record Step(ControlKind kind, ControlStatus status, String explanation, String reference) {

        public Step {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(status, "status");
            explanation = explanation == null ? "" : explanation;
            reference = reference == null ? "" : reference;
        }
    }

    public static EnforcementTrace from(ControlVector vector) {
        return new EnforcementTrace(vector.asList().stream()
                .map(result -> new Step(
                        result.kind(),
                        result.status(),
                        result.safeExplanation(),
                        result.metadataRef() == null ? "" : result.metadataRef()))
                .toList());
    }
}
