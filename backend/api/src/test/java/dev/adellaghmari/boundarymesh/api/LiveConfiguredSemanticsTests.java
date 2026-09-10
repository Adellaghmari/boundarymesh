package dev.adellaghmari.boundarymesh.api;

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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LiveConfiguredSemanticsTests {

    @DynamicPropertySource
    static void liveConfigured(DynamicPropertyRegistry registry) {
        registry.add("boundarymesh.entra.tenant-id", () -> "00000000-0000-0000-0000-000000000001");
        registry.add("boundarymesh.entra.client-id", () -> "test-client");
        registry.add("boundarymesh.entra.client-secret", () -> "test-secret");
        registry.add("boundarymesh.entra.graph-mode", () -> "LIVE");
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void connectorModeIsLiveConfiguredNotVerified() throws Exception {
        mockMvc.perform(get("/api/v1/microsoft/connector"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("LIVE_CONFIGURED"))
                .andExpect(jsonPath("$.live").value(false))
                .andExpect(jsonPath("$.permission").value("Calendars.Read"));
    }

    @Test
    void sessionReportsCommonAuthorityAndLiveConfigured() throws Exception {
        mockMvc.perform(get("/api/v1/admin/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authority_mode").value("COMMON"))
                .andExpect(jsonPath("$.graph_mode").value("LIVE"))
                .andExpect(jsonPath("$.graph_mode_label").value("LIVE CONFIGURED"))
                .andExpect(jsonPath("$.entra_enabled").value(true));
    }

    @Test
    void provider401DoesNotProduceLiveVerified() throws Exception {
        mockMvc.perform(post("/api/v1/admin/microsoft/calendar/pull")
                        .with(oauth2Login().authorities(new SimpleGrantedAuthority("SCOPE_Calendars.Read")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("LIVE_CONFIGURED"))
                .andExpect(jsonPath("$.live").value(false))
                .andExpect(jsonPath("$.provider_status").value(401))
                .andExpect(jsonPath("$.permission").value("Calendars.Read"));
    }

    @Test
    void livePullStillBlocksAnonymous() throws Exception {
        mockMvc.perform(post("/api/v1/admin/microsoft/calendar/pull").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void livePullStillRequiresCalendarsRead() throws Exception {
        mockMvc.perform(post("/api/v1/admin/microsoft/calendar/pull")
                        .with(oauth2Login())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void livePullStillRequiresCsrf() throws Exception {
        mockMvc.perform(post("/api/v1/admin/microsoft/calendar/pull")
                        .with(oauth2Login().authorities(new SimpleGrantedAuthority("SCOPE_Calendars.Read")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
