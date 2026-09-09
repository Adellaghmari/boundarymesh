package dev.adellaghmari.boundarymesh.api.security;

import dev.adellaghmari.boundarymesh.domain.identity.AuthenticationMethod;
import dev.adellaghmari.boundarymesh.domain.identity.BoundaryPrincipal;
import dev.adellaghmari.boundarymesh.domain.identity.Issuer;
import dev.adellaghmari.boundarymesh.domain.identity.PrincipalId;
import dev.adellaghmari.boundarymesh.domain.identity.PrincipalType;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/** Adapter: Spring Security Jwt → BoundaryMesh principal. */
@Component
public class JwtToPrincipalMapper {

    public BoundaryPrincipal from(Jwt jwt) {
        Set<String> scopes = scopes(jwt);
        Object kid = jwt.getHeaders().get("kid");
        return new BoundaryPrincipal(
                PrincipalId.of(jwt.getSubject()),
                PrincipalType.APPLICATION,
                Issuer.of(jwt.getIssuer().toString()),
                AuthenticationMethod.JWT_BEARER,
                scopes,
                Set.of(),
                kid instanceof String value ? value : null);
    }

    private static Set<String> scopes(Jwt jwt) {
        Set<String> scopes = new LinkedHashSet<>();
        Object scope = jwt.getClaim("scope");
        if (scope instanceof String text) {
            for (String part : text.split(" ")) {
                if (!part.isBlank()) {
                    scopes.add(part);
                }
            }
        }
        Object scp = jwt.getClaim("scp");
        if (scp instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof String value && !value.isBlank()) {
                    scopes.add(value);
                }
            }
        }
        return scopes;
    }
}
