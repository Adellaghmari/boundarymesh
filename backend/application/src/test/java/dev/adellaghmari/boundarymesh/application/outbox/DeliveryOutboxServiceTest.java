package dev.adellaghmari.boundarymesh.application.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import dev.adellaghmari.boundarymesh.domain.BoundaryOutcome;
import dev.adellaghmari.boundarymesh.domain.ReasonCode;
import org.junit.jupiter.api.Test;

class DeliveryOutboxServiceTest {

    private final InMemoryOutboxRepository outbox = new InMemoryOutboxRepository();
    private final DeliveryOutboxService service =
            new DeliveryOutboxService(OutboxFixtures.factory(), outbox, OutboxFixtures.CLOCK);

    @Test
    void allowCreatesExactlyOnePendingRecord() {
        var record = service.persistApprovedInstruction(
                        OutboxFixtures.result(
                                BoundaryOutcome.ALLOW, ReasonCode.IDENTITY_INVALID, "{\"order_id\":\"ord-1001\"}"),
                        OutboxFixtures.command())
                .orElseThrow();
        assertThat(record.status()).isEqualTo(OutboxStatus.PENDING);
        assertThat(record.instruction().boundaryOutcome()).isEqualTo(BoundaryOutcome.ALLOW);
        assertThat(outbox.pending(10)).hasSize(1);
    }

    @Test
    void allowWithTransformationPersistsApprovedBodyOnly() {
        var record = service.persistApprovedInstruction(
                        OutboxFixtures.result(
                                BoundaryOutcome.ALLOW_WITH_TRANSFORMATION,
                                ReasonCode.EGRESS_REDACTION_REQUIRED,
                                "{\"order_id\":\"ord-1001\",\"email\":\"[REDACTED]\"}"),
                        OutboxFixtures.command())
                .orElseThrow();
        assertThat(record.instruction().boundaryOutcome()).isEqualTo(BoundaryOutcome.ALLOW_WITH_TRANSFORMATION);
        assertThat(record.instruction().payloadJson()).contains("[REDACTED]");
        assertThat(record.instruction().payloadJson()).doesNotContain("ada.lovelace@example.test");
        assertThat(outbox.pending(10)).hasSize(1);
    }

    @Test
    void quarantineCreatesZeroOutboxRecords() {
        assertThat(service.persistApprovedInstruction(
                        OutboxFixtures.result(BoundaryOutcome.QUARANTINE, ReasonCode.CONTRACT_DRIFT_DETECTED, null),
                        OutboxFixtures.command()))
                .isEmpty();
        assertThat(outbox.pending(10)).isEmpty();
    }

    @Test
    void rejectCreatesZeroOutboxRecords() {
        assertThat(service.persistApprovedInstruction(
                        OutboxFixtures.result(BoundaryOutcome.REJECT, ReasonCode.IDENTITY_INVALID, null),
                        OutboxFixtures.command()))
                .isEmpty();
        assertThat(outbox.pending(10)).isEmpty();
    }
}
