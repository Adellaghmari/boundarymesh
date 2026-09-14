package dev.adellaghmari.boundarymesh.gcp.kms;

import com.google.cloud.kms.v1.AsymmetricSignRequest;
import com.google.cloud.kms.v1.CryptoKeyVersion.CryptoKeyVersionAlgorithm;
import com.google.cloud.kms.v1.Digest;
import com.google.cloud.kms.v1.KeyManagementServiceClient;
import com.google.protobuf.ByteString;
import com.google.protobuf.Int64Value;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.zip.CRC32C;

/** Official Cloud KMS client. Never fetches private key material. */
public final class GoogleKmsSigningClient implements KmsSigningClient, AutoCloseable {

    private final KeyManagementServiceClient client;

    GoogleKmsSigningClient(KeyManagementServiceClient client) {
        this.client = Objects.requireNonNull(client, "client");
    }

    public static GoogleKmsSigningClient connect() {
        try {
            return new GoogleKmsSigningClient(KeyManagementServiceClient.create());
        } catch (IOException ex) {
            throw CloudKmsAccessException.wrap(ex);
        }
    }

    public Class<?> sdkClientType() {
        return client.getClass();
    }

    @Override
    public byte[] signSha256Digest(String cryptoKeyVersionName, byte[] sha256Digest) {
        Objects.requireNonNull(cryptoKeyVersionName, "cryptoKeyVersionName");
        Objects.requireNonNull(sha256Digest, "sha256Digest");
        try {
            if (sha256Digest.length != 32) {
                throw new IllegalArgumentException("SHA-256 digest must be 32 bytes");
            }
            AsymmetricSignRequest request = AsymmetricSignRequest.newBuilder()
                    .setName(cryptoKeyVersionName)
                    .setDigest(Digest.newBuilder().setSha256(ByteString.copyFrom(sha256Digest)))
                    .setDigestCrc32C(Int64Value.of(crc32c(sha256Digest)))
                    .build();
            var response = client.asymmetricSign(request);
            byte[] signature = response.getSignature().toByteArray();
            if (!response.getVerifiedDigestCrc32C()
                    || !cryptoKeyVersionName.equals(response.getName())
                    || signature.length == 0
                    || !response.hasSignatureCrc32C()
                    || response.getSignatureCrc32C().getValue() != crc32c(signature)) {
                throw new IllegalStateException("KMS signature integrity check failed");
            }
            return signature;
        } catch (RuntimeException ex) {
            throw CloudKmsAccessException.wrap(ex);
        }
    }

    @Override
    public String publicKeyPem(String cryptoKeyVersionName) {
        Objects.requireNonNull(cryptoKeyVersionName, "cryptoKeyVersionName");
        try {
            var response = client.getPublicKey(cryptoKeyVersionName);
            if (!cryptoKeyVersionName.equals(response.getName())
                    || response.getAlgorithm() != CryptoKeyVersionAlgorithm.EC_SIGN_P256_SHA256
                    || !response.hasPemCrc32C()
                    || response.getPemCrc32C().getValue()
                            != crc32c(response.getPem().getBytes(StandardCharsets.UTF_8))) {
                throw new IllegalStateException("KMS public key integrity check failed");
            }
            return response.getPem();
        } catch (RuntimeException ex) {
            throw CloudKmsAccessException.wrap(ex);
        }
    }

    @Override
    public void close() {
        client.close();
    }

    private static long crc32c(byte[] bytes) {
        CRC32C checksum = new CRC32C();
        checksum.update(bytes);
        return checksum.getValue();
    }
}
