package dev.adellaghmari.boundarymesh.application.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import dev.adellaghmari.boundarymesh.application.delivery.DeliveryRequest;
import dev.adellaghmari.boundarymesh.application.delivery.ProviderScenario;
import dev.adellaghmari.boundarymesh.domain.BoundaryOutcome;
import dev.adellaghmari.boundarymesh.domain.TransactionId;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialId;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialReference;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialStatus;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialVersion;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryInstruction;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryInstructionId;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryResultKind;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class OutboxDeliveryPortTest {

    @Test
    void persistsBeforePublishingAndDoesNotClaimProviderDelivery() {
        InMemoryOutboxRepository outbox = new InMemoryOutboxRepository();
        RecordingMessagePublisher messages = new RecordingMessagePublisher();
        Clock clock = Clock.fixed(Instant.parse("2026-09-15T08:00:00Z"), ZoneOffset.UTC);
        OutboxDeliveryPort port = new OutboxDeliveryPort(outbox, messages, clock);

        var result = port.deliver(new DeliveryRequest(instruction(), ProviderScenario.SUCCESS));

        assertThat(result.kind()).isEqualTo(DeliveryResultKind.NOT_ATTEMPTED);
        assertThat(result.providerCalled()).isFalse();
        assertThat(outbox.find("dlv-phase15i")).hasValueSatisfying(record -> {
            assertThat(record.status()).isEqualTo(OutboxStatus.PUBLISHED);
            assertThat(record.createdAt()).isEqualTo(clock.instant());
        });
        assertThat(messages.published()).hasSize(1);
    }

    @Test
    void replayOfPublishedInstructionDoesNotPublishTwice() {
        InMemoryOutboxRepository outbox = new InMemoryOutboxRepository();
        RecordingMessagePublisher messages = new RecordingMessagePublisher();
        OutboxDeliveryPort port = new OutboxDeliveryPort(outbox, messages, Clock.systemUTC());
        DeliveryRequest request = new DeliveryRequest(instruction(), ProviderScenario.SUCCESS);

        port.deliver(request);
        port.deliver(request);

        assertThat(messages.published()).hasSize(1);
    }

    private static DeliveryInstruction instruction() {
        return new DeliveryInstruction(
                new DeliveryInstructionId("dlv-phase15i"),
                TransactionId.of("00000000-0000-4000-8000-000000001501"),
                "quayforge-orders",
                "QUAYFORGE",
                "COVECRM",
                "order-contract:1",
                "egress-v1",
                BoundaryOutcome.ALLOW,
                "{\"synthetic\":true}",
                "corr-phase15i",
                Instant.parse("2026-09-15T08:00:00Z"),
                new CredentialReference(
                        new CredentialId("covecrm-api"), new CredentialVersion("1"), CredentialStatus.ACTIVE),
                "consent-phase15i");
    }
}
