package dev.adellaghmari.boundarymesh.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

class MicrosoftIdentityIssuerValidatorTest {

    private static final String ORG_ISSUER =
            "https://login.microsoftonline.com/00000000-0000-0000-0000-000000000001/v2.0";
    private static final String MSA_ISSUER =
            "https://login.microsoftonline.com/9188040d-6c67-4c5b-b112-36a304b66dad/v2.0";

    @Test
    void acceptsOrganizationalTenantIssuer() {
        assertThat(new MicrosoftIdentityIssuerValidator()
                        .validate(jwt(ORG_ISSUER, "test-client", future()))
                        .hasErrors())
                .isFalse();
    }

    @Test
    void acceptsPersonalMicrosoftAccountIssuer() {
        assertThat(new MicrosoftIdentityIssuerValidator()
                        .validate(jwt(MSA_ISSUER, "test-client", future()))
                        .hasErrors())
                .isFalse();
    }

    @Test
    void rejectsCommonPlaceholderIssuer() {
        assertThat(new MicrosoftIdentityIssuerValidator()
                        .validate(jwt("https://login.microsoftonline.com/common/v2.0", "test-client", future()))
                        .hasErrors())
                .isTrue();
    }

    @Test
    void rejectsConsumersPlaceholderIssuer() {
        assertThat(new MicrosoftIdentityIssuerValidator()
                        .validate(jwt("https://login.microsoftonline.com/consumers/v2.0", "test-client", future()))
                        .hasErrors())
                .isTrue();
    }

    @Test
    void rejectsV1WindowsIssuer() {
        assertThat(new MicrosoftIdentityIssuerValidator()
                        .validate(jwt(
                                "https://sts.windows.net/00000000-0000-0000-0000-000000000001/",
                                "test-client",
                                future()))
                        .hasErrors())
                .isTrue();
    }

    @Test
    void rejectsUntrustedHost() {
        assertThat(new MicrosoftIdentityIssuerValidator()
                        .validate(jwt(
                                "https://evil.example/00000000-0000-0000-0000-000000000001/v2.0",
                                "test-client",
                                future()))
                        .hasErrors())
                .isTrue();
    }

    @Test
    void rejectsHttpIssuer() {
        assertThat(new MicrosoftIdentityIssuerValidator()
                        .validate(jwt(
                                "http://login.microsoftonline.com/00000000-0000-0000-0000-000000000001/v2.0",
                                "test-client",
                                future()))
                        .hasErrors())
                .isTrue();
    }

    @Test
    void composedValidatorsRejectWrongAudience() {
        ClientRegistration registration = EntraOidcClientConfig.registration(entra());
        OAuth2TokenValidator<Jwt> validators = EntraOidcClientConfig.idTokenValidators(registration);
        OAuth2TokenValidatorResult result = validators.validate(jwt(ORG_ISSUER, "other-client", future()));
        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    void composedValidatorsRejectExpiredToken() {
        ClientRegistration registration = EntraOidcClientConfig.registration(entra());
        OAuth2TokenValidator<Jwt> validators = EntraOidcClientConfig.idTokenValidators(registration);
        Instant expired = Instant.parse("2020-01-01T00:00:00Z");
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuer(ORG_ISSUER)
                .audience(List.of("test-client"))
                .subject("subject")
                .issuedAt(expired.minusSeconds(60))
                .expiresAt(expired)
                .build();
        assertThat(validators.validate(jwt).hasErrors()).isTrue();
    }

    @Test
    void composedValidatorsAcceptMatchingOrgToken() {
        ClientRegistration registration = EntraOidcClientConfig.registration(entra());
        OAuth2TokenValidator<Jwt> validators = EntraOidcClientConfig.idTokenValidators(registration);
        assertThat(validators.validate(jwt(ORG_ISSUER, "test-client", future())).hasErrors())
                .isFalse();
    }

    @Test
    void composedValidatorsAcceptMatchingPersonalAccountToken() {
        ClientRegistration registration = EntraOidcClientConfig.registration(entra());
        OAuth2TokenValidator<Jwt> validators = EntraOidcClientConfig.idTokenValidators(registration);
        assertThat(validators.validate(jwt(MSA_ISSUER, "test-client", future())).hasErrors())
                .isFalse();
    }

    private static BoundaryMeshProperties.Entra entra() {
        return new BoundaryMeshProperties.Entra(
                "00000000-0000-0000-0000-000000000001",
                "test-client",
                "test-secret",
                "http://127.0.0.1:5173/login/oauth2/code/entra",
                "http://127.0.0.1:5173",
                "LIVE");
    }

    private static Instant future() {
        return Instant.now().plusSeconds(3600);
    }

    private static Jwt jwt(String issuer, String audience, Instant exp) {
        Instant issuedAt = Instant.now().minusSeconds(60);
        Instant expiresAt = exp.isAfter(issuedAt) ? exp : issuedAt.plusSeconds(1);
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuer(issuer)
                .audience(List.of(audience))
                .subject("subject")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
    }
}
