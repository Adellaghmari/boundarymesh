package dev.adellaghmari.boundarymesh.api.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import dev.adellaghmari.boundarymesh.api.config.BoundaryMeshProperties;
import dev.adellaghmari.boundarymesh.domain.identity.BoundaryPermission;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile({"local", "test"})
public class LocalTokenService {

    private final LocalSigningKeys keys;
    private final BoundaryMeshProperties properties;
    private final Clock clock;

    public LocalTokenService(LocalSigningKeys keys, BoundaryMeshProperties properties, Clock clock) {
        this.keys = keys;
        this.properties = properties;
        this.clock = clock;
    }

    public String issue(TokenScenario scenario) {
        Instant now = clock.instant();
        Instant exp = now.plus(properties.security().accessTokenTtl());
        Instant nbf = now.minusSeconds(5);
        String issuer = properties.security().issuer();
        String audience = properties.security().audience();
        String scope = BoundaryPermission.BOUNDARY_EVALUATE.scope();
        RSAKey signingKey = keys.current();

        switch (scenario) {
            case EXPIRED -> exp = now.minusSeconds(120);
            case WRONG_ISSUER -> issuer = "https://issuer.example.invalid/local";
            case WRONG_AUDIENCE -> audience = "some-other-api";
            case UNKNOWN_KID -> signingKey = unknownKey();
            case MISSING_PERMISSION -> scope = BoundaryPermission.BOUNDARY_READ.scope();
            case FUTURE_NBF -> nbf = now.plusSeconds(600);
            case VALID_ADMIN ->
                scope = BoundaryPermission.BOUNDARY_EVALUATE.scope() + " " + BoundaryPermission.BOUNDARY_ADMIN.scope();
            case VALID_MACHINE, INVALID_SIGNATURE -> {
                // defaults
            }
        }

        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .audience(audience)
                    .subject("velora-quayforge-connector")
                    .claim("scope", scope)
                    .claim("principal_type", "APPLICATION")
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(exp))
                    .notBeforeTime(Date.from(nbf))
                    .build();
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256)
                            .keyID(signingKey.getKeyID())
                            .type(JOSEObjectType.JWT)
                            .build(),
                    claims);
            jwt.sign(new RSASSASigner(signingKey));
            String serialized = jwt.serialize();
            if (scenario == TokenScenario.INVALID_SIGNATURE) {
                return tamper(serialized);
            }
            return serialized;
        } catch (JOSEException ex) {
            throw new IllegalStateException("Unable to sign local test token", ex);
        }
    }

    private static RSAKey unknownKey() {
        try {
            return new RSAKeyGenerator(2048).keyID("unknown-kid").generate();
        } catch (JOSEException ex) {
            throw new IllegalStateException("Unable to generate unknown kid", ex);
        }
    }

    private static String tamper(String jwt) {
        int lastDot = jwt.lastIndexOf('.');
        return jwt.substring(0, lastDot) + ".aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    }
}
