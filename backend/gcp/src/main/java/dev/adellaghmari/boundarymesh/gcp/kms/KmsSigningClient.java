package dev.adellaghmari.boundarymesh.gcp.kms;

/**
 * Narrow Cloud KMS surface. Implementations must never return private key material. Tests use a
 * fake; production uses the official Java client.
 */
public interface KmsSigningClient {

    byte[] signSha256Digest(String cryptoKeyVersionName, byte[] sha256Digest);

    String publicKeyPem(String cryptoKeyVersionName);
}
