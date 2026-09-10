package dev.adellaghmari.boundarymesh.application.attestation;

import dev.adellaghmari.boundarymesh.domain.attestation.AttestationStatus;
import dev.adellaghmari.boundarymesh.domain.attestation.AttestationVerificationResult;
import dev.adellaghmari.boundarymesh.domain.attestation.ExchangeAttestation;
import dev.adellaghmari.boundarymesh.domain.attestation.SignatureVerificationStatus;
import java.util.Objects;

public final class AttestationService {

    private final AttestationRepository repository;
    private final AttestationVerifier verifier;

    public AttestationService(AttestationRepository repository, AttestationVerifier verifier) {
        this.repository = Objects.requireNonNull(repository);
        this.verifier = Objects.requireNonNull(verifier);
    }

    public AttestationVerificationResult verify(ExchangeAttestation attestation) {
        Objects.requireNonNull(attestation, "attestation");
        if (attestation.status() == AttestationStatus.UNSIGNED
                || !attestation.signature().present()) {
            return AttestationCanonicalizer.unsignedResult(attestation);
        }
        boolean ok = verifier.verify(
                AttestationCanonicalizer.bytes(attestation), attestation.signature(), attestation.signer());
        if (ok) {
            return new AttestationVerificationResult(
                    attestation.id(),
                    SignatureVerificationStatus.VALID,
                    "ECDSA P-256 signature matches the canonical attestation bytes.");
        }
        return new AttestationVerificationResult(
                attestation.id(),
                SignatureVerificationStatus.INVALID,
                "Signature does not match the canonical attestation bytes.");
    }

    public AttestationRepository repository() {
        return repository;
    }
}
