package dev.adellaghmari.boundarymesh.application.attestation;

import dev.adellaghmari.boundarymesh.application.canon.Rfc8785Canonicalizer;
import dev.adellaghmari.boundarymesh.domain.attestation.AttestationSignature;
import dev.adellaghmari.boundarymesh.domain.attestation.AttestationVerificationResult;
import dev.adellaghmari.boundarymesh.domain.attestation.ExchangeAttestation;
import dev.adellaghmari.boundarymesh.domain.attestation.SignatureVerificationStatus;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public final class AttestationCanonicalizer {

    private AttestationCanonicalizer() {}

    public static Map<String, Object> payload(ExchangeAttestation attestation) {
        Objects.requireNonNull(attestation, "attestation");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("algorithm", attestation.signer().algorithm());
        body.put("attestation_id", attestation.id().value());
        body.put("audit_chain_head", attestation.auditHead().hex());
        body.put("audit_chain_id", attestation.auditChainId().value());
        body.put("audit_sequence", attestation.auditSequence());
        body.put("authentication_method", nullToEmpty(attestation.authenticationMethod()));
        body.put("boundary_outcome", attestation.boundaryOutcome().name());
        body.put("connector_route", nullToEmpty(attestation.connectorRoute()));
        body.put("consent_result", nullToEmpty(attestation.consentResult()));
        body.put("contract_id", nullToEmpty(attestation.contractId()));
        body.put("contract_result", nullToEmpty(attestation.contractResult()));
        body.put("contract_version", nullToEmpty(attestation.contractVersion()));
        body.put("control_results", new TreeMap<>(attestation.controlResults()));
        body.put("credential_reference", nullToEmpty(attestation.credentialReference()));
        body.put("delivery_result", nullToEmpty(attestation.deliveryResult()));
        body.put("destination_system", nullToEmpty(attestation.destinationSystem()));
        body.put("egress_policy_id", nullToEmpty(attestation.egressPolicyId()));
        body.put("integration_id", attestation.integrationId());
        body.put("key_id", attestation.signer().keyId());
        body.put("key_version", attestation.signer().keyVersion());
        body.put("occurred_at", attestation.occurredAt().toString());
        body.put("principal_reference", nullToEmpty(attestation.principalReference()));
        body.put("schema_version", attestation.schemaVersion().value());
        body.put("source_system", nullToEmpty(attestation.sourceSystem()));
        body.put("transformation_count", (long) attestation.transformationCount());
        body.put("transaction_id", attestation.transactionId());
        return body;
    }

    public static byte[] bytes(ExchangeAttestation attestation) {
        return Rfc8785Canonicalizer.canonicalize(payload(attestation));
    }

    public static AttestationVerificationResult unsignedResult(ExchangeAttestation attestation) {
        return new AttestationVerificationResult(
                attestation.id(),
                SignatureVerificationStatus.UNSIGNED,
                "No signature was produced. Crossing history was not rewritten.");
    }

    public static boolean hasSignature(AttestationSignature signature) {
        return signature != null && signature.present();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
