package dev.adellaghmari.boundarymesh.api.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.adellaghmari.boundarymesh.application.attestation.AttestationService;
import dev.adellaghmari.boundarymesh.domain.attestation.AttestationId;
import dev.adellaghmari.boundarymesh.domain.attestation.AttestationVerificationResult;
import dev.adellaghmari.boundarymesh.domain.attestation.ExchangeAttestation;
import dev.adellaghmari.boundarymesh.domain.exception.InvalidInputException;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/attestations", produces = MediaType.APPLICATION_JSON_VALUE)
public class AttestationController {

    private static final int MAX = 50;

    private final AttestationService attestations;

    public AttestationController(AttestationService attestations) {
        this.attestations = attestations;
    }

    @GetMapping
    public ListView list() {
        List<ExchangeAttestation> items = attestations.repository().list(MAX);
        return new ListView(
                "LOCAL / SYNTHETIC SESSION DATA. Local JCA P-256 signatures. Cloud KMS remains PLANNED.",
                items.stream().map(AttestationView::from).toList());
    }

    @GetMapping("/{id}")
    public AttestationView one(@PathVariable String id) {
        return AttestationView.from(attestations
                .repository()
                .find(new AttestationId(id))
                .orElseThrow(() -> new InvalidInputException("ATTESTATION_NOT_FOUND", "attestation was not found")));
    }

    @GetMapping("/{id}/summary")
    public SummaryView summary(@PathVariable String id) {
        ExchangeAttestation item = attestations
                .repository()
                .find(new AttestationId(id))
                .orElseThrow(() -> new InvalidInputException("ATTESTATION_NOT_FOUND", "attestation was not found"));
        return new SummaryView(
                item.id().value(),
                item.transactionId(),
                item.integrationId(),
                item.boundaryOutcome().name(),
                item.status().name(),
                item.signer().algorithm(),
                item.signer().keyVersion(),
                item.auditChainId().value(),
                item.signature().present());
    }

    public record SummaryView(
            String id,
            @JsonProperty("transaction_id") String transactionId,
            @JsonProperty("integration_id") String integrationId,
            @JsonProperty("boundary_outcome") String boundaryOutcome,
            String status,
            String algorithm,
            @JsonProperty("key_version") String keyVersion,
            @JsonProperty("audit_chain_id") String auditChainId,
            @JsonProperty("signature_present") boolean signaturePresent) {}

    @PostMapping("/{id}/verify")
    public VerifyView verify(@PathVariable String id) {
        ExchangeAttestation attestation = attestations
                .repository()
                .find(new AttestationId(id))
                .orElseThrow(() -> new InvalidInputException("ATTESTATION_NOT_FOUND", "attestation was not found"));
        return VerifyView.from(attestations.verify(attestation));
    }

    @GetMapping("/{id}/verify")
    public VerifyView verifyGet(@PathVariable String id) {
        return verify(id);
    }

    public record ListView(String banner, List<AttestationView> items) {}

    public record AttestationView(
            String id,
            @JsonProperty("transaction_id") String transactionId,
            @JsonProperty("integration_id") String integrationId,
            String source,
            String destination,
            @JsonProperty("boundary_outcome") String boundaryOutcome,
            @JsonProperty("contract_id") String contractId,
            @JsonProperty("contract_version") String contractVersion,
            @JsonProperty("egress_policy_id") String egressPolicyId,
            @JsonProperty("transformation_count") int transformationCount,
            @JsonProperty("credential_reference") String credentialReference,
            @JsonProperty("consent_result") String consentResult,
            @JsonProperty("connector_route") String connectorRoute,
            @JsonProperty("delivery_result") String deliveryResult,
            @JsonProperty("audit_chain_id") String auditChainId,
            @JsonProperty("audit_sequence") long auditSequence,
            @JsonProperty("audit_head_short") String auditHeadShort,
            String algorithm,
            @JsonProperty("key_id") String keyId,
            @JsonProperty("key_version") String keyVersion,
            String status,
            @JsonProperty("signature_present") boolean signaturePresent,
            @JsonProperty("control_results") Map<String, String> controlResults,
            String claim) {

        static AttestationView from(ExchangeAttestation item) {
            return new AttestationView(
                    item.id().value(),
                    item.transactionId(),
                    item.integrationId(),
                    item.sourceSystem(),
                    item.destinationSystem(),
                    item.boundaryOutcome().name(),
                    item.contractId(),
                    item.contractVersion(),
                    item.egressPolicyId(),
                    item.transformationCount(),
                    item.credentialReference(),
                    item.consentResult(),
                    item.connectorRoute(),
                    item.deliveryResult(),
                    item.auditChainId().value(),
                    item.auditSequence(),
                    item.auditHead().shortened(),
                    item.signer().algorithm(),
                    item.signer().keyId(),
                    item.signer().keyVersion(),
                    item.status().name(),
                    item.signature().present(),
                    item.controlResults(),
                    "Proves this enforcement artifact was signed and has not been modified without detection. Does not prove business data is true.");
        }
    }

    public record VerifyView(String id, String status, String explanation) {
        static VerifyView from(AttestationVerificationResult result) {
            return new VerifyView(
                    result.attestationId().value(), result.status().name(), result.explanation());
        }
    }
}
