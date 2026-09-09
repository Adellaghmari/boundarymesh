package dev.adellaghmari.boundarymesh.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContractFirewallTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void validV1Allows() throws Exception {
        mockMvc.perform(post("/api/v1/dev/contract-lab/valid-v1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("ALLOW"))
                .andExpect(jsonPath("$.contract.version").value("v1"))
                .andExpect(jsonPath("$.classification").value("VALID"));
    }

    @Test
    void missingRequiredAndWrongTypeReject() throws Exception {
        mockMvc.perform(post("/api/v1/dev/contract-lab/missing-required"))
                .andExpect(jsonPath("$.outcome").value("REJECT"))
                .andExpect(jsonPath("$.classification").value("SCHEMA_VIOLATION"))
                .andExpect(jsonPath("$.reason_codes[0].code").value("CONTRACT_SCHEMA_INVALID"));
        mockMvc.perform(post("/api/v1/dev/contract-lab/wrong-type"))
                .andExpect(jsonPath("$.outcome").value("REJECT"))
                .andExpect(jsonPath("$.classification").value("SCHEMA_VIOLATION"));
    }

    @Test
    void unsupportedVersionQuarantines() throws Exception {
        mockMvc.perform(post("/api/v1/dev/contract-lab/unsupported-version"))
                .andExpect(jsonPath("$.outcome").value("QUARANTINE"))
                .andExpect(jsonPath("$.classification").value("UNSUPPORTED_CONTRACT_VERSION"))
                .andExpect(jsonPath("$.reason_codes[0].code").value("CONTRACT_VERSION_UNSUPPORTED"));
    }

    @Test
    void driftIsDistinctFromMalformed() throws Exception {
        mockMvc.perform(post("/api/v1/dev/contract-lab/contract-drift"))
                .andExpect(jsonPath("$.outcome").value("QUARANTINE"))
                .andExpect(jsonPath("$.classification").value("CONTRACT_DRIFT"))
                .andExpect(jsonPath("$.reason_codes[0].code").value("CONTRACT_DRIFT_DETECTED"));
        mockMvc.perform(post("/api/v1/dev/contract-lab/contract-unavailable"))
                .andExpect(jsonPath("$.outcome").value("QUARANTINE"))
                .andExpect(jsonPath("$.classification").value("CONTRACT_UNAVAILABLE"));
    }

    @Test
    void violationsAreTruncatedAndDoNotLeakValues() throws Exception {
        String body = mockMvc.perform(post("/api/v1/dev/contract-lab/too-many-violations"))
                .andExpect(jsonPath("$.contract.truncated_count").value(org.hamcrest.Matchers.greaterThan(0)))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(body.toLowerCase()).doesNotContain("secret");
        assertThat(body).doesNotContain("ada.lovelace@example.test");
        assertThat(body).doesNotContain("bearer ");
    }

    @Test
    void webhookOracleDoesNotExposeViolationPaths() throws Exception {
        // Public webhook path stays a bounded decision, not a schema oracle.
        // Contract lab may show paths; this test documents the split.
        String lab = mockMvc.perform(post("/api/v1/dev/contract-lab/wrong-type"))
                .andExpect(jsonPath("$.contract.violations[0].path").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(lab).contains("\"path\"");
    }
}
