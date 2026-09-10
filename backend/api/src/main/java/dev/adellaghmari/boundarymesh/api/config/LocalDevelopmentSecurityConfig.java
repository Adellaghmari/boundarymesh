package dev.adellaghmari.boundarymesh.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Local/test resource-server security.
 *
 * <p>Spring Security owns HTTP authentication and route authorization. The Boundary Enforcement
 * Engine owns domain crossing outcomes after a request is admitted.
 *
 * <p>CSRF is disabled because this API is a stateless bearer JWT / HMAC resource server and does
 * not use cookie sessions. A future Entra OIDC BFF must re-enable CSRF for browser cookies.
 *
 * <p>This is not Microsoft Entra and not an interactive OIDC login.
 */
@Configuration
@Profile({"local", "test"})
public class LocalDevelopmentSecurityConfig {

    @Bean
    SecurityFilterChain localDevelopmentSecurity(HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .headers(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(JsonAccessDeniedWriter.unauthorized())
                        .accessDeniedHandler(JsonAccessDeniedWriter.forbidden()))
                .authorizeHttpRequests(auth -> auth.requestMatchers("/api/v1/health", "/api/v1/ready")
                        .permitAll()
                        .requestMatchers("/v3/api-docs", "/v3/api-docs/**")
                        .permitAll()
                        .requestMatchers("/api/v1/dev/jwks", "/api/v1/dev/issuer")
                        .permitAll()
                        .requestMatchers(
                                "/api/v1/dev/tokens",
                                "/api/v1/dev/identity-lab/**",
                                "/api/v1/dev/webhook-lab/**",
                                "/api/v1/dev/contract-lab/**",
                                "/api/v1/dev/egress-lab/**",
                                "/api/v1/dev/credential-lab/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/quarantine", "/api/v1/quarantine/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/webhooks/quayforge")
                        .permitAll()
                        .requestMatchers("/api/v1/security/me")
                        .authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/identity/evaluate")
                        .authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/dev/evaluate")
                        .hasAuthority("SCOPE_boundary.evaluate")
                        .requestMatchers(HttpMethod.POST, "/api/v1/dev/crossings/evaluate")
                        .hasAuthority("SCOPE_boundary.evaluate")
                        .requestMatchers(HttpMethod.POST, "/api/v1/dev/delivery-lab/**")
                        .hasAuthority("SCOPE_boundary.evaluate")
                        .requestMatchers(HttpMethod.POST, "/api/v1/quarantine/*/reprocess")
                        .hasAuthority("SCOPE_boundary.admin")
                        .anyRequest()
                        .denyAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(JsonAccessDeniedWriter.unauthorized())
                        .accessDeniedHandler(JsonAccessDeniedWriter.forbidden()))
                .build();
    }
}
