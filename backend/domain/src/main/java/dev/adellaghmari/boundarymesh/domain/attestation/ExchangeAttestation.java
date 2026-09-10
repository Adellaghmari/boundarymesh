package dev.adellaghmari.boundarymesh.domain.attestation;

import dev.adellaghmari.boundarymesh.domain.BoundaryOutcome;
import dev.adellaghmari.boundarymesh.domain.audit.AuditChainId;
import dev.adellaghmari.boundarymesh.domain.audit.AuditHash;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Signed summary of one terminal crossing. Proves the recorded enforcement artifact has not been
 * modified without detection. Does not prove business data is true.
 */
public record ExchangeAttestation(
        AttestationId id,
        AttestationVersion schemaVersion,
        String transactionId,
        String integrationId,
        String sourceSystem,
        String destinationSystem,
        Instant occurredAt,
        String principalReference,
        String authenticationMethod,
        Map<String, String> controlResults,
        String contractId,
        String contractVersion,
        String contractResult,
        String egressPolicyId,
        int transformationCount,
        String credentialReference,
        String consentResult,
        String connectorRoute,
        String deliveryResult,
        BoundaryOutcome boundaryOutcome,
        AuditChainId auditChainId,
        long auditSequence,
        AuditHash auditHead,
        SignerKeyReference signer,
        AttestationStatus status,
        AttestationSignature signature) {

    public ExchangeAttestation {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(schemaVersion, "schemaVersion");
        Objects.requireNonNull(transactionId, "transactionId");
        Objects.requireNonNull(integrationId, "integrationId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(controlResults, "controlResults");
        Objects.requireNonNull(boundaryOutcome, "boundaryOutcome");
        Objects.requireNonNull(auditChainId, "auditChainId");
        Objects.requireNonNull(auditHead, "auditHead");
        Objects.requireNonNull(signer, "signer");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(signature, "signature");
        controlResults = Map.copyOf(new TreeMap<>(controlResults));
    }

    public ExchangeAttestation signed(AttestationSignature value) {
        return new ExchangeAttestation(
                id,
                schemaVersion,
                transactionId,
                integrationId,
                sourceSystem,
                destinationSystem,
                occurredAt,
                principalReference,
                authenticationMethod,
                controlResults,
                contractId,
                contractVersion,
                contractResult,
                egressPolicyId,
                transformationCount,
                credentialReference,
                consentResult,
                connectorRoute,
                deliveryResult,
                boundaryOutcome,
                auditChainId,
                auditSequence,
                auditHead,
                signer,
                AttestationStatus.SIGNED,
                value);
    }

    public ExchangeAttestation unsigned() {
        return new ExchangeAttestation(
                id,
                schemaVersion,
                transactionId,
                integrationId,
                sourceSystem,
                destinationSystem,
                occurredAt,
                principalReference,
                authenticationMethod,
                controlResults,
                contractId,
                contractVersion,
                contractResult,
                egressPolicyId,
                transformationCount,
                credentialReference,
                consentResult,
                connectorRoute,
                deliveryResult,
                boundaryOutcome,
                auditChainId,
                auditSequence,
                auditHead,
                signer,
                AttestationStatus.UNSIGNED,
                AttestationSignature.none());
    }
}
