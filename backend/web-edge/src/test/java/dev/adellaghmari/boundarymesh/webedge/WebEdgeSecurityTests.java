package dev.adellaghmari.boundarymesh.webedge;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {SessionController.class, ApiProxyController.class})
@Import({WebEdgeSecurityConfig.class, WebEdgeSecurityTests.TestBeans.class})
@ImportAutoConfiguration({SecurityAutoConfiguration.class, ServletWebSecurityAutoConfiguration.class})
class WebEdgeSecurityTests {
    private static final String CSRF = "0123456789012345678901234567890123456789012";
    private static final String INSTRUCTION = "dlv-ef133a64-fe3c-4cd3-afc6-6c4486f3dd00";

    @TestConfiguration(proxyBeanMethods = false)
    static class TestBeans {
        @Bean
        OperatorPolicy operatorPolicy() {
            return new OperatorPolicy("approved-uid");
        }

        @Bean
        CsrfGuard csrfGuard() {
            return new CsrfGuard();
        }
    }

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AuthGateway auth;

    @MockitoBean
    CoreApiClient core;

    private AuthGateway.Principal approved() {
        return new AuthGateway.Principal(
                "approved-uid", "operator@example.invalid", Instant.now().getEpochSecond(), true);
    }

    private Cookie csrfCookie() {
        return new Cookie(CsrfGuard.COOKIE, CSRF);
    }

    private Cookie sessionCookie() {
        return new Cookie(SessionController.SESSION_COOKIE, "signed-session");
    }

    @Test
    void bootstrapIssuesSecureReadableCsrfCookie() throws Exception {
        mvc.perform(get("/session/bootstrap"))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Secure")))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    }

    @Test
    void unauthenticatedAndRevokedSessionsAreDenied() throws Exception {
        mvc.perform(get("/api/v1/deliveries/" + INSTRUCTION)).andExpect(status().isUnauthorized());
        given(auth.verifySessionCookie("revoked")).willThrow(new InvalidSessionException());
        mvc.perform(get("/session/me").cookie(new Cookie(SessionController.SESSION_COOKIE, "revoked")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginRequiresCsrfAndExplicitOperatorApproval() throws Exception {
        mvc.perform(post("/session/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"token\"}"))
                .andExpect(status().isForbidden());
        given(auth.verifyIdToken("token"))
                .willReturn(new AuthGateway.Principal(
                        "not-approved", "x@example.invalid", Instant.now().getEpochSecond(), true));
        mvc.perform(post("/session/login")
                        .cookie(csrfCookie())
                        .header(CsrfGuard.HEADER, CSRF)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"token\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error_code").value("OPERATOR_NOT_AUTHORIZED"));
    }

    @Test
    void validLoginIssuesSecureHttpOnlyBoundedSession() throws Exception {
        given(auth.verifyIdToken("token")).willReturn(approved());
        given(auth.createSessionCookie(eq("token"), anyLong())).willReturn("signed-session");
        mvc.perform(post("/session/login")
                        .cookie(csrfCookie())
                        .header(CsrfGuard.HEADER, CSRF)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"token\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                                "Set-Cookie",
                                org.hamcrest.Matchers.allOf(
                                        org.hamcrest.Matchers.containsString("HttpOnly"),
                                        org.hamcrest.Matchers.containsString("Secure"),
                                        org.hamcrest.Matchers.containsString("SameSite=Lax"))));
    }

    @Test
    void oldSignInIsRejected() throws Exception {
        given(auth.verifyIdToken("old"))
                .willReturn(new AuthGateway.Principal("approved-uid", "operator@example.invalid", 1, true));
        mvc.perform(post("/session/login")
                        .cookie(csrfCookie())
                        .header(CsrfGuard.HEADER, CSRF)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"old\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void mutationNeedsValidCsrfAndForwardsOnlyAnAllowlistedRoute() throws Exception {
        given(auth.verifySessionCookie("signed-session")).willReturn(approved());
        byte[] body = "{\"synthetic\":true}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        mvc.perform(post("/api/v1/transactions")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/transactions")
                        .cookie(sessionCookie(), csrfCookie())
                        .header(CsrfGuard.HEADER, "invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
        given(core.call(eq("POST"), eq("/api/v1/transactions"), any(byte[].class)))
                .willReturn(new CoreApiClient.CoreResponse(202, "{\"status\":\"ACCEPTED\"}"));
        mvc.perform(post("/api/v1/transactions")
                        .cookie(sessionCookie(), csrfCookie())
                        .header(CsrfGuard.HEADER, CSRF)
                        .header("Authorization", "Bearer browser-controlled")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted());
        verify(core).call(eq("POST"), eq("/api/v1/transactions"), any(byte[].class));
    }

    @Test
    void safeReadIsAllowlistedButDevAndAdminPathsAreDenied() throws Exception {
        given(auth.verifySessionCookie("signed-session")).willReturn(approved());
        given(core.call(eq("GET"), eq("/api/v1/deliveries/" + INSTRUCTION), eq(null)))
                .willReturn(new CoreApiClient.CoreResponse(200, "{\"state\":\"QUARANTINED\"}"));
        mvc.perform(get("/api/v1/deliveries/" + INSTRUCTION).cookie(sessionCookie()))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/dev/evaluate")
                        .cookie(sessionCookie(), csrfCookie())
                        .header(CsrfGuard.HEADER, CSRF))
                .andExpect(status().is4xxClientError());
        mvc.perform(get("/api/v1/admin/session").cookie(sessionCookie())).andExpect(status().is4xxClientError());
        mvc.perform(get("/api/v1/attestations").cookie(sessionCookie())).andExpect(status().is4xxClientError());
    }

    @Test
    void logoutRevokesAndClearsSession() throws Exception {
        given(auth.verifySessionCookie("signed-session")).willReturn(approved());
        mvc.perform(post("/session/logout")
                        .cookie(sessionCookie(), csrfCookie())
                        .header(CsrfGuard.HEADER, CSRF))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));
        verify(auth).revokeSessions("approved-uid");
    }
}
