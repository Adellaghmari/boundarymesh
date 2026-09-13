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

final class OutboxLiveFixtures {

    static final Instant FIXED = Instant.parse("2026-09-14T11:00:00Z");
    static final Clock CLOCK = Clock.fixed(FIXED, ZoneOffset.UTC);

    private OutboxLiveFixtures() {}

    static DeliveryInstructionFactory factory() {
        return new DeliveryInstructionFactory(CLOCK);
    }

    static CrossingCommand command(String runId, String payloadJson) {
        return new CrossingCommand(
                null,
                ControlResult.pass(ControlKind.IDENTITY),
                ControlResult.pass(ControlKind.AUTHENTICITY),
                ControlResult.pass(ControlKind.FRESHNESS),
                ControlResult.pass(ControlKind.REPLAY),
                "livepubsub-" + runId,
                "v1",
                payloadJson,
                "livepubsub-destination-" + runId,
                false,
                TransactionId.random(),
                "active",
                "valid",
                "closed");
    }

    static CrossingResult result(BoundaryOutcome outcome, ReasonCode reason, String transformed) {
        ControlVector vector = ControlVector.allPass();
        BoundaryDecision decision = new BoundaryDecision(
                TransactionId.random(),
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
                        new EgressPolicyId("livepubsub-policy"),
                        new EgressPolicyVersion("v1"),
                        "LIVEPUBSUB",
                        ControlResult.pass(ControlKind.EGRESS),
                        List.of(),
                        new RedactionSummary(List.of(), List.of(), List.of(), List.of(), 1),
                        transformed),
                new ConsentEvaluation(ConsentStatus.GRANTED, null, "ok", null),
                null,
                EnforcementTrace.from(vector));
    }
}
