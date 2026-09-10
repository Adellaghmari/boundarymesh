package dev.adellaghmari.boundarymesh.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class QuarantineApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LocalTokenService tokens;

    @Test
    void publicListIsReadOnlyAndReprocessRequiresAdmin() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/dev/delivery-lab/credential-missing")
                        .header("Authorization", "Bearer " + tokens.issue(TokenScenario.VALID_MACHINE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(jsonPath("$.quarantine.id").exists())
                .andReturn();
        mockMvc.perform(get("/api/v1/quarantine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.banner").value(org.hamcrest.Matchers.containsString("LOCAL")));
        String id = JsonMapper.shared()
                .readTree(created.getResponse().getContentAsString())
                .at("/quarantine/id")
                .asString();
        mockMvc.perform(post("/api/v1/quarantine/" + id + "/reprocess")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/quarantine/" + id + "/reprocess")
                        .header("Authorization", "Bearer " + tokens.issue(TokenScenario.VALID_MACHINE)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/quarantine/" + id + "/reprocess")
                        .header("Authorization", "Bearer " + tokens.issue(TokenScenario.VALID_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision.outcome").value("ALLOW_WITH_TRANSFORMATION"));
    }
}
