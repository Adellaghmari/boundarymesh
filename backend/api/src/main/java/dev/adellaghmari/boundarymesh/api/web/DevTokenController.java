package dev.adellaghmari.boundarymesh.api.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.adellaghmari.boundarymesh.api.security.LocalTokenService;
import dev.adellaghmari.boundarymesh.api.security.TokenScenario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/dev", produces = MediaType.APPLICATION_JSON_VALUE)
@ConditionalOnProperty(name = "boundarymesh.dev-security-tools-enabled", havingValue = "true")
public class DevTokenController {

    private final LocalTokenService tokens;

    public DevTokenController(LocalTokenService tokens) {
        this.tokens = tokens;
    }

    @PostMapping(path = "/tokens", consumes = MediaType.APPLICATION_JSON_VALUE)
    public TokenResponse issue(@Valid @RequestBody TokenRequest request) {
        return new TokenResponse(
                "Bearer",
                tokens.issue(request.scenario()),
                request.scenario().name(),
                "SYNTHETIC DEMO local issuer token. Not Entra. Do not treat as a production credential.");
    }

    public record TokenRequest(@NotNull TokenScenario scenario) {}

    public record TokenResponse(
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("access_token") String accessToken,
            String scenario,
            String truth) {}
}
