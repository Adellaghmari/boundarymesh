package dev.adellaghmari.boundarymesh.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
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
 * <p>CSRF is disabled on this chain because it is a stateless bearer JWT / HMAC resource server.
 * Browser session mutations live on {@link AdminSessionSecurityConfig} and keep cookie CSRF.
 *
 * <p>Machine identity here remains the local JWT resource server. Entra OIDC is a separate chain.
 */
@Configuration
@Profile({"local", "test"})
public class LocalDevelopmentSecurityConfig {

    @Bean
    @Order(2)
    SecurityFilterChain localDevelopmentSecurity(HttpSecurity http) throws Exception {
        return http.securityMatcher("/api/**", "/v3/**")
                .csrf(AbstractHttpConfigurer::disable)
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
                        .requestMatchers(HttpMethod.GET, "/api/v1/audit/chains/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/attestations", "/api/v1/attestations/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/attestations/*/verify")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/boundary-graph")
                        .permitAll()
                        .requestMatchers(
                                "/api/v1/dev/audit-lab/**",
                                "/api/v1/dev/attestation-lab/**",
                                "/api/v1/dev/microsoft-lab/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/microsoft/connector")
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
