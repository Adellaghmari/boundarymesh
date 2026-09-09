package dev.adellaghmari.boundarymesh.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.adellaghmari.boundarymesh.api.security.LocalTokenService;
import dev.adellaghmari.boundarymesh.api.security.TokenScenario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CrossingPipelineTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LocalTokenService tokens;

    @Test
    void composedAllowUsesRealIdentity() throws Exception {
        mockMvc.perform(post("/api/v1/dev/crossings/evaluate")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scenario\":\"composed-allow\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("ALLOW"))
                .andExpect(jsonPath("$.identity.status").value("PASS"))
                .andExpect(jsonPath("$.authorization.status").value("PASS"))
                .andExpect(jsonPath("$.applied_controls.CONTRACT").value("PASS"))
                .andExpect(jsonPath("$.applied_controls.EGRESS").value("PASS"))
                .andExpect(jsonPath("$.enforcement_trace[0].kind").value("IDENTITY"));
    }

    @Test
    void composedRedactionTransforms() throws Exception {
        mockMvc.perform(post("/api/v1/dev/crossings/evaluate")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scenario\":\"composed-transform\"}"))
                .andExpect(jsonPath("$.outcome").value("ALLOW_WITH_TRANSFORMATION"))
                .andExpect(jsonPath("$.reason_codes[0].code").value("EGRESS_REDACTION_REQUIRED"))
                .andExpect(jsonPath("$.transformed_payload").value(org.hamcrest.Matchers.containsString("[REDACTED]")));
    }

    @Test
    void composedDriftQuarantinesAndForbiddenRejects() throws Exception {
        mockMvc.perform(post("/api/v1/dev/crossings/evaluate")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scenario\":\"composed-quarantine\"}"))
                .andExpect(jsonPath("$.outcome").value("QUARANTINE"))
                .andExpect(jsonPath("$.classification").value("CONTRACT_DRIFT"));
        mockMvc.perform(post("/api/v1/dev/crossings/evaluate")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scenario\":\"composed-reject-egress\"}"))
                .andExpect(jsonPath("$.outcome").value("REJECT"))
                .andExpect(jsonPath("$.reason_codes[0].code").value("EGRESS_FIELD_DENIED"));
    }

    @Test
    void crossingEvaluateRequiresScope() throws Exception {
        mockMvc.perform(post("/api/v1/dev/crossings/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scenario\":\"composed-allow\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/dev/crossings/evaluate")
                        .header("Authorization", "Bearer " + tokens.issue(TokenScenario.MISSING_PERMISSION))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scenario\":\"composed-allow\"}"))
                .andExpect(status().isForbidden());
    }

    private String bearer() {
        return "Bearer " + tokens.issue(TokenScenario.VALID_MACHINE);
    }
}
