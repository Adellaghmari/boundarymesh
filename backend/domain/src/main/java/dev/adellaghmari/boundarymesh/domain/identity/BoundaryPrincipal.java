package dev.adellaghmari.boundarymesh.domain.identity;

import java.util.Objects;
import java.util.Set;

/**
 * Framework-independent authenticated principal. Adapters map Spring Security Authentication here.
 * Never holds a raw token.
 */
public record BoundaryPrincipal(
        PrincipalId id,
        PrincipalType type,
        Issuer issuer,
        AuthenticationMethod method,
        Set<String> scopes,
        Set<String> roles,
        String keyId) {

    public BoundaryPrincipal {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(issuer, "issuer");
        Objects.requireNonNull(method, "method");
        scopes = scopes == null ? Set.of() : Set.copyOf(scopes);
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }

    public boolean hasScope(BoundaryPermission permission) {
        return scopes.contains(permission.scope());
    }
}
