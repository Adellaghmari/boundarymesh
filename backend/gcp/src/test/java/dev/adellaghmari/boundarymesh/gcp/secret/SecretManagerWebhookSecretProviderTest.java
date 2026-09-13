package dev.adellaghmari.boundarymesh.gcp.secret;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class SecretManagerWebhookSecretProviderTest {

    private static final String FAKE = "TEST-SM-WEBHOOK-FAKE-VALUE-NOT-A-SECRET";

    private final SecretIdCatalog catalog = new SecretIdCatalog("boundarymesh-dev");
    private final FakeSecretAccessor accessor = new FakeSecretAccessor();
    private final SecretManagerWebhookSecretProvider provider =
            new SecretManagerWebhookSecretProvider(catalog, accessor);

    @Test
    void returnsBytesForMappedWebhookConnector() {
        SecretResourceName name =
                catalog.forWebhookConnector("quayforge-orders").orElseThrow();
        accessor.put(name, FAKE);

        assertThat(provider.secretBytes("quayforge-orders")).isEqualTo(FAKE.getBytes(StandardCharsets.UTF_8));
        assertThat(accessor.requested())
                .containsExactly("projects/boundarymesh-dev/secrets/hmac-quayforge/versions/latest");
    }

    @Test
    void unknownConnectorAndAccessFailuresFailClosedWithoutPayload() {
        assertThatThrownBy(() -> provider.secretBytes("helix-365"))
                .isInstanceOf(SecretAccessException.class)
                .satisfies(error -> {
                    SecretAccessException ex = (SecretAccessException) error;
                    assertThat(ex.kind()).isEqualTo(SecretAccessFailureKind.INVALID_REFERENCE);
                    assertThat(ex.getMessage()).doesNotContain(FAKE);
                });

        accessor.failWith(SecretAccessException.denied("hmac-quayforge"));
        assertThatThrownBy(() -> provider.secretBytes("quayforge-orders"))
                .isInstanceOf(SecretAccessException.class)
                .satisfies(error -> assertThat(error.getMessage()).doesNotContain(FAKE));
    }
}
