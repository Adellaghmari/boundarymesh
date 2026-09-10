package dev.adellaghmari.boundarymesh.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

class EntraOidcClientConfigTest {

    @Test
    void registrationUsesCommonV2EndpointsAndCalendarsReadOnly() {
        BoundaryMeshProperties.Entra entra = new BoundaryMeshProperties.Entra(
                "00000000-0000-0000-0000-000000000001",
                "test-client",
                "test-secret",
                "http://127.0.0.1:5173/login/oauth2/code/entra",
                "http://127.0.0.1:5173",
                "LIVE");
        ClientRegistration registration = EntraOidcClientConfig.registration(entra);
        String authority = "https://login.microsoftonline.com/common";
        assertThat(registration.getAuthorizationGrantType()).isEqualTo(AuthorizationGrantType.AUTHORIZATION_CODE);
        assertThat(registration.getProviderDetails().getAuthorizationUri())
                .isEqualTo(authority + "/oauth2/v2.0/authorize");
        assertThat(registration.getProviderDetails().getTokenUri()).isEqualTo(authority + "/oauth2/v2.0/token");
        assertThat(registration.getProviderDetails().getJwkSetUri()).isEqualTo(authority + "/discovery/v2.0/keys");
        assertThat(registration.getProviderDetails().getIssuerUri()).isNull();
        assertThat(registration.getScopes()).contains("openid", "profile", "offline_access", "Calendars.Read");
        assertThat(registration.getScopes()).doesNotContain("Calendars.ReadWrite", "Mail.Read");
        assertThat(registration.getRedirectUri()).isEqualTo("http://127.0.0.1:5173/login/oauth2/code/entra");
    }
}
