package dev.adellaghmari.boundarymesh.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Cloud Run IAM authenticates external callers. This layer exposes only health/readiness and the
 * private transaction ingress and selected evidence reads; Entra remains PLANNED.
 */
@Configuration
@Profile("prod")
public class ProdFoundationSecurityConfig {

    @Bean
    SecurityFilterChain prodFoundationSecurity(HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers("/api/v1/health", "/api/v1/ready")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/transactions")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/deliveries/{instructionId}")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/attestations/{id}/summary")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/attestations/{id}/verify")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/audit/chains/{integrationId}/verify")
                        .permitAll()
                        .anyRequest()
                        .denyAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(JsonAccessDeniedWriter.unauthorized())
                        .accessDeniedHandler(JsonAccessDeniedWriter.forbidden()))
                .build();
    }
}
