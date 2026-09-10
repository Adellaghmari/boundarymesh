package dev.adellaghmari.boundarymesh.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.adellaghmari.boundarymesh.api.security.LocalTokenService;
import dev.adellaghmari.boundarymesh.api.security.TokenScenario;
import dev.adellaghmari.boundarymesh.application.delivery.RecordingDeliveryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeliveryInvariantTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LocalTokenService tokens;

    @Autowired
    private RecordingDeliveryPort deliveryPort;

    @BeforeEach
    void resetPort() {
        deliveryPort.reset();
    }

    @Test
    void rejectNeverReachesDeliveryPort() throws Exception {
        int before = deliveryPort.invocationCount();
        mockMvc.perform(post("/api/v1/dev/delivery-lab/reject-egress")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision.outcome").value("REJECT"))
                .andExpect(jsonPath("$.delivery.kind").value("NOT_ATTEMPTED"))
                .andExpect(jsonPath("$.instruction").doesNotExist());
        assertThat(deliveryPort.invocationCount()).isEqualTo(before);
    }

    @Test
    void quarantineNeverReachesDeliveryPort() throws Exception {
        int before = deliveryPort.invocationCount();
        mockMvc.perform(post("/api/v1/dev/delivery-lab/credential-missing")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(jsonPath("$.decision.outcome").value("QUARANTINE"))
                .andExpect(jsonPath("$.delivery.kind").value("NOT_ATTEMPTED"))
                .andExpect(jsonPath("$.quarantine.reason").value("CREDENTIAL_UNAVAILABLE"));
        assertThat(deliveryPort.invocationCount()).isEqualTo(before);
    }

    @Test
    void transformedDeliveryUsesApprovedBody() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/dev/delivery-lab/transformed-delivery")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(jsonPath("$.decision.outcome").value("ALLOW_WITH_TRANSFORMATION"))
                .andExpect(jsonPath("$.delivery.kind").value("DELIVERED"))
                .andExpect(jsonPath("$.instruction.payload_json")
                        .value(org.hamcrest.Matchers.containsString("[REDACTED]")))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("TEST-SECRET-COVECRM");
        assertThat(deliveryPort.requests().getLast().instruction().payloadJson())
                .doesNotContain("ada.lovelace@example.test");
        assertThat(deliveryPort.requests().getLast().instruction().payloadJson())
                .contains("[REDACTED]");
    }

    @Test
    void consentMissingDoesNotCallDelivery() throws Exception {
        int before = deliveryPort.invocationCount();
        mockMvc.perform(post("/api/v1/dev/delivery-lab/consent-missing")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(jsonPath("$.decision.outcome").value("QUARANTINE"))
                .andExpect(jsonPath("$.consent.status").value("MISSING"));
        assertThat(deliveryPort.invocationCount()).isEqualTo(before);
    }

    @Test
    void circuitOpenDoesNotCallDelivery() throws Exception {
        int before = deliveryPort.invocationCount();
        mockMvc.perform(post("/api/v1/dev/delivery-lab/circuit-open")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(jsonPath("$.decision.outcome").value("QUARANTINE"))
                .andExpect(
                        jsonPath("$.decision.applied_controls.CONNECTOR_SAFETY").value("UNAVAILABLE"));
        assertThat(deliveryPort.invocationCount()).isEqualTo(before);
    }

    @Test
    void duplicateInstructionIsSuppressedOnSecondDeliver() throws Exception {
        mockMvc.perform(post("/api/v1/dev/delivery-lab/duplicate")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delivery.kind").value("DUPLICATE_SUPPRESSED"))
                .andExpect(jsonPath("$.delivery.provider_called").value(false));
        assertThat(deliveryPort.invocationCount()).isEqualTo(2);
        assertThat(deliveryPort.requests().get(0).instruction().eventId())
                .isEqualTo(deliveryPort.requests().get(1).instruction().eventId());
    }

    @Test
    void deliveryLabRequiresEvaluateScope() throws Exception {
        mockMvc.perform(post("/api/v1/dev/delivery-lab/transformed-delivery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    private String bearer() {
        return "Bearer " + tokens.issue(TokenScenario.VALID_MACHINE);
    }
}
