package dev.adellaghmari.boundarymesh.domain.credential;

import java.util.Objects;

/** Versioned pointer to connector secret material. Never holds the secret value. */
public record CredentialReference(CredentialId id, CredentialVersion version, CredentialStatus status) {

    public CredentialReference {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(status, "status");
    }

    public String display() {
        return id.value() + ":v" + version.value();
    }
}
