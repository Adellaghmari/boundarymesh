package dev.adellaghmari.boundarymesh.gcp.kms;

import dev.adellaghmari.boundarymesh.application.attestation.AttestationSigner;
import dev.adellaghmari.boundarymesh.application.attestation.AttestationVerifier;
import dev.adellaghmari.boundarymesh.domain.attestation.AttestationSignature;
import dev.adellaghmari.boundarymesh.domain.attestation.SignerKeyReference;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;
import java.util.Objects;

/**
 * Production attestation signer. Cloud KMS holds the private key. Canonical bytes are SHA-256
 * digested locally, signed with {@code EC_SIGN_P256_SHA256}, then normalized from DER to IEEE
 * P1363 so {@link AttestationSignature} stays stable.
 */
public final class CloudKmsAttestationSigner implements AttestationSigner, AttestationVerifier, AutoCloseable {

    static final String JCA_VERIFY = "SHA256withECDSAinP1363Format";

    private final KmsSettings settings;
    private final KmsSigningClient client;
    private final SignerKeyReference reference;
    private volatile PublicKey cachedPublicKey;

    public CloudKmsAttestationSigner(KmsSettings settings, KmsSigningClient client) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.client = Objects.requireNonNull(client, "client");
        this.reference = new SignerKeyReference(
                settings.cryptoKeyName(), settings.cryptoKeyVersion(), SignerKeyReference.ALGORITHM);
    }

    public static CloudKmsAttestationSigner connect(KmsSettings settings) {
        return new CloudKmsAttestationSigner(settings, GoogleKmsSigningClient.connect());
    }

    public KmsSettings settings() {
        return settings;
    }

    @Override
    public SignerKeyReference key() {
        return reference;
    }

    @Override
    public AttestationSignature sign(byte[] canonicalBytes) {
        Objects.requireNonNull(canonicalBytes, "canonicalBytes");
        try {
            byte[] digest = sha256(canonicalBytes);
            byte[] der = client.signSha256Digest(settings.cryptoKeyVersionName(), digest);
            byte[] p1363 = EcdsaP1363Codec.derToP1363(der);
            AttestationSignature signature =
                    new AttestationSignature(Base64.getEncoder().encodeToString(p1363));
            return verify(canonicalBytes, signature, reference) ? signature : AttestationSignature.none();
        } catch (RuntimeException ex) {
            return AttestationSignature.none();
        }
    }

    @Override
    public boolean verify(byte[] canonicalBytes, AttestationSignature signature, SignerKeyReference key) {
        if (signature == null || !signature.present()) {
            return false;
        }
        if (key == null
                || !reference.keyId().equals(key.keyId())
                || !reference.keyVersion().equals(key.keyVersion())
                || !reference.algorithm().equals(key.algorithm())) {
            return false;
        }
        try {
            Signature verifier = Signature.getInstance(JCA_VERIFY);
            verifier.initVerify(publicKey());
            verifier.update(canonicalBytes);
            return verifier.verify(Base64.getDecoder().decode(signature.base64()));
        } catch (Exception ex) {
            return false;
        }
    }

    PublicKey publicKey() {
        PublicKey current = cachedPublicKey;
        if (current != null) {
            return current;
        }
        String pem = client.publicKeyPem(settings.cryptoKeyVersionName());
        if (!EcPublicKeys.looksLikePem(pem)) {
            throw new IllegalStateException("KMS public key PEM is required");
        }
        current = EcPublicKeys.fromPem(pem);
        cachedPublicKey = current;
        return current;
    }

    @Override
    public void close() {
        if (client instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception ignored) {
                // adapter shutdown
            }
        }
    }

    static byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
