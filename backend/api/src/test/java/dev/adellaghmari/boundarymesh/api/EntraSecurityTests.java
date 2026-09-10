package dev.adellaghmari.boundarymesh.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EntraSecurityTests {

    @DynamicPropertySource
    static void isolateFromOsEntra(DynamicPropertyRegistry registry) {
        registry.add("boundarymesh.entra.tenant-id", () -> "");
        registry.add("boundarymesh.entra.client-id", () -> "");
        registry.add("boundarymesh.entra.client-secret", () -> "");
        registry.add("boundarymesh.entra.graph-mode", () -> "HARNESS");
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void recruiterMicrosoftConnectorIsPublicHarness() throws Exception {
        mockMvc.perform(get("/api/v1/microsoft/connector"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("CONTRACT_HARNESS"))
                .andExpect(jsonPath("$.live").value(false))
                .andExpect(jsonPath("$.permission").value("Calendars.Read"));
    }

    @Test
    void recruiterSessionIsAnonymous() throws Exception {
        mockMvc.perform(get("/api/v1/admin/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false))
                .andExpect(jsonPath("$.entra_enabled").value(false))
                .andExpect(jsonPath("$.provider").value("NONE"))
                .andExpect(jsonPath("$.authority_mode").value("NONE"));
    }

    @Test
    void adminPullRequiresLogin() throws Exception {
        mockMvc.perform(post("/api/v1/admin/microsoft/calendar/pull").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPullWithoutCalendarsReadIsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/admin/microsoft/calendar/pull")
                        .with(oauth2Login())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPullWithScopeStillRequiresCsrf() throws Exception {
        mockMvc.perform(post("/api/v1/admin/microsoft/calendar/pull")
                        .with(oauth2Login().authorities(new SimpleGrantedAuthority("SCOPE_Calendars.Read")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPullAcceptsRawXsrfCookieCopiedToHeader() throws Exception {
        MvcResult session = mockMvc.perform(get("/api/v1/admin/session")).andReturn();
        Cookie xsrf = session.getResponse().getCookie("XSRF-TOKEN");
        assertThat(xsrf).isNotNull();
        assertThat(xsrf.getValue()).isNotBlank();

        mockMvc.perform(post("/api/v1/admin/microsoft/calendar/pull")
                        .cookie(xsrf)
                        .header("X-XSRF-TOKEN", xsrf.getValue())
                        .with(oauth2Login().authorities(new SimpleGrantedAuthority("SCOPE_Calendars.Read")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.live").value(false))
                .andExpect(jsonPath("$.mode").value("CONTRACT_HARNESS"));
    }

    @Test
    void adminPullWithScopeAndCsrfIsAcceptedForHarnessMode() throws Exception {
        mockMvc.perform(post("/api/v1/admin/microsoft/calendar/pull")
                        .with(oauth2Login().authorities(new SimpleGrantedAuthority("SCOPE_Calendars.Read")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.live").value(false))
                .andExpect(jsonPath("$.mode").value("CONTRACT_HARNESS"));
    }

    @Test
    void bearerApiStillWorksWithoutSession() throws Exception {
        mockMvc.perform(get("/api/v1/health")).andExpect(status().isOk());
    }

    @Test
    void harnessCrossingDoesNotRequireEntra() throws Exception {
        mockMvc.perform(post("/api/v1/dev/microsoft-lab/harness-crossing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.live").value(false))
                .andExpect(jsonPath("$.mode").value("CONTRACT_HARNESS"))
                .andExpect(jsonPath("$.boundary_outcome").value("ALLOW_WITH_TRANSFORMATION"))
                .andExpect(jsonPath("$.contract_id").value("m365-calendar"));
        String body = mockMvc.perform(post("/api/v1/dev/microsoft-lab/harness-crossing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(body).doesNotContain("Customer renewal");
        assertThat(body).doesNotContain("ada.lovelace");
        assertThat(body).doesNotContain("Bearer ");
        assertThat(body).doesNotContain("client_secret");
    }

    @Test
    void jwtResourceServerUnchanged() throws Exception {
        mockMvc.perform(get("/api/v1/security/me")).andExpect(status().isUnauthorized());
    }
}
