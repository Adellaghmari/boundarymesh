package dev.adellaghmari.boundarymesh.gcp.secret;

import dev.adellaghmari.boundarymesh.domain.credential.CredentialReference;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Phase 15E secret ID inventory. Maps existing credential/webhook identifiers onto Secret Manager
 * containers. Does not include Entra or Graph subscription secrets.
 */
public final class SecretIdCatalog {

    public static final String HMAC_QUAYFORGE = "hmac-quayforge";
    public static final String COVECRM_API = "covecrm-api";
    public static final String LUMENWAY_API = "lumenway-api";
    public static final String RIVERTIDE_API = "rivertide-api";
    public static final String QUAYFORGE_ORDERS = "quayforge-orders";

    public static final Set<String> PHASE_15E_SECRET_IDS =
            Set.of(HMAC_QUAYFORGE, COVECRM_API, LUMENWAY_API, RIVERTIDE_API);

    private static final Map<String, String> CREDENTIAL_SECRETS = Map.of(
            COVECRM_API, COVECRM_API,
            LUMENWAY_API, LUMENWAY_API,
            RIVERTIDE_API, RIVERTIDE_API);

    private static final Map<String, String> WEBHOOK_SECRETS = Map.of(QUAYFORGE_ORDERS, HMAC_QUAYFORGE);

    private final String projectId;

    public SecretIdCatalog(String projectId) {
        this.projectId = projectId;
    }

    public Optional<SecretResourceName> forCredential(CredentialReference reference) {
        if (reference == null) {
            return Optional.empty();
        }
        String secretId = CREDENTIAL_SECRETS.get(reference.id().value());
        if (secretId == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(new SecretResourceName(
                    projectId, secretId, reference.version().value()));
        } catch (SecretAccessException ex) {
            return Optional.empty();
        }
    }

    public Optional<SecretResourceName> forWebhookConnector(String connectorId) {
        if (connectorId == null || connectorId.isBlank()) {
            return Optional.empty();
        }
        String secretId = WEBHOOK_SECRETS.get(connectorId.trim());
        if (secretId == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(SecretResourceName.latest(projectId, secretId));
        } catch (SecretAccessException ex) {
            return Optional.empty();
        }
    }
}
