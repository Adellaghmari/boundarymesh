package dev.adellaghmari.boundarymesh.application.quarantine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.adellaghmari.boundarymesh.domain.ReasonCode;
import dev.adellaghmari.boundarymesh.domain.TransactionId;
import dev.adellaghmari.boundarymesh.domain.exception.InvalidInputException;
import dev.adellaghmari.boundarymesh.domain.quarantine.QuarantineStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QuarantineOrchestratorTest {

    private static final Instant FIXED = Instant.parse("2026-09-10T12:00:00Z");
    private QuarantineOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator =
                new QuarantineOrchestrator(new InMemoryQuarantineRepository(), Clock.fixed(FIXED, ZoneOffset.UTC));
    }

    @Test
    void eligibleReasonCreatesOpenItem() {
        var item = orchestrator.open(
                TransactionId.of("11111111-2222-3333-4444-555555555555"),
                "quayforge-orders",
                "QUAYFORGE",
                "COVECRM",
                ReasonCode.PROVIDER_TEMPORARY_FAILURE,
                "v1",
                "covecrm",
                "covecrm-api:v2",
                "CLOSED");
        assertThat(item.status()).isEqualTo(QuarantineStatus.OPEN);
        assertThat(item.createdAt()).isEqualTo(FIXED);
        assertThat(item.recoveryDecision().name()).isEqualTo("ELIGIBLE");
    }

    @Test
    void reprocessCapBecomesTerminal() {
        var item = orchestrator.open(
                TransactionId.of("11111111-2222-3333-4444-555555555555"),
                "quayforge-orders",
                "QUAYFORGE",
                "COVECRM",
                ReasonCode.DELIVERY_RETRIES_EXHAUSTED,
                "v1",
                "covecrm",
                "covecrm-api:v2",
                "OPEN");
        for (int i = 0; i < 3; i++) {
            orchestrator.beginReprocess(item.id());
            item = orchestrator.remain(item.id(), "Provider still unavailable.");
        }
        assertThat(item.status()).isEqualTo(QuarantineStatus.TERMINAL);
        var terminalId = item.id();
        assertThatThrownBy(() -> orchestrator.beginReprocess(terminalId)).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void resolveRequiresReprocessPath() {
        var item = orchestrator.open(
                TransactionId.of("11111111-2222-3333-4444-555555555555"),
                "quayforge-orders",
                "QUAYFORGE",
                "COVECRM",
                ReasonCode.CONNECTOR_CIRCUIT_OPEN,
                "v1",
                "covecrm",
                "covecrm-api:v2",
                "OPEN");
        orchestrator.beginReprocess(item.id());
        var resolved = orchestrator.resolve(item.id());
        assertThat(resolved.status()).isEqualTo(QuarantineStatus.RESOLVED);
        assertThatThrownBy(() -> orchestrator.beginReprocess(item.id())).isInstanceOf(InvalidInputException.class);
    }
}
