package dev.adellaghmari.boundarymesh.application.crossing;

import dev.adellaghmari.boundarymesh.application.BoundaryEnforcementEngine;
import dev.adellaghmari.boundarymesh.application.connector.ConnectorSafetyEvaluator;
import dev.adellaghmari.boundarymesh.application.connector.LocalConnectorSafetyPort;
import dev.adellaghmari.boundarymesh.application.consent.ConsentEvaluation;
import dev.adellaghmari.boundarymesh.application.consent.ConsentEvaluator;
import dev.adellaghmari.boundarymesh.application.consent.InMemoryConsentCatalog;
import dev.adellaghmari.boundarymesh.application.contract.ContractEvaluator;
import dev.adellaghmari.boundarymesh.application.credential.CredentialEvaluator;
import dev.adellaghmari.boundarymesh.application.credential.InMemoryCredentialCatalog;
import dev.adellaghmari.boundarymesh.application.egress.EgressEvaluator;
import dev.adellaghmari.boundarymesh.application.identity.IdentityControlFactory;
import dev.adellaghmari.boundarymesh.domain.BoundaryDecision;
import dev.adellaghmari.boundarymesh.domain.BoundaryOutcome;
import dev.adellaghmari.boundarymesh.domain.ControlKind;
import dev.adellaghmari.boundarymesh.domain.ControlResult;
import dev.adellaghmari.boundarymesh.domain.ControlVector;
import dev.adellaghmari.boundarymesh.domain.EnforcementTrace;
import dev.adellaghmari.boundarymesh.domain.PolicyVersion;
import dev.adellaghmari.boundarymesh.domain.TransactionId;
import dev.adellaghmari.boundarymesh.domain.consent.ConnectorOperation;
import dev.adellaghmari.boundarymesh.domain.contract.ContractEvaluation;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialReference;
import dev.adellaghmari.boundarymesh.domain.egress.EgressEvaluation;
import dev.adellaghmari.boundarymesh.domain.identity.BoundaryPermission;
import dev.adellaghmari.boundarymesh.domain.identity.BoundaryPrincipal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class CrossingComposer {

    private final BoundaryEnforcementEngine engine;
    private final ContractEvaluator contracts;
    private final EgressEvaluator egress;
    private final CredentialEvaluator credentials;
    private final ConsentEvaluator consents;
    private final ConnectorSafetyEvaluator connectors;
    private final Clock clock;

    public CrossingComposer(BoundaryEnforcementEngine engine, ContractEvaluator contracts, EgressEvaluator egress) {
        this(
                engine,
                contracts,
                egress,
                new CredentialEvaluator(
                        InMemoryCredentialCatalog.veloraDefaults(Instant.parse("2026-09-10T00:00:00Z"))),
                new ConsentEvaluator(InMemoryConsentCatalog.veloraDefaults()),
                new ConnectorSafetyEvaluator(new LocalConnectorSafetyPort()),
                Clock.fixed(Instant.parse("2026-09-10T00:00:00Z"), ZoneOffset.UTC));
    }

    public CrossingComposer(
            BoundaryEnforcementEngine engine,
            ContractEvaluator contracts,
            EgressEvaluator egress,
            CredentialEvaluator credentials,
            ConsentEvaluator consents,
            ConnectorSafetyEvaluator connectors,
            Clock clock) {
        this.engine = Objects.requireNonNull(engine);
        this.contracts = Objects.requireNonNull(contracts);
        this.egress = Objects.requireNonNull(egress);
        this.credentials = Objects.requireNonNull(credentials);
        this.consents = Objects.requireNonNull(consents);
        this.connectors = Objects.requireNonNull(connectors);
        this.clock = Objects.requireNonNull(clock);
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
        ControlResult credential = credentials.evaluate(command.destination(), command.credentialScenario());
        ConsentEvaluation consent = consents.evaluate(
                command.destination(), operationFor(command.destination()), command.consentScenario());
        ControlResult connector = connectors.evaluate(command.destination(), command.connectorScenario());
        ControlVector vector = ControlVector.of(
                identity,
                authorization,
                command.authenticity(),
                command.freshness(),
                command.replay(),
                contract.control(),
                egressEvaluation.control(),
                credential,
                connector);
        BoundaryDecision decision = applyConsent(engine.decide(transactionId, vector), consent, vector);
        return new CrossingResult(
                decision,
                vector,
                contract,
                egressEvaluation,
                consent,
                credentials
                        .reference(command.destination())
                        .map(item -> item.reference())
                        .orElse(null),
                EnforcementTrace.from(vector));
    }

    private BoundaryDecision applyConsent(BoundaryDecision decision, ConsentEvaluation consent, ControlVector vector) {
        if (!consent.blocksCrossing()) {
            return decision;
        }
        if (decision.outcome() == BoundaryOutcome.REJECT) {
            return decision;
        }
        if (decision.outcome() == BoundaryOutcome.QUARANTINE) {
            return decision;
        }
        return new BoundaryDecision(
                decision.transactionId(),
                BoundaryOutcome.QUARANTINE,
                List.of(consent.reason()),
                BoundaryDecision.summaryOf(vector),
                clock.instant(),
                decision.policyVersion() == null ? PolicyVersion.PHASE_10 : decision.policyVersion());
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
                null,
                "active",
                "valid",
                "closed");
    }

    public static ConnectorOperation operationFor(String destination) {
        return switch (destination == null ? "" : destination.trim().toUpperCase(Locale.ROOT)) {
            case "COVECRM" -> ConnectorOperation.ACTIVITY_WRITE;
            case "LUMENWAY" -> ConnectorOperation.FULFILLMENT_CREATE;
            default -> ConnectorOperation.SETTLEMENT_WRITE;
        };
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
            TransactionId transactionId,
            String credentialScenario,
            String consentScenario,
            String connectorScenario) {

        public CrossingCommand {
            if (principal == null) {
                Objects.requireNonNull(identity, "identity");
            }
            Objects.requireNonNull(authenticity, "authenticity");
            Objects.requireNonNull(freshness, "freshness");
            Objects.requireNonNull(replay, "replay");
            Objects.requireNonNull(payloadJson, "payloadJson");
            Objects.requireNonNull(destination, "destination");
            credentialScenario = credentialScenario == null ? "active" : credentialScenario;
            consentScenario = consentScenario == null ? "valid" : consentScenario;
            connectorScenario = connectorScenario == null ? "closed" : connectorScenario;
        }

        public CrossingCommand withPrincipal(BoundaryPrincipal next) {
            return copy(next, identity, authenticity, freshness, replay, transactionId);
        }

        public CrossingCommand withTransaction(TransactionId id) {
            return copy(principal, identity, authenticity, freshness, replay, id);
        }

        public CrossingCommand withProfiles(String credential, String consent, String connector) {
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
                    transactionId,
                    credential,
                    consent,
                    connector);
        }

        public CrossingCommand withReplay(ControlResult nextReplay) {
            return copy(principal, identity, authenticity, freshness, nextReplay, transactionId);
        }

        private CrossingCommand copy(
                BoundaryPrincipal nextPrincipal,
                ControlResult nextIdentity,
                ControlResult nextAuthenticity,
                ControlResult nextFreshness,
                ControlResult nextReplay,
                TransactionId id) {
            return new CrossingCommand(
                    nextPrincipal,
                    nextIdentity,
                    nextAuthenticity,
                    nextFreshness,
                    nextReplay,
                    integrationId,
                    contractVersion,
                    payloadJson,
                    destination,
                    forceContractUnavailable,
                    id,
                    credentialScenario,
                    consentScenario,
                    connectorScenario);
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
            ConsentEvaluation consent,
            CredentialReference credentialReference,
            EnforcementTrace trace) {}

    public static String utf8(byte[] raw) {
        return new String(raw, StandardCharsets.UTF_8);
    }
}
