package dev.adellaghmari.boundarymesh.api.webhook;

import dev.adellaghmari.boundarymesh.api.config.BoundaryMeshProperties;
import dev.adellaghmari.boundarymesh.application.webhook.WebhookSecretProvider;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LocalWebhookSecretProvider implements WebhookSecretProvider {

    private static final Logger log = LoggerFactory.getLogger(LocalWebhookSecretProvider.class);
    private final byte[] secret;

    public LocalWebhookSecretProvider(BoundaryMeshProperties properties) {
        String configured = properties.webhook().quayforgeSecret();
        if (configured != null && !configured.isBlank()) {
            this.secret = configured.getBytes(StandardCharsets.UTF_8);
            log.info("QuayForge webhook secret source=environment");
        } else {
            byte[] generated = new byte[32];
            new SecureRandom().nextBytes(generated);
            this.secret = HexFormat.of().formatHex(generated).getBytes(StandardCharsets.UTF_8);
            log.info("QuayForge webhook secret source=generated-ephemeral");
        }
    }

    @Override
    public byte[] secretBytes(String connectorId) {
        return secret.clone();
    }
}
