package dev.adellaghmari.boundarymesh.domain.attestation;

import java.util.Objects;

public record AttestationVerificationResult(
        AttestationId attestationId, SignatureVerificationStatus status, String explanation) {

    public AttestationVerificationResult {
        Objects.requireNonNull(attestationId, "attestationId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(explanation, "explanation");
    }

    public boolean valid() {
        return status == SignatureVerificationStatus.VALID;
    }
}
