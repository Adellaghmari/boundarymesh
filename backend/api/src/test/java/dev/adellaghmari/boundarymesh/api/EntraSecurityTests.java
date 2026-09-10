package dev.adellaghmari.boundarymesh.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EntraSecurityTests {

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
                .andExpect(jsonPath("$.provider").value("NONE"));
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
