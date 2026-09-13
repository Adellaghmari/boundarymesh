package dev.adellaghmari.boundarymesh.gcp.secret;

import static org.assertj.core.api.Assertions.assertThat;

import dev.adellaghmari.boundarymesh.application.credential.SecretMaterial;
import dev.adellaghmari.boundarymesh.application.credential.SecretValueProvider;
import dev.adellaghmari.boundarymesh.application.webhook.WebhookSecretProvider;
import dev.adellaghmari.boundarymesh.gcp.GcpRuntimeMode;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SecretProviderSelectionTest {

    private final SecretValueProvider localSecrets = reference -> Optional.of(SecretMaterial.fromPlain("local"));
    private final SecretValueProvider cloudSecrets = reference -> Optional.of(SecretMaterial.fromPlain("cloud"));
    private final WebhookSecretProvider localWebhook = connectorId -> new byte[] {1};
    private final WebhookSecretProvider cloudWebhook = connectorId -> new byte[] {2};

    @Test
    void onlyProdSelectsSecretManager() {
        assertThat(SecretProviderSelection.usesSecretManager(GcpRuntimeMode.LOCAL))
                .isFalse();
        assertThat(SecretProviderSelection.usesSecretManager(GcpRuntimeMode.TEST))
                .isFalse();
        assertThat(SecretProviderSelection.usesSecretManager(GcpRuntimeMode.GCP_EMULATOR))
                .isFalse();
        assertThat(SecretProviderSelection.usesSecretManager(GcpRuntimeMode.PROD))
                .isTrue();
    }

    @Test
    void localAndEmulatorKeepLocalProviders() {
        assertThat(SecretProviderSelection.secretValueProvider(GcpRuntimeMode.LOCAL, localSecrets, cloudSecrets))
                .isSameAs(localSecrets);
        assertThat(SecretProviderSelection.secretValueProvider(GcpRuntimeMode.TEST, localSecrets, cloudSecrets))
                .isSameAs(localSecrets);
        assertThat(SecretProviderSelection.secretValueProvider(GcpRuntimeMode.GCP_EMULATOR, localSecrets, cloudSecrets))
                .isSameAs(localSecrets);
        assertThat(SecretProviderSelection.webhookSecretProvider(GcpRuntimeMode.LOCAL, localWebhook, cloudWebhook))
                .isSameAs(localWebhook);
    }

    @Test
    void prodCanSelectSecretManagerProviders() {
        assertThat(SecretProviderSelection.secretValueProvider(GcpRuntimeMode.PROD, localSecrets, cloudSecrets))
                .isSameAs(cloudSecrets);
        assertThat(SecretProviderSelection.webhookSecretProvider(GcpRuntimeMode.PROD, localWebhook, cloudWebhook))
                .isSameAs(cloudWebhook);
    }
}
