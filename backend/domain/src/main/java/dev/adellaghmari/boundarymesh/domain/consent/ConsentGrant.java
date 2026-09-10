package dev.adellaghmari.boundarymesh.domain.consent;

import dev.adellaghmari.boundarymesh.domain.credential.ConnectorIdentity;
import java.util.Objects;
import java.util.Set;

/**
 * Connector permission grant. Distinct from credential resolvability and from inbound JWT
 * authorization.
 */
public record ConsentGrant(
        ConsentId id,
        ConnectorIdentity connector,
        String destination,
        ConnectorOperation operation,
        Set<Permission> permissions,
        ConsentStatus status,
        GrantKind kind,
        String tenantRef) {

    public ConsentGrant {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(connector, "connector");
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(operation, "operation");
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(kind, "kind");
    }

    public boolean permits(Permission required) {
        return status == ConsentStatus.GRANTED && permissions.contains(required);
    }
}
