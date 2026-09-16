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
    void terminalQuarantineAcksAfterDurableIdempotencyRecord() {
        RecordingDeliveryPort port = new RecordingDeliveryPort();
        port.succeedWith(new DeliveryResult(
                DeliveryResultKind.QUARANTINED,
                0,
                ProviderFailureCategory.CREDENTIAL_OR_CONSENT,
                ReasonCode.CONNECTOR_UNAVAILABLE,
                "connector endpoint is not configured",
                Duration.ZERO,
                "covecrm",
                "sink.covecrm.activity",
                "CLOSED",
                null,
                null,
                null,
                false));
        InMemoryDeliveryIdempotencyStore store = new InMemoryDeliveryIdempotencyStore();
        DeliveryInstructionHandler handler = new DeliveryInstructionHandler(port, store);
        DeliveryInstructionMessage message = DeliveryInstructionMessage.of(OutboxFixtures.allowInstruction());

        assertThat(handler.handle(message)).isEqualTo(ConsumeDisposition.ACK);
        assertThat(store.previous(message.instructionId())).isPresent();
        assertThat(handler.handle(message)).isEqualTo(ConsumeDisposition.ACK);
        assertThat(port.invocationCount()).isEqualTo(1);
    }

    @Test
    void transientDeliveryNacksWithoutDurableTerminalRecord() {
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
    void quarantinePersistenceFailureNacksInsteadOfFalselyAcknowledging() {
        RecordingDeliveryPort port = new RecordingDeliveryPort();
        port.succeedWith(new DeliveryResult(
                DeliveryResultKind.QUARANTINED,
                0,
                ProviderFailureCategory.CREDENTIAL_OR_CONSENT,
                ReasonCode.CONNECTOR_UNAVAILABLE,
                "connector endpoint is not configured",
                Duration.ZERO,
                "covecrm",
                "sink.covecrm.activity",
                "CLOSED",
                null,
                null,
                null,
                false));
        DeliveryInstructionHandler handler = new DeliveryInstructionHandler(port, new FailingIdempotencyStore());
        assertThat(handler.handle(DeliveryInstructionMessage.of(OutboxFixtures.allowInstruction())))
                .isEqualTo(ConsumeDisposition.NACK);
    }

    private static final class FailingIdempotencyStore
            implements dev.adellaghmari.boundarymesh.application.delivery.DeliveryIdempotencyStore {
        @Override
        public java.util.Optional<DeliveryResult> previous(String deliveryInstructionId) {
            return java.util.Optional.empty();
        }

        @Override
        public void remember(String deliveryInstructionId, DeliveryResult result) {
            throw new IllegalStateException("persistence unavailable");
        }

        @Override
        public void clear() {}
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
