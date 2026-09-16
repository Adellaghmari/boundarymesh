package dev.adellaghmari.boundarymesh.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
class BoundaryMeshApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LocalTokenService tokens;

    @Test
    void localCorsAllowsKnownOriginAndIgnoresUnknown() throws Exception {
        mockMvc.perform(get("/api/v1/health").header("Origin", "http://127.0.0.1:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:5173"));
        mockMvc.perform(get("/api/v1/health").header("Origin", "https://evil.example"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void health() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(header().exists("X-Request-Id"));
    }

    @Test
    void ready() throws Exception {
        mockMvc.perform(get("/api/v1/ready"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"));
    }

    @Test
    void productionTransactionIngressUsesTheSharedDeliveryPipeline() throws Exception {
        mockMvc.perform(
                        post("/api/v1/transactions")
                                .header("Authorization", bearer())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "transaction_id": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
                                  "integration_id": "quayforge-orders",
                                  "contract_version": "v1",
                                  "destination_system": "RIVERTIDE",
                                  "payload": {
                                    "event_id": "evt-production-contract",
                                    "event_type": "order.updated",
                                    "order_id": "ord-1001",
                                    "customer_id": "cust-9"
                                  }
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.transaction_id").value("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"))
                .andExpect(jsonPath("$.submission_status").value("ACCEPTED_FOR_ASYNCHRONOUS_DELIVERY"))
                .andExpect(jsonPath("$.attestation_status").value("SIGNED"));
    }

    @Test
    void productionTransactionIngressRejectsInvalidRequestBeforeApplicationMutation() throws Exception {
        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("REQUEST_INVALID"));
    }

    @Test
    void trustedInboundRequestIdIsBounded() throws Exception {
        mockMvc.perform(get("/api/v1/health").header("X-Request-Id", "ok-request_1"))
                .andExpect(header().string("X-Request-Id", "ok-request_1"));
        mockMvc.perform(get("/api/v1/health").header("X-Request-Id", " ".repeat(200)))
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(header().string(
                                "X-Request-Id", org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("  "))));
    }

    @Test
    void evaluateAllow() throws Exception {
        mockMvc.perform(post("/api/v1/dev/evaluate")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(evaluateJson("PASS", "[]")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("ALLOW"))
                .andExpect(jsonPath("$.banner").value(org.hamcrest.Matchers.containsString("SYNTHETIC")));
    }

    @Test
    void evaluateReject() throws Exception {
        String body =
                """
                {
                  "synthetic": true,
                  "transaction_id": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
                  "integration_id": "helix-calendar-crm",
                  "source_system": "HELIX_365",
                  "destination_system": "COVECRM",
                  %s
                }
                """
                        .formatted(controlsJson("FAIL", "[\"IDENTITY_INVALID\"]", "PASS", "[]"));
        mockMvc.perform(post("/api/v1/dev/evaluate")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("REJECT"))
                .andExpect(jsonPath("$.reason_codes[0].code").value("IDENTITY_INVALID"));
    }

    @Test
    void evaluateRequiresSyntheticFlag() throws Exception {
        mockMvc.perform(post("/api/v1/dev/evaluate")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(evaluateJson("PASS", "[]").replace("\"synthetic\": true", "\"synthetic\": false")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("SYNTHETIC_REQUIRED"));
    }

    @Test
    void unknownRouteIsSanitized() throws Exception {
        mockMvc.perform(get("/api/v1/does-not-exist"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("java."))));
    }

    @Test
    void openApiFoundationIsPublished() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").value(org.hamcrest.Matchers.startsWith("3.1")));
    }

    private String bearer() {
        return "Bearer " + tokens.issue(TokenScenario.VALID_MACHINE);
    }

    private static String evaluateJson(String identityStatus, String identityReasons) {
        String body =
                """
                {
                  "synthetic": true,
                  "transaction_id": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
                  "integration_id": "helix-calendar-crm",
                  "source_system": "HELIX_365",
                  "destination_system": "COVECRM",
                  %s
                }
                """;
        return body.formatted(controlsJson(identityStatus, identityReasons, "PASS", "[]"));
    }

    private static String controlsJson(
            String identityStatus, String identityReasons, String otherStatus, String otherReasons) {
        return """
                "controls": {
                  "identity": { "status": "%s", "reason_codes": %s, "safe_explanation": "Synthetic identity." },
                  "authorization": { "status": "%s", "reason_codes": %s, "safe_explanation": "Synthetic authorization." },
                  "authenticity": { "status": "%s", "reason_codes": %s, "safe_explanation": "Synthetic authenticity." },
                  "freshness": { "status": "%s", "reason_codes": %s, "safe_explanation": "Synthetic freshness." },
                  "replay": { "status": "%s", "reason_codes": %s, "safe_explanation": "Synthetic replay." },
                  "contract": { "status": "%s", "reason_codes": %s, "safe_explanation": "Synthetic contract." },
                  "egress": { "status": "%s", "reason_codes": %s, "safe_explanation": "Synthetic egress." },
                  "credential_resolvability": { "status": "%s", "reason_codes": %s, "safe_explanation": "Synthetic credential." },
                  "connector_safety": { "status": "%s", "reason_codes": %s, "safe_explanation": "Synthetic connector." }
                }
                """
                .formatted(
                        identityStatus,
                        identityReasons,
                        otherStatus,
                        otherReasons,
                        otherStatus,
                        otherReasons,
                        otherStatus,
                        otherReasons,
                        otherStatus,
                        otherReasons,
                        otherStatus,
                        otherReasons,
                        otherStatus,
                        otherReasons,
                        otherStatus,
                        otherReasons,
                        otherStatus,
                        otherReasons);
    }
}
