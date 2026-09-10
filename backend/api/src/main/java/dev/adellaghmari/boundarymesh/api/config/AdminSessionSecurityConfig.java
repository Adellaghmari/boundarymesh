package dev.adellaghmari.boundarymesh.api.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * Browser/admin session chain. Cookie CSRF is required because mutations use a server session.
 *
 * <p>Order 1 so {@code /api/v1/admin/**} is not claimed by the stateless bearer chain.
 */
@Configuration
@Profile({"local", "test"})
public class AdminSessionSecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain adminSessionSecurity(
            HttpSecurity http,
            BoundaryMeshProperties properties,
            ObjectProvider<ClientRegistrationRepository> registrations)
            throws Exception {
        CookieCsrfTokenRepository csrfRepo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepo.setCookieCustomizer(
                cookie -> cookie.sameSite("Lax").secure(false).path("/"));

        http.securityMatcher("/api/v1/admin/**", "/oauth2/**", "/login/**", "/logout")
                .cors(Customizer.withDefaults())
                .csrf(csrf ->
                        csrf.csrfTokenRepository(csrfRepo).csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.requestMatchers("/oauth2/**", "/login/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/admin/session")
                        .permitAll()
                        .requestMatchers("/logout")
                        .permitAll()
                        .requestMatchers("/api/v1/admin/**")
                        .hasAuthority("SCOPE_Calendars.Read"))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(JsonAccessDeniedWriter.unauthorized())
                        .accessDeniedHandler(JsonAccessDeniedWriter.forbidden()));

        if (registrations.getIfAvailable() != null) {
            String origin = properties.entra().frontendOrigin() == null
                    ? "http://127.0.0.1:5173"
                    : properties.entra().frontendOrigin();
            http.oauth2Login(oauth -> oauth.defaultSuccessUrl(origin + "/connections", true)
                    .failureUrl(origin + "/connections?entra=failed"));
            http.logout(logout -> logout.logoutRequestMatcher(
                            PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/logout"))
                    .logoutSuccessUrl(origin + "/")
                    .deleteCookies("JSESSIONID", "XSRF-TOKEN"));
        } else {
            http.oauth2Login(AbstractHttpConfigurer::disable);
            http.logout(AbstractHttpConfigurer::disable);
        }
        return http.build();
    }
}
