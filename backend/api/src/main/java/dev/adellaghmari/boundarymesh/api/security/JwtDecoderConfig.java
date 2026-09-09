package dev.adellaghmari.boundarymesh.api.security;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import dev.adellaghmari.boundarymesh.api.config.BoundaryMeshProperties;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
@Profile({"local", "test"})
public class JwtDecoderConfig {

    @Bean
    JwtDecoder jwtDecoder(LocalSigningKeys keys, BoundaryMeshProperties properties) {
        JWKSource<SecurityContext> source = (selector, context) -> selector.select(keys.publicJwkSet());
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSource(source).build();
        OAuth2TokenValidator<Jwt> issuer =
                JwtValidators.createDefaultWithIssuer(properties.security().issuer());
        OAuth2TokenValidator<Jwt> audience = jwt -> {
            List<String> audiences = jwt.getAudience();
            if (audiences != null && audiences.contains(properties.security().audience())) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Audience does not target BoundaryMesh.", null));
        };
        OAuth2TokenValidator<Jwt> subject = jwt -> {
            if (jwt.getSubject() != null && !jwt.getSubject().isBlank()) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Subject is required.", null));
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuer, audience, subject));
        return decoder;
    }
}
