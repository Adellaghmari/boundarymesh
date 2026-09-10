package dev.adellaghmari.boundarymesh.domain.attestation;

import java.util.Objects;

/** Base64 P1363 ECDSA signature bytes. Empty when the attestation is UNSIGNED. */
public record AttestationSignature(String base64) {

    public static AttestationSignature none() {
        return new AttestationSignature("");
    }

    public AttestationSignature {
        Objects.requireNonNull(base64, "signature");
    }

    public boolean present() {
        return !base64.isBlank();
    }
}
