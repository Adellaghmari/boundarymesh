package dev.adellaghmari.boundarymesh.application.outbox;

import dev.adellaghmari.boundarymesh.application.delivery.DeliveryIdempotencyStore;
import dev.adellaghmari.boundarymesh.application.delivery.DeliveryPort;
import dev.adellaghmari.boundarymesh.application.delivery.DeliveryRequest;
import dev.adellaghmari.boundarymesh.application.delivery.ProviderScenario;
import dev.adellaghmari.boundarymesh.domain.ReasonCode;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryResult;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryResultKind;
import dev.adellaghmari.boundarymesh.domain.delivery.ProviderFailureCategory;
import java.time.Duration;
import java.util.Objects;

/**
 * Consumes an already-approved instruction. Camel / {@link DeliveryPort} runs after consume.
 * Duplicate instruction ids ACK without a second external attempt.
 */
public final class DeliveryInstructionHandler {

    private final DeliveryPort deliveryPort;
    private final DeliveryIdempotencyStore idempotency;

    public DeliveryInstructionHandler(DeliveryPort deliveryPort, DeliveryIdempotencyStore idempotency) {
        this.deliveryPort = Objects.requireNonNull(deliveryPort, "deliveryPort");
        this.idempotency = Objects.requireNonNull(idempotency, "idempotency");
    }

    public ConsumeDisposition handle(DeliveryInstructionMessage message) {
        if (!valid(message)) {
            return ConsumeDisposition.ACK;
        }
        String instructionId = message.instructionId();
        if (idempotency.previous(instructionId).isPresent()) {
            return ConsumeDisposition.ACK;
        }
        try {
            DeliveryResult result =
                    deliveryPort.deliver(new DeliveryRequest(message.instruction(), ProviderScenario.SUCCESS));
            ConsumeDisposition disposition = disposition(result);
            if (disposition == ConsumeDisposition.ACK) {
                idempotency.remember(instructionId, result);
            }
            return disposition;
        } catch (RuntimeException ex) {
            return ConsumeDisposition.NACK;
        }
    }

    public static ConsumeDisposition disposition(DeliveryResult result) {
        return switch (result.kind()) {
            case DELIVERED, DUPLICATE_SUPPRESSED, PERMANENT_FAILURE, NOT_ATTEMPTED -> ConsumeDisposition.ACK;
            case QUARANTINED ->
                switch (result.providerCategory()) {
                    case RATE_LIMITED, TEMPORARY_FAILURE, TIMEOUT, CIRCUIT_OPEN -> ConsumeDisposition.NACK;
                    default -> ConsumeDisposition.ACK;
                };
        };
    }

    public static DeliveryResult duplicateResult(DeliveryInstructionMessage message) {
        return new DeliveryResult(
                DeliveryResultKind.DUPLICATE_SUPPRESSED,
                0,
                ProviderFailureCategory.DUPLICATE,
                ReasonCode.DELIVERY_DUPLICATE_SUPPRESSED,
                "Idempotent consumer recognized a duplicate delivery instruction.",
                Duration.ZERO,
                message.destinationSystem().toLowerCase(),
                "outbox.consumer",
                "CLOSED",
                null,
                message.instruction().credentialReference(),
                message.instruction().consentId(),
                false);
    }

    static boolean valid(DeliveryInstructionMessage message) {
        if (message == null || message.instruction() == null) {
            return false;
        }
        if (!DeliveryInstructionMessage.SCHEMA_VERSION.equals(message.schemaVersion())) {
            return false;
        }
        String id = message.instructionId();
        return id != null && !id.isBlank();
    }
}
