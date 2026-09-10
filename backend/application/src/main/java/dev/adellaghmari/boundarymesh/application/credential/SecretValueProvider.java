package dev.adellaghmari.boundarymesh.application.credential;

import dev.adellaghmari.boundarymesh.domain.credential.CredentialReference;
import java.util.Optional;

/**
 * Resolves secret material by reference immediately before an outbound connector call. Public API
 * must not implement this port.
 */
public interface SecretValueProvider {

    Optional<SecretMaterial> resolve(CredentialReference reference);
}
