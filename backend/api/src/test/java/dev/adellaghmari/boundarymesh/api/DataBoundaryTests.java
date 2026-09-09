package dev.adellaghmari.boundarymesh.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.adellaghmari.boundarymesh.api.egress.ResourceEgressPolicyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DataBoundaryTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void destinationPoliciesDifferForEmail() throws Exception {
        mockMvc.perform(post("/api/v1/dev/egress-lab/COVECRM/email-only"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("ALLOW"))
                .andExpect(
                        jsonPath("$.transformed_payload").value(org.hamcrest.Matchers.containsString("ada.lovelace")));
        mockMvc.perform(post("/api/v1/dev/egress-lab/LUMENWAY/email-only"))
                .andExpect(jsonPath("$.outcome").value("ALLOW_WITH_TRANSFORMATION"))
                .andExpect(jsonPath("$.transformed_payload").value(org.hamcrest.Matchers.containsString("[REDACTED]")))
                .andExpect(jsonPath("$.transformed_payload")
                        .value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("ada.lovelace"))));
    }

    @Test
    void forbiddenFieldRejects() throws Exception {
        mockMvc.perform(post("/api/v1/dev/egress-lab/RIVERTIDE/deny"))
                .andExpect(jsonPath("$.outcome").value("REJECT"))
                .andExpect(jsonPath("$.reason_codes[0].code").value("EGRESS_FIELD_DENIED"));
    }

    @Test
    void nestedAndImplicitDenyTransform() throws Exception {
        mockMvc.perform(post("/api/v1/dev/egress-lab/RIVERTIDE/nested"))
                .andExpect(jsonPath("$.outcome").value("ALLOW_WITH_TRANSFORMATION"))
                .andExpect(jsonPath("$.egress.redacted_paths[0]").value("/customer/email"));
        mockMvc.perform(post("/api/v1/dev/egress-lab/RIVERTIDE/implicit-deny"))
                .andExpect(jsonPath("$.outcome").value("ALLOW_WITH_TRANSFORMATION"))
                .andExpect(jsonPath("$.egress.implicit_deny_paths[0]").value("/legacy_region"));
    }

    @Test
    void evidenceOmitsSensitiveValues() throws Exception {
        String body = mockMvc.perform(post("/api/v1/dev/egress-lab/RIVERTIDE/redact"))
                .andExpect(jsonPath("$.egress.policy_id").value("rivertide-settlements"))
                .andExpect(jsonPath("$.egress.policy_version").value("v1"))
                .andExpect(
                        jsonPath("$.egress.fields[?(@.path=='/email')].action").value("REDACT"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        var json = JsonMapper.shared().readTree(body);
        String evidence = json.get("egress").toString()
                + json.get("contract").toString()
                + json.get("enforcement_trace").toString()
                + json.get("controls").toString();
        assertThat(evidence).contains("/email");
        assertThat(evidence).doesNotContain("ada.lovelace@example.test");
    }

    @Test
    void originalSourceRemainsInLabPayloadWhileOutboundChanges() throws Exception {
        String body = mockMvc.perform(post("/api/v1/dev/egress-lab/RIVERTIDE/redact"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(body).contains("source_payload");
        assertThat(body).contains("transformed_payload");
        assertThat(body).contains("[REDACTED]");
    }

    @Test
    void unavailablePolicyQuarantines() throws Exception {
        mockMvc.perform(post("/api/v1/dev/egress-lab/NOWHERE/policy-unavailable"))
                .andExpect(jsonPath("$.outcome").value("QUARANTINE"))
                .andExpect(jsonPath("$.reason_codes[0].code").value("EGRESS_POLICY_UNAVAILABLE"));
    }

    @Test
    void invalidPolicyIsRejectedAtParse() {
        var node = JsonMapper.shared()
                .readTree(
                        """
                        {"id":"bad-policy","version":"v1","destination":"RIVERTIDE","rules":[
                          {"rule_id":"A","path":"/email","action":"ALLOW"},
                          {"rule_id":"B","path":"/email","action":"DENY","deny_mode":"REJECT"}
                        ]}
                        """);
        assertThatThrownBy(() -> ResourceEgressPolicyRepository.parse(node))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
