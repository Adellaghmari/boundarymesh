package dev.adellaghmari.boundarymesh.domain;

import dev.adellaghmari.boundarymesh.domain.exception.InvalidInputException;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Result of evaluating whether a transaction may cross a system boundary.
 */
public record BoundaryDecision(
        TransactionId transactionId,
        BoundaryOutcome outcome,
        List<ReasonCode> reasonCodes,
        Map<ControlKind, ControlStatus> appliedControls,
        Instant decidedAt,
        PolicyVersion policyVersion) {

    public BoundaryDecision {
        Objects.requireNonNull(transactionId, "transactionId");
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(reasonCodes, "reasonCodes");
        Objects.requireNonNull(appliedControls, "appliedControls");
        Objects.requireNonNull(decidedAt, "decidedAt");
        Objects.requireNonNull(policyVersion, "policyVersion");
        reasonCodes = List.copyOf(reasonCodes);
        appliedControls = Map.copyOf(appliedControls);
        if (outcome != BoundaryOutcome.ALLOW && reasonCodes.isEmpty()) {
            throw new InvalidInputException(
                    "DECISION_REASON_REQUIRED", "non-ALLOW decisions require at least one reason code");
        }
        if (appliedControls.size() != ControlKind.values().length) {
            throw new InvalidInputException(
                    "DECISION_CONTROLS_INCOMPLETE", "applied controls must include every ControlKind");
        }
    }

    public static Map<ControlKind, ControlStatus> summaryOf(ControlVector vector) {
        EnumMap<ControlKind, ControlStatus> map = new EnumMap<>(ControlKind.class);
        for (ControlResult result : vector.asList()) {
            map.put(result.kind(), result.status());
        }
        return Map.copyOf(map);
    }
}
