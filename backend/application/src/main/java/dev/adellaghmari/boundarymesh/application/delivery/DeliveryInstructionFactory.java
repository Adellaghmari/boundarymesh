package dev.adellaghmari.boundarymesh.application.delivery;

import dev.adellaghmari.boundarymesh.application.crossing.CrossingComposer.CrossingCommand;
import dev.adellaghmari.boundarymesh.application.crossing.CrossingComposer.CrossingResult;
import dev.adellaghmari.boundarymesh.domain.BoundaryOutcome;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryInstruction;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryInstructionId;
import dev.adellaghmari.boundarymesh.domain.exception.InvariantViolationException;
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class DeliveryInstructionFactory {

    private final Clock clock;

    public DeliveryInstructionFactory(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public Optional<DeliveryInstruction> from(CrossingResult result, CrossingCommand command) {
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(command, "command");
        BoundaryOutcome outcome = result.decision().outcome();
        if (outcome != BoundaryOutcome.ALLOW && outcome != BoundaryOutcome.ALLOW_WITH_TRANSFORMATION) {
            return Optional.empty();
        }
        String payload = approvedPayload(result, command, outcome);
        String eventId = "dlv-" + UUID.randomUUID();
        return Optional.of(new DeliveryInstruction(
                new DeliveryInstructionId(eventId),
                result.decision().transactionId(),
                command.integrationId() == null ? "quayforge-orders" : command.integrationId(),
                "QUAYFORGE",
                command.destination(),
                result.contract().reference() == null
                        ? null
                        : result.contract().reference().id().value() + ":"
                                + result.contract().reference().version().value(),
                result.egress().policyId() == null
                        ? null
                        : result.egress().policyId().value(),
                outcome,
                payload,
                result.decision().transactionId().value(),
                clock.instant(),
                result.credentialReference(),
                result.consent() == null || result.consent().grant() == null
                        ? null
                        : result.consent().grant().id().value()));
    }

    private static String approvedPayload(CrossingResult result, CrossingCommand command, BoundaryOutcome outcome) {
        if (outcome == BoundaryOutcome.ALLOW_WITH_TRANSFORMATION) {
            String transformed = result.egress().transformedJson();
            if (transformed == null || transformed.isBlank()) {
                throw new InvariantViolationException(
                        "TRANSFORMED_PAYLOAD_REQUIRED", "transformation outcome requires an approved egress body");
            }
            if (transformed.equals(command.payloadJson())
                    && command.payloadJson().contains("ada.lovelace@example.test")) {
                throw new InvariantViolationException(
                        "UNREDACTED_PAYLOAD_BLOCKED",
                        "original sensitive payload must not be delivered after redaction");
            }
            return transformed;
        }
        String transformed = result.egress().transformedJson();
        return transformed == null || transformed.isBlank() ? command.payloadJson() : transformed;
    }
}
