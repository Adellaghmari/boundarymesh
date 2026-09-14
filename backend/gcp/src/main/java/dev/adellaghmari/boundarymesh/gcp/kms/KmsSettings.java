package dev.adellaghmari.boundarymesh.gcp.kms;

import java.util.Objects;

/** Typed Cloud KMS coordinates. Resource names are built here, not scattered through callers. */
public record KmsSettings(
        String projectId, String location, String keyRing, String cryptoKey, String cryptoKeyVersion) {

    public static final String DEFAULT_PROJECT = "boundarymesh-dev";
    public static final String DEFAULT_LOCATION = "europe-west4";
    public static final String DEFAULT_KEY_RING = "boundarymesh-attestation";
    public static final String DEFAULT_CRYPTO_KEY = "exchange-attestation";
    public static final String DEFAULT_VERSION = "1";
    public static final String ALGORITHM = "EC_SIGN_P256_SHA256";

    public KmsSettings {
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(keyRing, "keyRing");
        Objects.requireNonNull(cryptoKey, "cryptoKey");
        Objects.requireNonNull(cryptoKeyVersion, "cryptoKeyVersion");
        if (projectId.isBlank()
                || location.isBlank()
                || keyRing.isBlank()
                || cryptoKey.isBlank()
                || cryptoKeyVersion.isBlank()) {
            throw new IllegalArgumentException("KMS coordinates are required");
        }
        if (projectId.contains("/")
                || location.contains("/")
                || keyRing.contains("/")
                || cryptoKey.contains("/")
                || cryptoKeyVersion.contains("/")) {
            throw new IllegalArgumentException("KMS coordinates must be path segments, not resource names");
        }
    }

    public static KmsSettings production() {
        return new KmsSettings(
                DEFAULT_PROJECT, DEFAULT_LOCATION, DEFAULT_KEY_RING, DEFAULT_CRYPTO_KEY, DEFAULT_VERSION);
    }

    public String keyRingName() {
        return KmsResourceNames.keyRing(projectId, location, keyRing);
    }

    public String cryptoKeyName() {
        return KmsResourceNames.cryptoKey(projectId, location, keyRing, cryptoKey);
    }

    public String cryptoKeyVersionName() {
        return KmsResourceNames.cryptoKeyVersion(projectId, location, keyRing, cryptoKey, cryptoKeyVersion);
    }
}
