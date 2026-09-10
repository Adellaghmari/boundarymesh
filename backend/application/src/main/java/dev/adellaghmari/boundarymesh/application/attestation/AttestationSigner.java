package dev.adellaghmari.boundarymesh.application.attestation;

import dev.adellaghmari.boundarymesh.domain.attestation.AttestationSignature;
import dev.adellaghmari.boundarymesh.domain.attestation.SignerKeyReference;

public interface AttestationSigner {

    SignerKeyReference key();

    AttestationSignature sign(byte[] canonicalBytes);
}
