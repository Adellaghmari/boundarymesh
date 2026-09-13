package dev.adellaghmari.boundarymesh.gcp.secret;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.google.api.gax.grpc.GrpcStatusCode;
import com.google.api.gax.rpc.ApiException;
import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretPayload;
import com.google.protobuf.ByteString;
import io.grpc.Status;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GoogleSecretAccessorTest {

    private static final String FAKE = "TEST-SM-GOOGLE-FAKE-VALUE-NOT-A-SECRET";
    private static final SecretResourceName NAME = new SecretResourceName("boundarymesh-dev", "covecrm-api", "2");

    @Mock
    private SecretManagerServiceClient client;

    @Test
    void accessUsesResourceNameAndReturnsPayloadBytes() {
        when(client.accessSecretVersion(NAME.resourceName()))
                .thenReturn(AccessSecretVersionResponse.newBuilder()
                        .setPayload(SecretPayload.newBuilder().setData(ByteString.copyFromUtf8(FAKE)))
                        .build());

        byte[] value = new GoogleSecretAccessor(client).access(NAME);

        assertThat(value).isEqualTo(FAKE.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test
    void mapsNotFoundAndDeniedWithoutLeakingPayload() {
        assertThat(GoogleSecretAccessor.translate("covecrm-api", api(Status.Code.NOT_FOUND))
                        .kind())
                .isEqualTo(SecretAccessFailureKind.NOT_FOUND);
        assertThat(GoogleSecretAccessor.translate("covecrm-api", api(Status.Code.PERMISSION_DENIED))
                        .kind())
                .isEqualTo(SecretAccessFailureKind.PERMISSION_DENIED);
        assertThat(GoogleSecretAccessor.translate("covecrm-api", api(Status.Code.UNAVAILABLE))
                        .kind())
                .isEqualTo(SecretAccessFailureKind.PROVIDER_UNAVAILABLE);

        SecretAccessException denied =
                GoogleSecretAccessor.translate("covecrm-api", api(Status.Code.PERMISSION_DENIED));
        assertThat(denied.getMessage()).doesNotContain(FAKE);
        assertThat(denied.toString()).doesNotContain(FAKE);
    }

    @Test
    void clientFailureFailsClosed() {
        when(client.accessSecretVersion(NAME.resourceName())).thenThrow(api(Status.Code.NOT_FOUND));

        assertThatThrownBy(() -> new GoogleSecretAccessor(client).access(NAME))
                .isInstanceOf(SecretAccessException.class)
                .satisfies(error -> {
                    SecretAccessException ex = (SecretAccessException) error;
                    assertThat(ex.kind()).isEqualTo(SecretAccessFailureKind.NOT_FOUND);
                    assertThat(ex.getMessage()).doesNotContain(FAKE);
                });
    }

    private static ApiException api(Status.Code code) {
        return new ApiException(null, GrpcStatusCode.of(code), false);
    }
}
