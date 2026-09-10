package dev.adellaghmari.boundarymesh.application.delivery;

import static org.assertj.core.api.Assertions.assertThat;

import dev.adellaghmari.boundarymesh.application.consent.ConsentEvaluation;
import dev.adellaghmari.boundarymesh.application.crossing.CrossingComposer.CrossingCommand;
import dev.adellaghmari.boundarymesh.application.crossing.CrossingComposer.CrossingResult;
import dev.adellaghmari.boundarymesh.domain.BoundaryDecision;
import dev.adellaghmari.boundarymesh.domain.BoundaryOutcome;
import dev.adellaghmari.boundarymesh.domain.ControlKind;
import dev.adellaghmari.boundarymesh.domain.ControlResult;
import dev.adellaghmari.boundarymesh.domain.ControlVector;
import dev.adellaghmari.boundarymesh.domain.EnforcementTrace;
import dev.adellaghmari.boundarymesh.domain.PolicyVersion;
import dev.adellaghmari.boundarymesh.domain.ReasonCode;
import dev.adellaghmari.boundarymesh.domain.TransactionId;
import dev.adellaghmari.boundarymesh.domain.consent.ConsentStatus;
import dev.adellaghmari.boundarymesh.domain.contract.ContractEvaluation;
import dev.adellaghmari.boundarymesh.domain.egress.EgressEvaluation;
import dev.adellaghmari.boundarymesh.domain.egress.EgressPolicyId;
import dev.adellaghmari.boundarymesh.domain.egress.EgressPolicyVersion;
import dev.adellaghmari.boundarymesh.domain.egress.RedactionSummary;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class DeliveryInstructionFactoryTest {

    private static final Instant FIXED = Instant.parse("2026-09-10T12:00:00Z");

    @Test
    void rejectDoesNotProduceInstruction() {
        var factory = new DeliveryInstructionFactory(Clock.fixed(FIXED, ZoneOffset.UTC));
        assertThat(factory.from(result(BoundaryOutcome.REJECT, ReasonCode.IDENTITY_INVALID, null), command()))
                .isEmpty();
    }

    @Test
    void transformUsesApprovedBodyNotSource() {
        var factory = new DeliveryInstructionFactory(Clock.fixed(FIXED, ZoneOffset.UTC));
        var instruction = factory.from(
                        result(
                                BoundaryOutcome.ALLOW_WITH_TRANSFORMATION,
                                ReasonCode.EGRESS_REDACTION_REQUIRED,
                                "{\"order_id\":\"ord-1001\",\"email\":\"[REDACTED]\"}"),
                        command())
                .orElseThrow();
        assertThat(instruction.payloadJson()).contains("[REDACTED]");
        assertThat(instruction.payloadJson()).doesNotContain("ada.lovelace@example.test");
        assertThat(instruction.boundaryOutcome()).isEqualTo(BoundaryOutcome.ALLOW_WITH_TRANSFORMATION);
    }

    private static CrossingCommand command() {
        return new CrossingCommand(
                null,
                ControlResult.pass(ControlKind.IDENTITY),
                ControlResult.pass(ControlKind.AUTHENTICITY),
                ControlResult.pass(ControlKind.FRESHNESS),
                ControlResult.pass(ControlKind.REPLAY),
                "quayforge-orders",
                "v1",
                "{\"email\":\"ada.lovelace@example.test\",\"order_id\":\"ord-1001\"}",
                "COVECRM",
                false,
                TransactionId.of("11111111-2222-3333-4444-555555555555"),
                "active",
                "valid",
                "closed");
    }

    private static CrossingResult result(BoundaryOutcome outcome, ReasonCode reason, String transformed) {
        ControlVector vector = ControlVector.allPass();
        BoundaryDecision decision = new BoundaryDecision(
                TransactionId.of("11111111-2222-3333-4444-555555555555"),
                outcome,
                outcome == BoundaryOutcome.ALLOW ? List.of() : List.of(reason),
                BoundaryDecision.summaryOf(vector),
                FIXED,
                PolicyVersion.PHASE_10);
        return new CrossingResult(
                decision,
                vector,
                new ContractEvaluation(null, ControlResult.pass(ControlKind.CONTRACT), List.of(), 0, "VALID"),
                new EgressEvaluation(
                        new EgressPolicyId("covecrm-policy"),
                        new EgressPolicyVersion("v1"),
                        "COVECRM",
                        ControlResult.pass(ControlKind.EGRESS),
                        List.of(),
                        new RedactionSummary(List.of(), List.of(), List.of(), List.of(), 1),
                        transformed),
                new ConsentEvaluation(ConsentStatus.GRANTED, null, "ok", null),
                null,
                EnforcementTrace.from(vector));
    }
}
