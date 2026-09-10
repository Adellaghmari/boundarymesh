package dev.adellaghmari.boundarymesh.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.util.StringUtils;

@Configuration
@Profile({"local", "test"})
public class EntraOidcClientConfig {

    @Bean
    @Conditional(EntraConfiguredCondition.class)
    ClientRegistrationRepository entraClientRegistrationRepository(BoundaryMeshProperties properties) {
        BoundaryMeshProperties.Entra entra = properties.entra();
        ClientRegistration registration = ClientRegistration.withRegistrationId("entra")
                .clientId(entra.clientId())
                .clientSecret(entra.clientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(entra.redirectUri())
                .scope("openid", "profile", "offline_access", "Calendars.Read")
                .issuerUri("https://login.microsoftonline.com/" + entra.tenantId() + "/v2.0")
                .userNameAttributeName(IdTokenClaimNames.SUB)
                .clientName("Microsoft Entra")
                .build();
        return new InMemoryClientRegistrationRepository(registration);
    }

    @Bean
    @Conditional(EntraConfiguredCondition.class)
    OAuth2AuthorizedClientRepository authorizedClientRepository() {
        return new HttpSessionOAuth2AuthorizedClientRepository();
    }

    static final class EntraConfiguredCondition implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            var env = context.getEnvironment();
            return StringUtils.hasText(env.getProperty("boundarymesh.entra.tenant-id"))
                    && StringUtils.hasText(env.getProperty("boundarymesh.entra.client-id"))
                    && StringUtils.hasText(env.getProperty("boundarymesh.entra.client-secret"));
        }
    }
}
