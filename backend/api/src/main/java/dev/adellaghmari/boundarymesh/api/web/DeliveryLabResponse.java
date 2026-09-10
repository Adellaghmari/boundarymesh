package dev.adellaghmari.boundarymesh.api.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.adellaghmari.boundarymesh.application.crossing.CrossingDeliveryService.CrossingDeliveryResult;
import dev.adellaghmari.boundarymesh.domain.IngressKind;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryInstruction;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryResult;
import dev.adellaghmari.boundarymesh.domain.identity.BoundaryPrincipal;
import dev.adellaghmari.boundarymesh.domain.quarantine.QuarantineItem;
import java.util.List;

public record DeliveryLabResponse(
        String banner,
        DecisionResponse decision,
        InstructionView instruction,
        DeliveryView delivery,
        CredentialView credential,
        ConsentView consent,
        QuarantineView quarantine,
        EvidenceView evidence,
        List<String> stages) {

    public static DeliveryLabResponse from(
            CrossingDeliveryResult result, BoundaryPrincipal principal, String sourcePayload) {
        DecisionResponse decision = DecisionResponse.fromCrossing(
                result.crossing(), IngressKind.TOKEN_PROTECTED_API, principal, true, sourcePayload);
        return new DeliveryLabResponse(
                "SYNTHETIC DEMO - local delivery worker. Not Pub/Sub. Not Secret Manager.",
                decision,
                InstructionView.from(result.instruction().orElse(null)),
                DeliveryView.from(result.delivery()),
                CredentialView.from(result),
                ConsentView.from(result),
                QuarantineView.from(result.quarantine()),
                EvidenceView.from(result),
                List.of(
                        "Boundary Decision",
                        "Delivery Instruction",
                        "Credential / Consent",
                        "Camel Route",
                        "Connector",
                        "Resilience",
                        "Delivery Result",
                        "Audit / Attestation"));
    }

    public record InstructionView(
            @JsonProperty("event_id") String eventId,
            @JsonProperty("transaction_id") String transactionId,
            String destination,
            String outcome,
            @JsonProperty("payload_json") String payloadJson,
            @JsonProperty("contract_reference") String contractReference) {

        static InstructionView from(DeliveryInstruction instruction) {
            if (instruction == null) {
                return null;
            }
            return new InstructionView(
                    instruction.eventId().value(),
                    instruction.transactionId().value(),
                    instruction.destinationSystem(),
                    instruction.boundaryOutcome().name(),
                    instruction.payloadJson(),
                    instruction.contractReference());
        }
    }

    public record DeliveryView(
            String kind,
            @JsonProperty("attempt_count") int attemptCount,
            @JsonProperty("provider_category") String providerCategory,
            String reason,
            String explanation,
            @JsonProperty("connector_id") String connectorId,
            @JsonProperty("route_id") String routeId,
            @JsonProperty("circuit_state") String circuitState,
            @JsonProperty("http_status") Integer httpStatus,
            @JsonProperty("provider_called") boolean providerCalled) {

        static DeliveryView from(DeliveryResult result) {
            if (result == null) {
                return null;
            }
            return new DeliveryView(
                    result.kind().name(),
                    result.attemptCount(),
                    result.providerCategory().name(),
                    result.reason() == null ? null : result.reason().code(),
                    result.safeExplanation(),
                    result.connectorId(),
                    result.routeId(),
                    result.circuitState(),
                    result.httpStatus(),
                    result.providerCalled());
        }
    }

    public record CredentialView(String reference, String status, String explanation) {
        static CredentialView from(CrossingDeliveryResult result) {
            var control = result.crossing().vector().credentialResolvability();
            return new CredentialView(
                    result.crossing().credentialReference() == null
                            ? null
                            : result.crossing().credentialReference().display(),
                    control.status().name(),
                    control.safeExplanation());
        }
    }

    public record ConsentView(String id, String status, List<String> permissions, String explanation) {
        static ConsentView from(CrossingDeliveryResult result) {
            var consent = result.crossing().consent();
            return new ConsentView(
                    consent.grant() == null ? null : consent.grant().id().value(),
                    consent.status().name(),
                    consent.grant() == null
                            ? List.of()
                            : consent.grant().permissions().stream()
                                    .map(item -> item.value())
                                    .toList(),
                    consent.explanation());
        }
    }

    public record QuarantineView(
            String id, String reason, String status, int attempts, boolean eligible, String destination) {

        static QuarantineView from(QuarantineItem item) {
            if (item == null) {
                return null;
            }
            return new QuarantineView(
                    item.id().value(),
                    item.reason().name(),
                    item.status().name(),
                    item.recoveryAttempts(),
                    item.recoveryDecision()
                            == dev.adellaghmari.boundarymesh.domain.quarantine.RecoveryDecision.ELIGIBLE,
                    item.destinationSystem());
        }
    }

    public record EvidenceView(
            @JsonProperty("attestation_id") String attestationId,
            @JsonProperty("attestation_status") String attestationStatus,
            @JsonProperty("audit_chain_id") String auditChainId) {

        static EvidenceView from(CrossingDeliveryResult result) {
            if (result.attestation() == null) {
                return null;
            }
            return new EvidenceView(
                    result.attestation().id().value(),
                    result.attestation().status().name(),
                    result.attestation().auditChainId().value());
        }
    }
}
