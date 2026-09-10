package dev.adellaghmari.boundarymesh.domain.credential;

import java.time.Instant;
import java.util.Objects;

/** Safe credential metadata. Secret material is never stored here. */
public record CredentialRecord(
        CredentialReference reference, ConnectorIdentity connector, String destination, Instant rotatedAt) {

    public CredentialRecord {
        Objects.requireNonNull(reference, "reference");
        Objects.requireNonNull(connector, "connector");
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(rotatedAt, "rotatedAt");
    }
}
