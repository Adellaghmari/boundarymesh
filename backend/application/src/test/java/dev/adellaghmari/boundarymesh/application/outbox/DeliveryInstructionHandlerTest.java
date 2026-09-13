package dev.adellaghmari.boundarymesh.application.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import dev.adellaghmari.boundarymesh.application.delivery.InMemoryDeliveryIdempotencyStore;
import dev.adellaghmari.boundarymesh.application.delivery.RecordingDeliveryPort;
import dev.adellaghmari.boundarymesh.domain.ReasonCode;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryResult;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryResultKind;
import dev.adellaghmari.boundarymesh.domain.delivery.ProviderFailureCategory;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class DeliveryInstructionHandlerTest {

    @Test
    void successfulDeliveryAcks() {
        RecordingDeliveryPort port = new RecordingDeliveryPort();
        DeliveryInstructionHandler handler =
                new DeliveryInstructionHandler(port, new InMemoryDeliveryIdempotencyStore());
        assertThat(handler.handle(DeliveryInstructionMessage.of(OutboxFixtures.allowInstruction())))
                .isEqualTo(ConsumeDisposition.ACK);
        assertThat(port.invocationCount()).isEqualTo(1);
    }

    @Test
    void duplicateInstructionAcksWithoutSecondExternalCallAfterStoreHit() {
        RecordingDeliveryPort port = new RecordingDeliveryPort();
        InMemoryDeliveryIdempotencyStore store = new InMemoryDeliveryIdempotencyStore();
        DeliveryInstructionHandler handler = new DeliveryInstructionHandler(port, store);
        DeliveryInstructionMessage message = DeliveryInstructionMessage.of(OutboxFixtures.allowInstruction());
        assertThat(handler.handle(message)).isEqualTo(ConsumeDisposition.ACK);
        assertThat(handler.handle(message)).isEqualTo(ConsumeDisposition.ACK);
        assertThat(port.invocationCount()).isEqualTo(1);
    }

    @Test
    void malformedMessageAcksWithoutDelivery() {
        RecordingDeliveryPort port = new RecordingDeliveryPort();
        DeliveryInstructionHandler handler =
                new DeliveryInstructionHandler(port, new InMemoryDeliveryIdempotencyStore());
        assertThat(handler.handle(
                        new DeliveryInstructionMessage("delivery-instruction/v0", OutboxFixtures.allowInstruction())))
                .isEqualTo(ConsumeDisposition.ACK);
        assertThat(port.invocationCount()).isZero();
    }

    @Test
    void transientDeliveryNacks() {
        RecordingDeliveryPort port = new RecordingDeliveryPort();
        port.succeedWith(new DeliveryResult(
                DeliveryResultKind.QUARANTINED,
                1,
                ProviderFailureCategory.TEMPORARY_FAILURE,
                ReasonCode.PROVIDER_TEMPORARY_FAILURE,
                "fake-provider-unavailable",
                Duration.ZERO,
                "covecrm",
                "recording.delivery",
                "CLOSED",
                503,
                null,
                null,
                true));
        DeliveryInstructionHandler handler =
                new DeliveryInstructionHandler(port, new InMemoryDeliveryIdempotencyStore());
        assertThat(handler.handle(DeliveryInstructionMessage.of(OutboxFixtures.allowInstruction())))
                .isEqualTo(ConsumeDisposition.NACK);
    }

    @Test
    void unexpectedInfrastructureFailureNacks() {
        DeliveryInstructionHandler handler = new DeliveryInstructionHandler(
                request -> {
                    throw new IllegalStateException("fake-worker-unavailable");
                },
                new InMemoryDeliveryIdempotencyStore());
        assertThat(handler.handle(DeliveryInstructionMessage.of(OutboxFixtures.allowInstruction())))
                .isEqualTo(ConsumeDisposition.NACK);
    }
}
