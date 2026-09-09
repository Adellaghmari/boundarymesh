package dev.adellaghmari.boundarymesh.domain;

import dev.adellaghmari.boundarymesh.domain.exception.InvalidInputException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Result of one control. Status is more than a boolean. Never holds secrets or raw credentials.
 */
public record ControlResult(
        ControlKind kind,
        ControlStatus status,
        List<ReasonCode> reasonCodes,
        String safeExplanation,
        String metadataRef) {

    public ControlResult {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(reasonCodes, "reasonCodes");
        reasonCodes = List.copyOf(reasonCodes);
        if (safeExplanation != null && safeExplanation.length() > 500) {
            throw new InvalidInputException(
                    "CONTROL_EXPLANATION_INVALID", "safe explanation must be at most 500 characters");
        }
        if (metadataRef != null && metadataRef.length() > 128) {
            throw new InvalidInputException(
                    "CONTROL_METADATA_INVALID", "metadata reference must be at most 128 characters");
        }
        if (status == ControlStatus.FAIL && reasonCodes.isEmpty()) {
            throw new InvalidInputException("CONTROL_REASON_REQUIRED", "FAIL requires at least one reason code");
        }
        if (status == ControlStatus.UNAVAILABLE && reasonCodes.isEmpty()) {
            throw new InvalidInputException("CONTROL_REASON_REQUIRED", "UNAVAILABLE requires at least one reason code");
        }
    }

    public static ControlResult pass(ControlKind kind) {
        return new ControlResult(kind, ControlStatus.PASS, List.of(), "Control passed.", null);
    }

    public static ControlResult pass(ControlKind kind, ReasonCode reason, String safeExplanation) {
        return new ControlResult(kind, ControlStatus.PASS, List.of(reason), safeExplanation, null);
    }

    public static ControlResult fail(ControlKind kind, ReasonCode reason, String safeExplanation) {
        return new ControlResult(kind, ControlStatus.FAIL, List.of(reason), safeExplanation, null);
    }

    public static ControlResult unavailable(ControlKind kind, ReasonCode reason, String safeExplanation) {
        return new ControlResult(kind, ControlStatus.UNAVAILABLE, List.of(reason), safeExplanation, null);
    }

    public static ControlResult notApplicable(ControlKind kind) {
        return new ControlResult(kind, ControlStatus.NOT_APPLICABLE, List.of(), "Control does not apply.", null);
    }

    public Optional<String> metadataReference() {
        return Optional.ofNullable(metadataRef);
    }

    public boolean blocksCrossing() {
        return status == ControlStatus.FAIL || status == ControlStatus.UNAVAILABLE;
    }
}
