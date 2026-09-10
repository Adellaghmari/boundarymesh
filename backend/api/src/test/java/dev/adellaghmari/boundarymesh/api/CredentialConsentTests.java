package dev.adellaghmari.boundarymesh.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CredentialConsentTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void catalogShowsReferencesNotValues() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/dev/credential-lab"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.credentials[0].id").exists())
                .andExpect(jsonPath("$.consents[0].status").value("GRANTED"))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(body).doesNotContain("TEST-SECRET-");
        org.assertj.core.api.Assertions.assertThat(body).doesNotContain("Bearer ");
    }

    @Test
    void missingAndRevokedAndConsentAreDistinct() throws Exception {
        mockMvc.perform(post("/api/v1/dev/credential-lab/credential-missing"))
                .andExpect(jsonPath("$.outcome").value("QUARANTINE"))
                .andExpect(jsonPath("$.reason_codes[0].code").value("CREDENTIAL_MISSING"));
        mockMvc.perform(post("/api/v1/dev/credential-lab/credential-revoked"))
                .andExpect(jsonPath("$.outcome").value("QUARANTINE"))
                .andExpect(jsonPath("$.reason_codes[0].code").value("CREDENTIAL_REVOKED"));
        mockMvc.perform(post("/api/v1/dev/credential-lab/consent-missing"))
                .andExpect(jsonPath("$.outcome").value("QUARANTINE"))
                .andExpect(jsonPath("$.reason_codes[0].code").value("CONSENT_MISSING"));
        mockMvc.perform(post("/api/v1/dev/credential-lab/credential-rotated"))
                .andExpect(
                        jsonPath("$.applied_controls.CREDENTIAL_RESOLVABILITY").value("PASS"));
    }
}
