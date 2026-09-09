package dev.adellaghmari.boundarymesh.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.adellaghmari.boundarymesh.domain.exception.InvalidInputException;
import dev.adellaghmari.boundarymesh.domain.exception.InvariantViolationException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BoundaryDecisionTest {

    private static final TransactionId TX = TransactionId.of("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    @Test
    void allowMayHaveEmptyReasons() {
        BoundaryDecision decision = new BoundaryDecision(
                TX,
                BoundaryOutcome.ALLOW,
                List.of(),
                BoundaryDecision.summaryOf(ControlVector.allPass()),
                Instant.parse("2026-09-09T12:00:00Z"),
                PolicyVersion.PHASE_1);
        assertThat(decision.outcome()).isEqualTo(BoundaryOutcome.ALLOW);
        assertThat(decision.reasonCodes()).isEmpty();
    }

    @Test
    void rejectRequiresReasonCode() {
        assertThatThrownBy(() -> new BoundaryDecision(
                        TX,
                        BoundaryOutcome.REJECT,
                        List.of(),
                        BoundaryDecision.summaryOf(ControlVector.allPass()),
                        Instant.parse("2026-09-09T12:00:00Z"),
                        PolicyVersion.PHASE_1))
                .isInstanceOf(InvalidInputException.class)
                .hasFieldOrPropertyWithValue("errorCode", "DECISION_REASON_REQUIRED");
    }

    @Test
    void appliedControlsMustCoverEveryKind() {
        assertThatThrownBy(() -> new BoundaryDecision(
                        TX,
                        BoundaryOutcome.ALLOW,
                        List.of(),
                        Map.of(ControlKind.IDENTITY, ControlStatus.PASS),
                        Instant.parse("2026-09-09T12:00:00Z"),
                        PolicyVersion.PHASE_1))
                .isInstanceOf(InvalidInputException.class)
                .hasFieldOrPropertyWithValue("errorCode", "DECISION_CONTROLS_INCOMPLETE");
    }

    @Test
    void transactionLifecycleReceivedToRejected() {
        Instant now = Instant.parse("2026-09-09T12:00:00Z");
        BoundaryTransaction received = BoundaryTransaction.received(
                TX, IntegrationId.of("helix-calendar-crm"), SourceSystem.HELIX_365, DestinationSystem.COVECRM, now);
        BoundaryTransaction rejected = received.apply(BoundaryOutcome.REJECT);
        assertThat(rejected.state()).isEqualTo(TransactionState.REJECTED);
        assertThatThrownBy(() -> rejected.apply(BoundaryOutcome.ALLOW)).isInstanceOf(InvariantViolationException.class);
    }
}
