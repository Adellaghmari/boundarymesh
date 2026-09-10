package dev.adellaghmari.boundarymesh.application.evidence;

import dev.adellaghmari.boundarymesh.application.attestation.AttestationFactory;
import dev.adellaghmari.boundarymesh.application.attestation.AttestationRepository;
import dev.adellaghmari.boundarymesh.application.audit.AuditAppender;
import dev.adellaghmari.boundarymesh.application.audit.AuditRepository;
import dev.adellaghmari.boundarymesh.application.crossing.CrossingComposer.CrossingCommand;
import dev.adellaghmari.boundarymesh.application.crossing.CrossingComposer.CrossingResult;
import dev.adellaghmari.boundarymesh.application.graph.ObservedCrossingStore;
import dev.adellaghmari.boundarymesh.domain.BoundaryOutcome;
import dev.adellaghmari.boundarymesh.domain.attestation.ExchangeAttestation;
import dev.adellaghmari.boundarymesh.domain.audit.AuditChainHead;
import dev.adellaghmari.boundarymesh.domain.audit.AuditChainId;
import dev.adellaghmari.boundarymesh.domain.audit.AuditEntry;
import dev.adellaghmari.boundarymesh.domain.audit.AuditEventType;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryInstruction;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryResult;
import dev.adellaghmari.boundarymesh.domain.identity.BoundaryPrincipal;
import dev.adellaghmari.boundarymesh.domain.quarantine.QuarantineItem;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class CrossingEvidenceService {

    private final AuditAppender audit;
    private final AuditRepository auditRepository;
    private final AttestationFactory attestations;
    private final AttestationRepository attestationRepository;
    private final ObservedCrossingStore observations;

    public CrossingEvidenceService(
            AuditAppender audit,
            AuditRepository auditRepository,
            AttestationFactory attestations,
            AttestationRepository attestationRepository,
            ObservedCrossingStore observations) {
        this.audit = Objects.requireNonNull(audit);
        this.auditRepository = Objects.requireNonNull(auditRepository);
        this.attestations = Objects.requireNonNull(attestations);
        this.attestationRepository = Objects.requireNonNull(attestationRepository);
        this.observations = Objects.requireNonNull(observations);
    }

    public Evidence record(
            CrossingCommand command,
            CrossingResult crossing,
            Optional<DeliveryInstruction> instruction,
            DeliveryResult delivery,
            QuarantineItem quarantine,
            BoundaryPrincipal principal,
            boolean reprocess) {
        AuditChainId chainId = AuditChainId.ofIntegration(command.integrationId());
        String transactionId = crossing.decision().transactionId().value();
        if (reprocess) {
            audit.append(chainId, AuditEventType.REPROCESS_STARTED, Map.of("transaction_id", transactionId));
        }
        audit.append(chainId, AuditEventType.BOUNDARY_DECISION, decisionAttributes(command, crossing, transactionId));
        if (instruction.isPresent()) {
            audit.append(
                    chainId,
                    AuditEventType.DELIVERY_INSTRUCTION_CREATED,
                    Map.of(
                            "transaction_id",
                            transactionId,
                            "destination",
                            command.destination(),
                            "outcome",
                            instruction.get().boundaryOutcome().name()));
        }
        if (delivery != null) {
            Map<String, String> deliveryAttrs = new LinkedHashMap<>();
            deliveryAttrs.put("transaction_id", transactionId);
            deliveryAttrs.put("kind", delivery.kind().name());
            deliveryAttrs.put("attempt_count", Integer.toString(delivery.attemptCount()));
            if (delivery.routeId() != null) {
                deliveryAttrs.put("route_id", delivery.routeId());
            }
            if (delivery.connectorId() != null) {
                deliveryAttrs.put("connector_id", delivery.connectorId());
            }
            audit.append(chainId, AuditEventType.DELIVERY_RESULT, deliveryAttrs);
        }
        if (quarantine != null) {
            AuditEventType type = reprocess && crossing.decision().outcome() != BoundaryOutcome.QUARANTINE
                    ? AuditEventType.QUARANTINE_RESOLVED
                    : AuditEventType.QUARANTINE_CREATED;
            if (reprocess && quarantine.status().name().equals("RESOLVED")) {
                type = AuditEventType.QUARANTINE_RESOLVED;
            }
            audit.append(
                    chainId,
                    type,
                    Map.of(
                            "transaction_id",
                            transactionId,
                            "quarantine_id",
                            quarantine.id().value(),
                            "reason",
                            quarantine.reason().name(),
                            "status",
                            quarantine.status().name()));
        }
        AuditChainHead head = auditRepository.head(chainId).orElse(AuditChainHead.empty(chainId));
        ExchangeAttestation attestation = attestations.create(
                crossing,
                "QUAYFORGE",
                command.destination(),
                principal == null ? null : principal.id().value(),
                principal == null ? "WEBHOOK_HMAC" : "JWT_BEARER",
                delivery,
                chainId,
                head);
        attestationRepository.save(attestation);
        audit.append(
                chainId,
                AuditEventType.ATTESTATION_CREATED,
                Map.of(
                        "transaction_id",
                        transactionId,
                        "attestation_id",
                        attestation.id().value(),
                        "status",
                        attestation.status().name()));
        AuditEntry last = auditRepository.entries(chainId, 1).isEmpty()
                ? null
                : auditRepository.entries(chainId, 1).getLast();
        observations.record(new ObservedCrossingStore.Observation(
                command.integrationId(),
                "QUAYFORGE",
                command.destination(),
                crossing.decision().outcome().name(),
                delivery == null ? "NOT_ATTEMPTED" : delivery.kind().name(),
                chainId.value(),
                attestation.id().value(),
                quarantine != null && !quarantine.status().name().equals("RESOLVED"),
                attestation.occurredAt()));
        return new Evidence(chainId, last, attestation);
    }

    private static Map<String, String> decisionAttributes(
            CrossingCommand command, CrossingResult crossing, String transactionId) {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("transaction_id", transactionId);
        attributes.put("integration_id", command.integrationId());
        attributes.put("destination", command.destination());
        attributes.put("outcome", crossing.decision().outcome().name());
        if (!crossing.decision().reasonCodes().isEmpty()) {
            attributes.put(
                    "reason_code", crossing.decision().reasonCodes().getFirst().code());
        }
        crossing.vector()
                .asList()
                .forEach(control -> attributes.put(
                        control.kind().name().toLowerCase(), control.status().name()));
        if (crossing.contract().reference() != null) {
            attributes.put(
                    "contract_reference",
                    crossing.contract().reference().id().value()
                            + ":"
                            + crossing.contract().reference().version().value());
        }
        if (crossing.credentialReference() != null) {
            attributes.put(
                    "credential_reference", crossing.credentialReference().display());
        }
        attributes.put("consent", crossing.consent().status().name());
        return attributes;
    }

    public record Evidence(AuditChainId chainId, AuditEntry lastEntry, ExchangeAttestation attestation) {}
}
