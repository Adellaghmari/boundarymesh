package dev.adellaghmari.boundarymesh.gcp.secret;

import static org.assertj.core.api.Assertions.assertThat;

import dev.adellaghmari.boundarymesh.application.credential.SecretMaterial;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialId;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialReference;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialStatus;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialVersion;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Read-only ADC proof against the four synthetic BoundaryMesh Secret Manager payloads. */
@Tag("gcp-live-secrets")
class SecretManagerLiveTest {

    private static final String PROJECT = "boundarymesh-dev";
    private static final Pattern SYNTHETIC_SECRET = Pattern.compile("^[A-Za-z0-9_-]{43}$");

    @Test
    void productionAdaptersReadAllFourSyntheticSecretsWithoutExposingValues() {
        assertThat(System.getenv("GOOGLE_APPLICATION_CREDENTIALS")).isNullOrEmpty();
        SecretIdCatalog catalog = new SecretIdCatalog(PROJECT);

        try (GoogleSecretAccessor accessor = GoogleSecretAccessor.connect()) {
            for (String secretId : SecretIdCatalog.PHASE_15E_SECRET_IDS) {
                byte[] payload = accessor.access(new SecretResourceName(PROJECT, secretId, "1"));
                assertThat(valid(payload))
                        .as("direct accessor format for %s", secretId)
                        .isTrue();
            }

            var webhook = new SecretManagerWebhookSecretProvider(catalog, accessor);
            assertThat(valid(webhook.secretBytes(SecretIdCatalog.QUAYFORGE_ORDERS)))
                    .as("webhook secret provider format")
                    .isTrue();

            var worker = new SecretManagerSecretValueProvider(catalog, accessor);
            assertWorkerSecret(worker, SecretIdCatalog.COVECRM_API);
            assertWorkerSecret(worker, SecretIdCatalog.LUMENWAY_API);
            assertWorkerSecret(worker, SecretIdCatalog.RIVERTIDE_API);
        }
    }

    private static void assertWorkerSecret(SecretManagerSecretValueProvider provider, String secretId) {
        CredentialReference reference = new CredentialReference(
                new CredentialId(secretId), new CredentialVersion("1"), CredentialStatus.ACTIVE);
        Optional<SecretMaterial> resolved = provider.resolve(reference);
        assertThat(resolved.isPresent())
                .as("worker provider resolved %s", secretId)
                .isTrue();
        try (SecretMaterial secret = resolved.orElseThrow()) {
            assertThat(valid(secret.reveal()))
                    .as("worker provider format for %s", secretId)
                    .isTrue();
        }
    }

    private static boolean valid(byte[] payload) {
        return payload != null
                && payload.length == 43
                && SYNTHETIC_SECRET
                        .matcher(new String(payload, StandardCharsets.US_ASCII))
                        .matches();
    }

    private static boolean valid(String payload) {
        return payload != null && SYNTHETIC_SECRET.matcher(payload).matches();
    }
}
