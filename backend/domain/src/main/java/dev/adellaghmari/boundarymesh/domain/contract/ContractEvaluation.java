package dev.adellaghmari.boundarymesh.domain.contract;

import dev.adellaghmari.boundarymesh.domain.ControlResult;
import java.util.List;
import java.util.Objects;

public record ContractEvaluation(
        ContractReference reference,
        ControlResult control,
        List<ValidationViolation> violations,
        int truncatedCount,
        String classification) {

    public static final int MAX_VIOLATIONS = 20;

    public ContractEvaluation {
        Objects.requireNonNull(control, "control");
        violations = List.copyOf(violations == null ? List.of() : violations);
        classification = classification == null ? "NONE" : classification;
        if (violations.size() > MAX_VIOLATIONS) {
            throw new IllegalArgumentException("violations must already be truncated");
        }
    }
}
