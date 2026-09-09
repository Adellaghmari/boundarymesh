package dev.adellaghmari.boundarymesh.api.web;

import com.nimbusds.jose.jwk.JWKSet;
import dev.adellaghmari.boundarymesh.api.config.BoundaryMeshProperties;
import dev.adellaghmari.boundarymesh.api.security.LocalSigningKeys;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/dev", produces = MediaType.APPLICATION_JSON_VALUE)
@ConditionalOnProperty(name = "boundarymesh.dev-security-tools-enabled", havingValue = "true")
public class DevJwksController {

    private final LocalSigningKeys keys;
    private final BoundaryMeshProperties properties;

    public DevJwksController(LocalSigningKeys keys, BoundaryMeshProperties properties) {
        this.keys = keys;
        this.properties = properties;
    }

    @GetMapping("/jwks")
    public Map<String, Object> jwks() {
        JWKSet set = keys.publicJwkSet();
        return set.toJSONObject();
    }

    @GetMapping("/issuer")
    public Map<String, String> issuer() {
        return Map.of(
                "issuer",
                properties.security().issuer(),
                "jwks_uri",
                "http://127.0.0.1:8080/api/v1/dev/jwks",
                "truth",
                "Local test issuer metadata only. Not Microsoft Entra discovery.");
    }
}
