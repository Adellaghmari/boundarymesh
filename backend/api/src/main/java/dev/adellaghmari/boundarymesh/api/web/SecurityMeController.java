package dev.adellaghmari.boundarymesh.api.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.adellaghmari.boundarymesh.api.security.JwtToPrincipalMapper;
import dev.adellaghmari.boundarymesh.domain.identity.BoundaryPrincipal;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/security", produces = MediaType.APPLICATION_JSON_VALUE)
public class SecurityMeController {

    private final JwtToPrincipalMapper mapper;

    public SecurityMeController(JwtToPrincipalMapper mapper) {
        this.mapper = mapper;
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        BoundaryPrincipal principal = mapper.from(jwt);
        return new MeResponse(
                principal.id().value(),
                principal.type().name(),
                principal.issuer().value(),
                principal.method().name(),
                List.copyOf(principal.scopes()),
                List.copyOf(principal.roles()),
                principal.keyId(),
                "SYNTHETIC DEMO / LOCAL issuer. Not Microsoft Entra.");
    }

    public record MeResponse(
            String subject,
            @JsonProperty("principal_type") String principalType,
            String issuer,
            String method,
            List<String> scopes,
            List<String> roles,
            @JsonProperty("key_id") String keyId,
            String truth) {}
}
