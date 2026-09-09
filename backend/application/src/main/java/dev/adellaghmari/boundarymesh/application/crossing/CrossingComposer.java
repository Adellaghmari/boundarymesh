package dev.adellaghmari.boundarymesh.application.crossing;

import dev.adellaghmari.boundarymesh.application.BoundaryEnforcementEngine;
import dev.adellaghmari.boundarymesh.application.contract.ContractEvaluator;
import dev.adellaghmari.boundarymesh.application.egress.EgressEvaluator;
import dev.adellaghmari.boundarymesh.application.identity.IdentityControlFactory;
import dev.adellaghmari.boundarymesh.domain.BoundaryDecision;
import dev.adellaghmari.boundarymesh.domain.ControlKind;
import dev.adellaghmari.boundarymesh.domain.ControlResult;
import dev.adellaghmari.boundarymesh.domain.ControlVector;
import dev.adellaghmari.boundarymesh.domain.EnforcementTrace;
import dev.adellaghmari.boundarymesh.domain.TransactionId;
import dev.adellaghmari.boundarymesh.domain.contract.ContractEvaluation;
import dev.adellaghmari.boundarymesh.domain.egress.EgressEvaluation;
import dev.adellaghmari.boundarymesh.domain.identity.BoundaryPermission;
import dev.adellaghmari.boundarymesh.domain.identity.BoundaryPrincipal;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

public final class CrossingComposer {

    private final BoundaryEnforcementEngine engine;
    private final ContractEvaluator contracts;
    private final EgressEvaluator egress;

    public CrossingComposer(BoundaryEnforcementEngine engine, ContractEvaluator contracts, EgressEvaluator egress) {
        this.engine = Objects.requireNonNull(engine);
        this.contracts = Objects.requireNonNull(contracts);
        this.egress = Objects.requireNonNull(egress);
    }

    public CrossingResult compose(CrossingCommand command) {
        Objects.requireNonNull(command, "command");
        TransactionId transactionId = command.transactionId() == null
                ? TransactionId.of(UUID.randomUUID().toString())
                : command.transactionId();
        ControlResult identity = command.identity();
        ControlResult authorization = command.authorization();
        if (command.principal() != null) {
            identity = IdentityControlFactory.identity(command.principal());
            authorization =
                    IdentityControlFactory.authorization(command.principal(), BoundaryPermission.BOUNDARY_EVALUATE);
        }
        ContractEvaluation contract = command.forceContractUnavailable()
                ? contracts.unavailable("Synthetic contract registry unavailable.")
                : contracts.evaluate(command.integrationId(), command.contractVersion(), command.payloadJson());
        EgressEvaluation egressEvaluation = skipEgress(contract)
                ? skippedEgress(command.destination())
                : egress.evaluate(command.destination(), command.payloadJson());
        ControlVector vector = ControlVector.of(
                identity,
                authorization,
                command.authenticity(),
                command.freshness(),
                command.replay(),
                contract.control(),
                egressEvaluation.control(),
                ControlResult.pass(ControlKind.CREDENTIAL_RESOLVABILITY),
                ControlResult.pass(ControlKind.CONNECTOR_SAFETY));
        BoundaryDecision decision = engine.decide(transactionId, vector);
        return new CrossingResult(decision, vector, contract, egressEvaluation, EnforcementTrace.from(vector));
    }

    public static CrossingCommand webhookAfterSecurity(
            String payloadJson,
            String contractVersion,
            ControlResult authenticity,
            ControlResult freshness,
            ControlResult replay) {
        return new CrossingCommand(
                null,
                new ControlResult(
                        ControlKind.IDENTITY,
                        dev.adellaghmari.boundarymesh.domain.ControlStatus.PASS,
                        java.util.List.of(),
                        "QuayForge connector identity is established by the signed webhook, not a JWT.",
                        null),
                authenticity,
                freshness,
                replay,
                ContractEvaluator.QUAYFORGE_ORDERS,
                contractVersion,
                payloadJson,
                "RIVERTIDE",
                false,
                null);
    }

    private static boolean skipEgress(ContractEvaluation contract) {
        return contract.control().blocksCrossing();
    }

    private static EgressEvaluation skippedEgress(String destination) {
        return new EgressEvaluation(
                new dev.adellaghmari.boundarymesh.domain.egress.EgressPolicyId("skipped"),
                new dev.adellaghmari.boundarymesh.domain.egress.EgressPolicyVersion("none"),
                destination,
                ControlResult.notApplicable(ControlKind.EGRESS),
                java.util.List.of(),
                new dev.adellaghmari.boundarymesh.domain.egress.RedactionSummary(
                        java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(), 0),
                null);
    }

    public record CrossingCommand(
            BoundaryPrincipal principal,
            ControlResult identity,
            ControlResult authenticity,
            ControlResult freshness,
            ControlResult replay,
            String integrationId,
            String contractVersion,
            String payloadJson,
            String destination,
            boolean forceContractUnavailable,
            TransactionId transactionId) {

        public CrossingCommand {
            if (principal == null) {
                Objects.requireNonNull(identity, "identity");
            }
            Objects.requireNonNull(authenticity, "authenticity");
            Objects.requireNonNull(freshness, "freshness");
            Objects.requireNonNull(replay, "replay");
            Objects.requireNonNull(payloadJson, "payloadJson");
            Objects.requireNonNull(destination, "destination");
        }

        public CrossingCommand withPrincipal(BoundaryPrincipal next) {
            return new CrossingCommand(
                    next,
                    identity,
                    authenticity,
                    freshness,
                    replay,
                    integrationId,
                    contractVersion,
                    payloadJson,
                    destination,
                    forceContractUnavailable,
                    transactionId);
        }

        public CrossingCommand withTransaction(TransactionId id) {
            return new CrossingCommand(
                    principal,
                    identity,
                    authenticity,
                    freshness,
                    replay,
                    integrationId,
                    contractVersion,
                    payloadJson,
                    destination,
                    forceContractUnavailable,
                    id);
        }

        public ControlResult authorization() {
            return principal == null
                    ? ControlResult.pass(ControlKind.AUTHORIZATION)
                    : IdentityControlFactory.authorization(principal, BoundaryPermission.BOUNDARY_EVALUATE);
        }
    }

    public record CrossingResult(
            BoundaryDecision decision,
            ControlVector vector,
            ContractEvaluation contract,
            EgressEvaluation egress,
            EnforcementTrace trace) {}

    public static String utf8(byte[] raw) {
        return new String(raw, StandardCharsets.UTF_8);
    }
}
