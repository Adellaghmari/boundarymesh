package dev.adellaghmari.boundarymesh.gcp.kms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.kms.v1.AsymmetricSignRequest;
import com.google.cloud.kms.v1.AsymmetricSignResponse;
import com.google.cloud.kms.v1.CryptoKeyVersion.CryptoKeyVersionAlgorithm;
import com.google.cloud.kms.v1.KeyManagementServiceClient;
import com.google.cloud.kms.v1.PublicKey;
import com.google.protobuf.ByteString;
import com.google.protobuf.Int64Value;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32C;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Controlled SDK doubles only. These tests make no live requests. */
class GoogleKmsSigningClientTest {
    private static final String TARGET = KmsSettings.production().cryptoKeyVersionName();

    @Test
    void sendsSha256DigestAndChecksumAndRejectsCorruptedOrMisaddressedResponses() {
        KeyManagementServiceClient sdk = mock(KeyManagementServiceClient.class);
        try (GoogleKmsSigningClient client = new GoogleKmsSigningClient(sdk)) {
            byte[] digest = new byte[32];
            byte[] der = {0x30, 6, 2, 1, 1, 2, 1, 1};
            AsymmetricSignResponse good = AsymmetricSignResponse.newBuilder()
                    .setName(TARGET)
                    .setVerifiedDigestCrc32C(true)
                    .setSignature(ByteString.copyFrom(der))
                    .setSignatureCrc32C(Int64Value.of(crc(der)))
                    .build();
            when(sdk.asymmetricSign(any(AsymmetricSignRequest.class))).thenReturn(good);
            assertThat(client.signSha256Digest(TARGET, digest)).isEqualTo(der);
            ArgumentCaptor<AsymmetricSignRequest> request = ArgumentCaptor.forClass(AsymmetricSignRequest.class);
            verify(sdk).asymmetricSign(request.capture());
            assertThat(request.getValue().getName()).isEqualTo(TARGET);
            assertThat(request.getValue().getDigest().getSha256().toByteArray()).isEqualTo(digest);
            assertThat(request.getValue().getDigestCrc32C().getValue()).isEqualTo(crc(digest));
            assertThat(request.getValue().getData().isEmpty()).isTrue();
            for (AsymmetricSignResponse invalid : java.util.List.of(
                    good.toBuilder().setVerifiedDigestCrc32C(false).build(),
                    good.toBuilder().setName(TARGET + "0").build(),
                    good.toBuilder().setSignatureCrc32C(Int64Value.of(1)).build(),
                    good.toBuilder().clearSignatureCrc32C().build(),
                    good.toBuilder().clearSignature().build())) {
                when(sdk.asymmetricSign(any(AsymmetricSignRequest.class))).thenReturn(invalid);
                assertThatThrownBy(() -> client.signSha256Digest(TARGET, digest))
                        .isInstanceOf(CloudKmsAccessException.class)
                        .hasMessage("Cloud KMS call failed (PROVIDER_ERROR)");
            }
            assertThatThrownBy(() -> client.signSha256Digest(TARGET, new byte[31]))
                    .isInstanceOf(CloudKmsAccessException.class);
        }
        verify(sdk).close();
    }

    @Test
    void rejectsPublicKeyWithWrongAlgorithmNameOrChecksum() {
        KeyManagementServiceClient sdk = mock(KeyManagementServiceClient.class);
        try (GoogleKmsSigningClient client = new GoogleKmsSigningClient(sdk)) {
            String pem = "synthetic public key response";
            PublicKey good = PublicKey.newBuilder()
                    .setName(TARGET)
                    .setPem(pem)
                    .setAlgorithm(CryptoKeyVersionAlgorithm.EC_SIGN_P256_SHA256)
                    .setPemCrc32C(Int64Value.of(crc(pem.getBytes(StandardCharsets.UTF_8))))
                    .build();
            when(sdk.getPublicKey(TARGET)).thenReturn(good);
            assertThat(client.publicKeyPem(TARGET)).isEqualTo(pem);
            for (PublicKey invalid : java.util.List.of(
                    good.toBuilder().setName(TARGET + "0").build(),
                    good.toBuilder()
                            .setAlgorithm(CryptoKeyVersionAlgorithm.EC_SIGN_P384_SHA384)
                            .build(),
                    good.toBuilder().clearPemCrc32C().build(),
                    good.toBuilder().setPem("corrupted").build())) {
                when(sdk.getPublicKey(TARGET)).thenReturn(invalid);
                assertThatThrownBy(() -> client.publicKeyPem(TARGET))
                        .isInstanceOf(CloudKmsAccessException.class)
                        .hasMessage("Cloud KMS call failed (PROVIDER_ERROR)");
            }
        }
    }

    private static long crc(byte[] bytes) {
        CRC32C crc = new CRC32C();
        crc.update(bytes);
        return crc.getValue();
    }
}
