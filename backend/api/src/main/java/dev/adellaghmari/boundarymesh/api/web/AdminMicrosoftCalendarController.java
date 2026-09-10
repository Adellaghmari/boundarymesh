package dev.adellaghmari.boundarymesh.api.web;

import dev.adellaghmari.boundarymesh.api.config.BoundaryMeshProperties;
import dev.adellaghmari.boundarymesh.api.microsoft.MicrosoftGraphCalendarAdapter;
import dev.adellaghmari.boundarymesh.application.canon.Sha256;
import dev.adellaghmari.boundarymesh.application.microsoft.MicrosoftCalendarCrossingService;
import dev.adellaghmari.boundarymesh.application.microsoft.RetryingGraphCalendarPort;
import dev.adellaghmari.boundarymesh.domain.identity.AuthenticationMethod;
import dev.adellaghmari.boundarymesh.domain.identity.BoundaryPrincipal;
import dev.adellaghmari.boundarymesh.domain.identity.Issuer;
import dev.adellaghmari.boundarymesh.domain.identity.PrincipalId;
import dev.adellaghmari.boundarymesh.domain.identity.PrincipalType;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/admin/microsoft", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminMicrosoftCalendarController {

    private final MicrosoftCalendarCrossingService crossings;
    private final BoundaryMeshProperties properties;
    private final ObjectProvider<OAuth2AuthorizedClientRepository> authorizedClients;

    public AdminMicrosoftCalendarController(
            MicrosoftCalendarCrossingService crossings,
            BoundaryMeshProperties properties,
            ObjectProvider<OAuth2AuthorizedClientRepository> authorizedClients) {
        this.crossings = crossings;
        this.properties = properties;
        this.authorizedClients = authorizedClients;
    }

    @PostMapping("/calendar/pull")
    public Map<String, Object> pull(
            @AuthenticationPrincipal OidcUser user, Authentication authentication, HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (!properties.entra().liveGraph()) {
            body.put("mode", "CONTRACT_HARNESS");
            body.put("live", false);
            body.put("message", "Live Graph is not enabled. Use the contract harness lab.");
            return body;
        }
        OAuth2AuthorizedClient client = loadClient(authentication, request);
        String token = client == null || client.getAccessToken() == null
                ? null
                : client.getAccessToken().getTokenValue();
        var adapter = new RetryingGraphCalendarPort(new MicrosoftGraphCalendarAdapter(() -> token));
        BoundaryPrincipal principal = principal(user);
        var result = crossings.pull(principal, true, adapter);
        body.put("mode", "LIVE_VERIFIED");
        body.put("live", true);
        body.put("permission", "Calendars.Read");
        body.put("provider_status", result.providerStatus());
        body.put("count", result.crossings().size());
        body.put("banner", "LIVE MICROSOFT GRAPH DATA. Event subjects, attendees, and bodies are not returned.");
        return body;
    }

    private static BoundaryPrincipal principal(OidcUser user) {
        String sub = user == null || user.getSubject() == null ? "unknown" : user.getSubject();
        String ref = "entra-" + Sha256.hexUtf8(sub).substring(0, 24);
        String issuer = user != null && user.getIssuer() != null
                ? user.getIssuer().toString()
                : "https://login.microsoftonline.com/unknown/v2.0";
        return new BoundaryPrincipal(
                PrincipalId.of(ref),
                PrincipalType.USER,
                Issuer.of(issuer),
                AuthenticationMethod.OIDC,
                Set.of("Calendars.Read", "boundary.evaluate"),
                Set.of("OPERATOR"),
                "entra-oidc");
    }

    private OAuth2AuthorizedClient loadClient(Authentication authentication, HttpServletRequest request) {
        OAuth2AuthorizedClientRepository repository = authorizedClients.getIfAvailable();
        if (repository == null || authentication == null) {
            return null;
        }
        return repository.loadAuthorizedClient("entra", authentication, request);
    }
}
