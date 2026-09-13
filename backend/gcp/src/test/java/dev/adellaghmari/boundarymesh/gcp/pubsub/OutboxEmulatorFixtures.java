package dev.adellaghmari.boundarymesh.gcp.pubsub;

import dev.adellaghmari.boundarymesh.application.consent.ConsentEvaluation;
import dev.adellaghmari.boundarymesh.application.crossing.CrossingComposer.CrossingCommand;
import dev.adellaghmari.boundarymesh.application.crossing.CrossingComposer.CrossingResult;
import dev.adellaghmari.boundarymesh.application.delivery.DeliveryInstructionFactory;
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

final class OutboxEmulatorFixtures {

    static final Instant FIXED = Instant.parse("2026-09-14T10:00:00Z");
    static final Clock CLOCK = Clock.fixed(FIXED, ZoneOffset.UTC);

    private OutboxEmulatorFixtures() {}

    static DeliveryInstructionFactory factory() {
        return new DeliveryInstructionFactory(CLOCK);
    }

    static CrossingCommand command() {
        return new CrossingCommand(
                null,
                ControlResult.pass(ControlKind.IDENTITY),
                ControlResult.pass(ControlKind.AUTHENTICITY),
                ControlResult.pass(ControlKind.FRESHNESS),
                ControlResult.pass(ControlKind.REPLAY),
                "quayforge-orders",
                "v1",
                "{\"email\":\"ada.lovelace@example.test\",\"order_id\":\"ord-15ga\"}",
                "COVECRM",
                false,
                TransactionId.of("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"),
                "active",
                "valid",
                "closed");
    }

    static CrossingResult result(BoundaryOutcome outcome, ReasonCode reason, String transformed) {
        ControlVector vector = ControlVector.allPass();
        BoundaryDecision decision = new BoundaryDecision(
                TransactionId.of("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"),
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
