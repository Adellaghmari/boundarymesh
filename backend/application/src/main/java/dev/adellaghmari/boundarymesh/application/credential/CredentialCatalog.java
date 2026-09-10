package dev.adellaghmari.boundarymesh.application.credential;

import dev.adellaghmari.boundarymesh.domain.credential.CredentialRecord;
import java.util.List;
import java.util.Optional;

public interface CredentialCatalog {

    Optional<CredentialRecord> activeForDestination(String destination);

    Optional<CredentialRecord> find(String credentialId, String version);

    List<CredentialRecord> list();
}
