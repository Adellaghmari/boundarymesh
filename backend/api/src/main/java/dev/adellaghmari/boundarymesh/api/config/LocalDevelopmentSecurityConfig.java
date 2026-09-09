package dev.adellaghmari.boundarymesh.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * LOCAL-ONLY development security. This is not Entra, OAuth2, OIDC, or JWT validation.
 * Those remain PLANNED until their dedicated phases.
 */
@Configuration
@Profile({"local", "test"})
public class LocalDevelopmentSecurityConfig {

    @Bean
    SecurityFilterChain localDevelopmentSecurity(HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers(
                                "/api/v1/health", "/api/v1/ready", "/api/v1/dev/**", "/v3/api-docs", "/v3/api-docs/**")
                        .permitAll()
                        .anyRequest()
                        .denyAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(JsonAccessDeniedWriter.unauthorized())
                        .accessDeniedHandler(JsonAccessDeniedWriter.forbidden()))
                .build();
    }
}
