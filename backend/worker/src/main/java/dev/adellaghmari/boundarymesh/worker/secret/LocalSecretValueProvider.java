package dev.adellaghmari.boundarymesh.worker.secret;

import dev.adellaghmari.boundarymesh.application.credential.SecretMaterial;
import dev.adellaghmari.boundarymesh.application.credential.SecretValueProvider;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialReference;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Ephemeral synthetic secrets. Not Google Secret Manager. Values are unmistakably non-production
 * and must never leave this worker.
 */
@Component
public class LocalSecretValueProvider implements SecretValueProvider {

    public static final String COVECRM_V1 = "TEST-SECRET-COVECRM-V1-NOT-FOR-PRODUCTION";
    public static final String COVECRM_V2 = "TEST-SECRET-COVECRM-V2-NOT-FOR-PRODUCTION";
    public static final String LUMENWAY_V1 = "TEST-SECRET-LUMENWAY-V1-NOT-FOR-PRODUCTION";
    public static final String RIVERTIDE_V1 = "TEST-SECRET-RIVERTIDE-V1-NOT-FOR-PRODUCTION";

    private static final Map<String, String> SECRETS = Map.of(
            "covecrm-api:1", COVECRM_V1,
            "covecrm-api:2", COVECRM_V2,
            "lumenway-api:1", LUMENWAY_V1,
            "rivertide-api:1", RIVERTIDE_V1);

    @Override
    public Optional<SecretMaterial> resolve(CredentialReference reference) {
        if (reference == null
                || reference.status() != dev.adellaghmari.boundarymesh.domain.credential.CredentialStatus.ACTIVE) {
            return Optional.empty();
        }
        String key = reference.id().value() + ":" + reference.version().value();
        String value = SECRETS.get(key);
        return value == null ? Optional.empty() : Optional.of(SecretMaterial.fromPlain(value));
    }
}
