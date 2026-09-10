package dev.adellaghmari.boundarymesh.domain.attestation;

import java.util.Objects;

/**
 * Identifies the signing key. Never contains private material. Local sessions use an ephemeral
 * version that does not survive process restart.
 */
public record SignerKeyReference(String keyId, String keyVersion, String algorithm) {

    public static final String LOCAL_KEY_ID = "local-jca-p256";
    public static final String ALGORITHM = "ES256";

    public SignerKeyReference {
        Objects.requireNonNull(keyId, "keyId");
        Objects.requireNonNull(keyVersion, "keyVersion");
        Objects.requireNonNull(algorithm, "algorithm");
    }
}
