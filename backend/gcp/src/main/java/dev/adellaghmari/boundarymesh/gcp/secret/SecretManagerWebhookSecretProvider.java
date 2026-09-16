package dev.adellaghmari.boundarymesh.gcp.secret;

import dev.adellaghmari.boundarymesh.application.webhook.WebhookSecretProvider;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Secret Manager adapter for webhook HMAC material. The production API composition selects it;
 * local/test do not. Fails closed without exposing payload.
 */
public final class SecretManagerWebhookSecretProvider implements WebhookSecretProvider {

    private static final Logger log = LoggerFactory.getLogger(SecretManagerWebhookSecretProvider.class);

    private final SecretIdCatalog catalog;
    private final SecretAccessor accessor;

    public SecretManagerWebhookSecretProvider(SecretIdCatalog catalog, SecretAccessor accessor) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.accessor = Objects.requireNonNull(accessor, "accessor");
    }

    @Override
    public byte[] secretBytes(String connectorId) {
        var name = catalog.forWebhookConnector(connectorId);
        if (name.isEmpty()) {
            log.warn(
                    "webhook secret rejected kind={} connector={}",
                    SecretAccessFailureKind.INVALID_REFERENCE,
                    connectorId);
            throw SecretAccessException.invalidReference();
        }
        try {
            byte[] payload = accessor.access(name.get());
            if (payload == null || payload.length == 0) {
                log.warn(
                        "webhook secret rejected kind={} secretId={}",
                        SecretAccessFailureKind.NOT_FOUND,
                        name.get().secretId());
                throw SecretAccessException.notFound(name.get().secretId());
            }
            return payload.clone();
        } catch (SecretAccessException ex) {
            log.warn(
                    "webhook secret rejected kind={} secretId={}",
                    ex.kind(),
                    name.get().secretId());
            throw ex;
        }
    }
}
