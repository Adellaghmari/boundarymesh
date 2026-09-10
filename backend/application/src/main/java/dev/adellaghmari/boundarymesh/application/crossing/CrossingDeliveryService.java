package dev.adellaghmari.boundarymesh.application.crossing;

import dev.adellaghmari.boundarymesh.application.crossing.CrossingComposer.CrossingCommand;
import dev.adellaghmari.boundarymesh.application.crossing.CrossingComposer.CrossingResult;
import dev.adellaghmari.boundarymesh.application.delivery.DeliveryInstructionFactory;
import dev.adellaghmari.boundarymesh.application.delivery.DeliveryPort;
import dev.adellaghmari.boundarymesh.application.delivery.DeliveryRequest;
import dev.adellaghmari.boundarymesh.application.delivery.ProviderScenario;
import dev.adellaghmari.boundarymesh.application.quarantine.QuarantineOrchestrator;
import dev.adellaghmari.boundarymesh.application.quarantine.ReprocessPayloadStore;
import dev.adellaghmari.boundarymesh.domain.BoundaryOutcome;
import dev.adellaghmari.boundarymesh.domain.ReasonCode;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryInstruction;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryResult;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryResultKind;
import dev.adellaghmari.boundarymesh.domain.exception.InvariantViolationException;
import dev.adellaghmari.boundarymesh.domain.quarantine.QuarantineItem;
import java.util.Objects;
import java.util.Optional;

public final class CrossingDeliveryService {

    private final CrossingComposer composer;
    private final DeliveryInstructionFactory instructions;
    private final DeliveryPort deliveryPort;
    private final QuarantineOrchestrator quarantine;
    private final ReprocessPayloadStore payloads;

    public CrossingDeliveryService(
            CrossingComposer composer,
            DeliveryInstructionFactory instructions,
            DeliveryPort deliveryPort,
            QuarantineOrchestrator quarantine,
            ReprocessPayloadStore payloads) {
        this.composer = Objects.requireNonNull(composer);
        this.instructions = Objects.requireNonNull(instructions);
        this.deliveryPort = Objects.requireNonNull(deliveryPort);
        this.quarantine = Objects.requireNonNull(quarantine);
        this.payloads = Objects.requireNonNull(payloads);
    }

    public CrossingDeliveryResult execute(CrossingCommand command, ProviderScenario providerScenario) {
        CrossingResult crossing = composer.compose(command);
        BoundaryOutcome outcome = crossing.decision().outcome();
        if (outcome == BoundaryOutcome.REJECT) {
            return new CrossingDeliveryResult(
                    crossing, Optional.empty(), DeliveryResult.notAttempted("REJECT never reaches delivery."), null);
        }
        if (outcome == BoundaryOutcome.QUARANTINE) {
            return new CrossingDeliveryResult(
                    crossing,
                    Optional.empty(),
                    DeliveryResult.notAttempted("QUARANTINE before delivery never reaches Camel."),
                    open(crossing, command, providerScenario, "pre-delivery"));
        }
        DeliveryInstruction instruction = instructions
                .from(crossing, command)
                .orElseThrow(() -> new InvariantViolationException(
                        "DELIVERY_INSTRUCTION_REQUIRED", "ALLOW outcomes must produce a delivery instruction"));
        if (outcome == BoundaryOutcome.ALLOW_WITH_TRANSFORMATION
                && instruction.payloadJson().contains("ada.lovelace@example.test")) {
            throw new InvariantViolationException(
                    "UNREDACTED_PAYLOAD_BLOCKED", "transformed delivery must not contain the original email");
        }
        DeliveryResult delivered = deliveryPort.deliver(new DeliveryRequest(instruction, providerScenario));
        QuarantineItem item = null;
        if (delivered.kind() == DeliveryResultKind.QUARANTINED
                || delivered.kind() == DeliveryResultKind.PERMANENT_FAILURE) {
            item = open(crossing, command, ProviderScenario.SUCCESS, "delivery-failure");
        }
        return new CrossingDeliveryResult(crossing, Optional.of(instruction), delivered, item);
    }

    /**
     * Delivers the same instruction a second time. Used to prove delivery-side idempotency, which is
     * distinct from inbound webhook replay defense.
     */
    public CrossingDeliveryResult executeThenReplay(CrossingCommand command, ProviderScenario providerScenario) {
        CrossingDeliveryResult first = execute(command, providerScenario);
        if (first.instruction().isEmpty()) {
            return first;
        }
        DeliveryResult second =
                deliveryPort.deliver(new DeliveryRequest(first.instruction().get(), providerScenario));
        return new CrossingDeliveryResult(first.crossing(), first.instruction(), second, first.quarantine());
    }

    public CrossingDeliveryResult reprocess(QuarantineItem current, boolean authorizedAdmin) {
        if (!authorizedAdmin) {
            throw new InvariantViolationException("REPROCESS_FORBIDDEN", "reprocess requires boundary.admin");
        }
        QuarantineItem processing = quarantine.beginReprocess(current.id());
        ReprocessPayloadStore.StoredCrossing stored = payloads.find(current.id().value())
                .orElseThrow(() -> new InvariantViolationException(
                        "REPROCESS_CONTEXT_MISSING", "local reprocess context is no longer available"));
        CrossingCommand command =
                CrossingScenarios.delivery("transformed-delivery").withProfiles("active", "valid", "closed");
        CrossingDeliveryResult result = execute(command, ProviderScenario.SUCCESS);
        boolean delivered = result.delivery().kind() == DeliveryResultKind.DELIVERED
                || result.delivery().kind() == DeliveryResultKind.DUPLICATE_SUPPRESSED;
        if (delivered) {
            quarantine.resolve(processing.id());
        } else {
            String note = result.delivery().safeExplanation();
            if (result.crossing().decision().outcome() != BoundaryOutcome.ALLOW
                    && result.crossing().decision().outcome() != BoundaryOutcome.ALLOW_WITH_TRANSFORMATION
                    && !result.crossing().decision().reasonCodes().isEmpty()) {
                note = result.crossing().decision().reasonCodes().getFirst().description();
            }
            quarantine.remain(processing.id(), note);
        }
        return new CrossingDeliveryResult(
                result.crossing(),
                result.instruction(),
                result.delivery(),
                quarantine.find(processing.id()).orElse(processing));
    }

    private QuarantineItem open(
            CrossingResult crossing, CrossingCommand command, ProviderScenario providerScenario, String origin) {
        ReasonCode reason = crossing.decision().reasonCodes().isEmpty()
                ? ReasonCode.CONNECTOR_UNAVAILABLE
                : crossing.decision().reasonCodes().getFirst();
        if ("delivery-failure".equals(origin)) {
            reason = ReasonCode.DELIVERY_RETRIES_EXHAUSTED;
        }
        QuarantineItem item = quarantine.open(
                crossing.decision().transactionId(),
                command.integrationId(),
                "QUAYFORGE",
                command.destination(),
                reason,
                crossing.contract().reference() == null
                        ? null
                        : crossing.contract().reference().version().value(),
                crossing.egress().policyId() == null
                        ? null
                        : crossing.egress().policyId().value(),
                crossing.credentialReference() == null
                        ? null
                        : crossing.credentialReference().display(),
                crossing.vector().connectorSafety().metadataRef());
        payloads.put(
                item.id().value(),
                new ReprocessPayloadStore.StoredCrossing(
                        origin,
                        command.destination(),
                        "active",
                        "valid",
                        "closed",
                        providerScenario == null ? ProviderScenario.SUCCESS.name() : providerScenario.name(),
                        command.payloadJson(),
                        command.contractVersion()));
        return item;
    }

    public record CrossingDeliveryResult(
            CrossingResult crossing,
            Optional<DeliveryInstruction> instruction,
            DeliveryResult delivery,
            QuarantineItem quarantine) {}
}
