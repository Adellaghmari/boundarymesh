package dev.adellaghmari.boundarymesh.gcp.secret;

import dev.adellaghmari.boundarymesh.application.credential.SecretValueProvider;
import dev.adellaghmari.boundarymesh.application.webhook.WebhookSecretProvider;
import dev.adellaghmari.boundarymesh.gcp.GcpRuntimeMode;
import java.util.Objects;

/**
 * Profile/mode composition for secret ports. LOCAL, TEST, and GCP_EMULATOR keep local providers.
 * Secret Manager has no emulator. PROD may select Secret Manager adapters when API/worker later
 * import this module under an explicit cloud profile. Phase 15E does not activate PROD.
 */
public final class SecretProviderSelection {

    private SecretProviderSelection() {}

    public static boolean usesSecretManager(GcpRuntimeMode mode) {
        return mode == GcpRuntimeMode.PROD;
    }

    public static SecretValueProvider secretValueProvider(
            GcpRuntimeMode mode, SecretValueProvider local, SecretValueProvider secretManager) {
        Objects.requireNonNull(local, "local");
        Objects.requireNonNull(secretManager, "secretManager");
        return usesSecretManager(mode) ? secretManager : local;
    }

    public static WebhookSecretProvider webhookSecretProvider(
            GcpRuntimeMode mode, WebhookSecretProvider local, WebhookSecretProvider secretManager) {
        Objects.requireNonNull(local, "local");
        Objects.requireNonNull(secretManager, "secretManager");
        return usesSecretManager(mode) ? secretManager : local;
    }
}
