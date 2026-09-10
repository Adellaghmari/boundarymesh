package dev.adellaghmari.boundarymesh.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenDecoderFactory;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenValidator;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.util.StringUtils;

@Configuration
@Profile({"local", "test"})
public class EntraOidcClientConfig {

    @Bean
    @Conditional(EntraConfiguredCondition.class)
    ClientRegistrationRepository entraClientRegistrationRepository(BoundaryMeshProperties properties) {
        return new InMemoryClientRegistrationRepository(registration(properties.entra()));
    }

    /**
     * Common v2 authorize/token/JWKS. ID token {@code iss} is tenant-specific and is validated by
     * {@link MicrosoftIdentityIssuerValidator}, not by equality to {@code .../common/v2.0}.
     */
    static ClientRegistration registration(BoundaryMeshProperties.Entra entra) {
        String authority = "https://login.microsoftonline.com/common";
        return ClientRegistration.withRegistrationId("entra")
                .clientId(entra.clientId())
                .clientSecret(entra.clientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(entra.redirectUri())
                .scope("openid", "profile", "offline_access", "Calendars.Read")
                .authorizationUri(authority + "/oauth2/v2.0/authorize")
                .tokenUri(authority + "/oauth2/v2.0/token")
                .jwkSetUri(authority + "/discovery/v2.0/keys")
                .userNameAttributeName(IdTokenClaimNames.SUB)
                .clientName("Microsoft Entra")
                .build();
    }

    static OAuth2TokenValidator<Jwt> idTokenValidators(ClientRegistration registration) {
        return new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                new MicrosoftIdentityIssuerValidator(),
                new OidcIdTokenValidator(registration));
    }

    @Bean
    @Conditional(EntraConfiguredCondition.class)
    JwtDecoderFactory<ClientRegistration> entraOidcIdTokenDecoderFactory() {
        OidcIdTokenDecoderFactory factory = new OidcIdTokenDecoderFactory();
        factory.setJwtValidatorFactory(EntraOidcClientConfig::idTokenValidators);
        return factory;
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
