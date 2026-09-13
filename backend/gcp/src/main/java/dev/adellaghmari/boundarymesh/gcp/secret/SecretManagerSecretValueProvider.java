package dev.adellaghmari.boundarymesh.gcp.secret;

import dev.adellaghmari.boundarymesh.application.credential.SecretMaterial;
import dev.adellaghmari.boundarymesh.application.credential.SecretValueProvider;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialReference;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialStatus;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Worker-facing Secret Manager adapter. Fail closed: missing, denied, or unmapped references become
 * empty. Never logs payload.
 */
public final class SecretManagerSecretValueProvider implements SecretValueProvider {

    private static final Logger log = LoggerFactory.getLogger(SecretManagerSecretValueProvider.class);

    private final SecretIdCatalog catalog;
    private final SecretAccessor accessor;

    public SecretManagerSecretValueProvider(SecretIdCatalog catalog, SecretAccessor accessor) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.accessor = Objects.requireNonNull(accessor, "accessor");
    }

    @Override
    public Optional<SecretMaterial> resolve(CredentialReference reference) {
        if (reference == null || reference.status() != CredentialStatus.ACTIVE) {
            return Optional.empty();
        }
        Optional<SecretResourceName> name = catalog.forCredential(reference);
        if (name.isEmpty()) {
            log.warn(
                    "secret resolve rejected kind={} credentialId={}",
                    SecretAccessFailureKind.INVALID_REFERENCE,
                    safeId(reference));
            return Optional.empty();
        }
        try {
            byte[] payload = accessor.access(name.get());
            if (payload == null || payload.length == 0) {
                log.warn(
                        "secret resolve rejected kind={} secretId={}",
                        SecretAccessFailureKind.NOT_FOUND,
                        name.get().secretId());
                return Optional.empty();
            }
            return Optional.of(SecretMaterial.fromPlain(new String(payload, StandardCharsets.UTF_8)));
        } catch (SecretAccessException ex) {
            log.warn(
                    "secret resolve rejected kind={} secretId={}",
                    ex.kind(),
                    name.get().secretId());
            return Optional.empty();
        }
    }

    private static String safeId(CredentialReference reference) {
        return reference.id().value();
    }
}
