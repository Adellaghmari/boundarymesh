package dev.adellaghmari.boundarymesh.api.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.adellaghmari.boundarymesh.application.attestation.AttestationService;
import dev.adellaghmari.boundarymesh.application.audit.AuditAppender;
import dev.adellaghmari.boundarymesh.application.audit.AuditChainVerifier;
import dev.adellaghmari.boundarymesh.domain.attestation.ExchangeAttestation;
import dev.adellaghmari.boundarymesh.domain.audit.AuditChainId;
import dev.adellaghmari.boundarymesh.domain.audit.AuditEntry;
import dev.adellaghmari.boundarymesh.domain.audit.AuditHash;
import dev.adellaghmari.boundarymesh.domain.audit.AuditVerificationResult;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Isolated synthetic tamper demos. Does not mutate the primary audit or attestation repositories.
 */
@RestController
@RequestMapping(path = "/api/v1/dev", produces = MediaType.APPLICATION_JSON_VALUE)
@ConditionalOnProperty(name = "boundarymesh.dev-evaluate-enabled", havingValue = "true")
public class DevEvidenceLabController {

    private final AttestationService attestations;

    public DevEvidenceLabController(AttestationService attestations) {
        this.attestations = attestations;
    }

    @GetMapping("/audit-lab/tamper-demo")
    public TamperDemoView auditTamper() {
        AuditChainId chainId = new AuditChainId("quayforge-orders");
        List<AuditEntry> original = isolatedSeed(chainId);
        AuditVerificationResult valid = new AuditChainVerifier().verify(chainId, original);
        AuditEntry first = original.getFirst();
        Map<String, String> mutated = new LinkedHashMap<>(first.attributes());
        mutated.put("outcome", "TAMPERED");
        AuditEntry tamperedEntry = new AuditEntry(
                first.id(),
                first.chainId(),
                first.sequence(),
                first.occurredAt(),
                first.eventType(),
                mutated,
                first.previousHash(),
                first.currentHash());
        List<AuditEntry> tampered = new ArrayList<>(original);
        tampered.set(0, tamperedEntry);
        AuditVerificationResult broken = new AuditChainVerifier().verify(chainId, tampered);
        return new TamperDemoView(
                "SYNTHETIC DEMO. Isolated copy only. The live audit repository was not mutated.",
                AuditChainController.VerifyView.from(valid),
                AuditChainController.VerifyView.from(broken));
    }

    @GetMapping("/attestation-lab/tamper-demo")
    public AttestationTamperView attestationTamper() {
        List<ExchangeAttestation> items = attestations.repository().list(50);
        if (items.isEmpty()) {
            return new AttestationTamperView(
                    "SYNTHETIC DEMO. Run a delivery-lab crossing first to create a signed attestation.",
                    null,
                    null,
                    "NONE");
        }
        ExchangeAttestation original = items.getLast();
        var valid = attestations.verify(original);
        ExchangeAttestation mutated = new ExchangeAttestation(
                original.id(),
                original.schemaVersion(),
                original.transactionId(),
                original.integrationId(),
                original.sourceSystem(),
                "TAMPERED-DESTINATION",
                original.occurredAt(),
                original.principalReference(),
                original.authenticationMethod(),
                original.controlResults(),
                original.contractId(),
                original.contractVersion(),
                original.contractResult(),
                original.egressPolicyId(),
                original.transformationCount(),
                original.credentialReference(),
                original.consentResult(),
                original.connectorRoute(),
                original.deliveryResult(),
                original.boundaryOutcome(),
                original.auditChainId(),
                original.auditSequence(),
                original.auditHead(),
                original.signer(),
                original.status(),
                original.signature());
        var broken = attestations.verify(mutated);
        return new AttestationTamperView(
                "SYNTHETIC DEMO. Isolated copy only. The live attestation repository was not mutated.",
                AttestationController.VerifyView.from(valid),
                AttestationController.VerifyView.from(broken),
                original.id().value());
    }

    private List<AuditEntry> isolatedSeed(AuditChainId chainId) {
        Instant now = Instant.parse("2026-09-10T12:00:00Z");
        AuditEntry first = new AuditEntry(
                new dev.adellaghmari.boundarymesh.domain.audit.AuditEntryId("seed-1"),
                chainId,
                1,
                now,
                dev.adellaghmari.boundarymesh.domain.audit.AuditEventType.BOUNDARY_DECISION,
                Map.of(
                        "outcome",
                        "ALLOW_WITH_TRANSFORMATION",
                        "transaction_id",
                        "00000000-0000-0000-0000-000000000001"),
                AuditHash.GENESIS,
                AuditHash.GENESIS);
        first = first.withCurrentHash(AuditAppender.hashOf(first));
        AuditEntry second = new AuditEntry(
                new dev.adellaghmari.boundarymesh.domain.audit.AuditEntryId("seed-2"),
                chainId,
                2,
                now.plusSeconds(1),
                dev.adellaghmari.boundarymesh.domain.audit.AuditEventType.DELIVERY_RESULT,
                Map.of("kind", "DELIVERED"),
                first.currentHash(),
                AuditHash.GENESIS);
        second = second.withCurrentHash(AuditAppender.hashOf(second));
        return List.of(first, second);
    }

    public record TamperDemoView(
            String banner, AuditChainController.VerifyView original, AuditChainController.VerifyView tampered) {}

    public record AttestationTamperView(
            String banner,
            AttestationController.VerifyView original,
            AttestationController.VerifyView tampered,
            @JsonProperty("attestation_id") String attestationId) {}
}
