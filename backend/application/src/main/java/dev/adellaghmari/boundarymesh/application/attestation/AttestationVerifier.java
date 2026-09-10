package dev.adellaghmari.boundarymesh.application.attestation;

import dev.adellaghmari.boundarymesh.domain.attestation.AttestationSignature;
import dev.adellaghmari.boundarymesh.domain.attestation.SignerKeyReference;

public interface AttestationVerifier {

    boolean verify(byte[] canonicalBytes, AttestationSignature signature, SignerKeyReference key);
}
