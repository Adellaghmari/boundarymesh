package dev.adellaghmari.boundarymesh.application.attestation;

import dev.adellaghmari.boundarymesh.domain.attestation.AttestationSignature;
import dev.adellaghmari.boundarymesh.domain.attestation.SignerKeyReference;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.UUID;

/**
 * Local ECDSA P-256 signer. Compatible conceptually with Cloud KMS EC_SIGN_P256_SHA256 (P1363).
 * Key material is ephemeral and not persisted. Signatures will not verify across process restart.
 */
public final class JcaP256AttestationCrypto implements AttestationSigner, AttestationVerifier {

    static final String JCA_ALGORITHM = "SHA256withECDSAinP1363Format";

    private final KeyPair keyPair;
    private final SignerKeyReference reference;

    public JcaP256AttestationCrypto() {
        this.keyPair = generate();
        this.reference = new SignerKeyReference(
                SignerKeyReference.LOCAL_KEY_ID, UUID.randomUUID().toString(), SignerKeyReference.ALGORITHM);
    }

    public JcaP256AttestationCrypto(KeyPair keyPair, SignerKeyReference reference) {
        this.keyPair = keyPair;
        this.reference = reference;
    }

    @Override
    public SignerKeyReference key() {
        return reference;
    }

    @Override
    public AttestationSignature sign(byte[] canonicalBytes) {
        try {
            Signature signature = Signature.getInstance(JCA_ALGORITHM);
            signature.initSign(keyPair.getPrivate());
            signature.update(canonicalBytes);
            return new AttestationSignature(Base64.getEncoder().encodeToString(signature.sign()));
        } catch (Exception ex) {
            throw new IllegalStateException("attestation signing failed", ex);
        }
    }

    @Override
    public boolean verify(byte[] canonicalBytes, AttestationSignature signature, SignerKeyReference key) {
        if (signature == null || !signature.present()) {
            return false;
        }
        if (key != null && !reference.keyVersion().equals(key.keyVersion())) {
            return false;
        }
        try {
            Signature verifier = Signature.getInstance(JCA_ALGORITHM);
            verifier.initVerify(keyPair.getPublic());
            verifier.update(canonicalBytes);
            return verifier.verify(Base64.getDecoder().decode(signature.base64()));
        } catch (Exception ex) {
            return false;
        }
    }

    private static KeyPair generate() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1"));
            return generator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException("unable to generate local P-256 key", ex);
        }
    }
}
