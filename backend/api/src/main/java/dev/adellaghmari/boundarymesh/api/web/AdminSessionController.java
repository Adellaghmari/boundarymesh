package dev.adellaghmari.boundarymesh.api.web;

import dev.adellaghmari.boundarymesh.api.config.BoundaryMeshProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/admin", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminSessionController {

    private final BoundaryMeshProperties properties;

    public AdminSessionController(BoundaryMeshProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/session")
    public Map<String, Object> session(Authentication authentication) {
        Map<String, Object> body = new LinkedHashMap<>();
        boolean configured = properties.entra().configured();
        boolean signedIn = authentication instanceof OAuth2AuthenticationToken;
        body.put("entra_enabled", configured);
        body.put("authenticated", signedIn);
        body.put("login_url", "/oauth2/authorization/entra");
        body.put("logout_url", "/logout");
        body.put("graph_mode", properties.entra().liveGraph() ? "LIVE" : "HARNESS");
        body.put("graph_mode_label", properties.entra().liveGraph() ? "LIVE CONFIGURED" : "CONTRACT HARNESS");
        body.put("authority_mode", properties.entra().authorityMode());
        body.put("role_label", signedIn ? "Operator" : "Recruiter");
        body.put("provider", signedIn ? "MICROSOFT_ENTRA" : "NONE");
        if (signedIn && authentication.getPrincipal() instanceof OidcUser user) {
            body.put(
                    "subject_present",
                    user.getSubject() != null && !user.getSubject().isBlank());
        } else {
            body.put("subject_present", false);
        }
        return body;
    }
}
