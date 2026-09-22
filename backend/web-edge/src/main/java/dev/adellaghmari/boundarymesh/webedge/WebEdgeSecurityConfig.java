package dev.adellaghmari.boundarymesh.webedge;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
class WebEdgeSecurityConfig {
    @Bean
    SecurityFilterChain edgeSecurity(HttpSecurity http) throws Exception {
        var csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepository.setCookieName(CsrfGuard.COOKIE);
        csrfRepository.setHeaderName(CsrfGuard.HEADER);
        csrfRepository.setCookieCustomizer(
                cookie -> cookie.path("/").secure(true).sameSite("Lax"));
        return http.csrf(csrf -> csrf.csrfTokenRepository(csrfRepository)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedHandler(new StructuredAccessDeniedHandler())
                        .authenticationEntryPoint(new StructuredAuthenticationEntryPoint()))
                .authorizeHttpRequests(auth -> auth.requestMatchers(
                                HttpMethod.GET, "/actuator/health", "/session/bootstrap", "/session/me")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/session/login", "/session/logout", "/api/v1/transactions")
                        .permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/deliveries/{instructionId}",
                                "/api/v1/attestations/{id}/summary",
                                "/api/v1/attestations/{id}/verify",
                                "/api/v1/audit/chains/{chainId}/verify")
                        .permitAll()
                        .anyRequest()
                        .denyAll())
                .build();
    }
}
