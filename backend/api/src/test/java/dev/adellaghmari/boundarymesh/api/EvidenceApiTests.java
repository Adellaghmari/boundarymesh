package dev.adellaghmari.boundarymesh.api;

import static org.assertj.core.api.Assertions.assertThat;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EvidenceApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LocalTokenService tokens;

    @Test
    void successfulCrossingCreatesSignedAttestationAndValidAuditChain() throws Exception {
        MvcResult delivered = mockMvc.perform(post("/api/v1/dev/delivery-lab/transformed-delivery")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision.outcome").value("ALLOW_WITH_TRANSFORMATION"))
                .andExpect(jsonPath("$.delivery.kind").value("DELIVERED"))
                .andExpect(jsonPath("$.evidence.attestation_status").value("SIGNED"))
                .andReturn();
        String body = delivered.getResponse().getContentAsString();
        assertThat(body).doesNotContain("TEST-SECRET");
        assertThat(body).doesNotContain("BEGIN PRIVATE");
        String attestationId = json(body, "attestation_id");

        mockMvc.perform(get("/api/v1/audit/chains/quayforge-orders/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALID"));
        mockMvc.perform(get("/api/v1/audit/chains/quayforge-orders"))
                .andExpect(jsonPath("$.entries[0].event_type").exists())
                .andExpect(jsonPath("$.entries[0].current_hash").exists());

        mockMvc.perform(post("/api/v1/attestations/" + attestationId + "/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALID"));

        mockMvc.perform(get("/api/v1/boundary-graph"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.edges[?(@.destination=='LUMENWAY')].status")
                        .value(org.hamcrest.Matchers.hasItem("ACTIVE")))
                .andExpect(jsonPath("$.edges[?(@.id=='helix365-calendar')].status")
                        .value(org.hamcrest.Matchers.hasItem("NO_RECENT_ACTIVITY")));
        String graph = mockMvc.perform(get("/api/v1/boundary-graph"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(graph).doesNotContain("TEST-SECRET");
        assertThat(graph).doesNotContain("eyJ");
    }

    @Test
    void contractDriftQuarantineUpdatesGraphAndAudit() throws Exception {
        mockMvc.perform(post("/api/v1/dev/delivery-lab/contract-drift")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(jsonPath("$.decision.outcome").value("QUARANTINE"))
                .andExpect(jsonPath("$.delivery.kind").value("NOT_ATTEMPTED"));
        mockMvc.perform(get("/api/v1/audit/chains/quayforge-orders"))
                .andExpect(jsonPath("$.entries[?(@.event_type=='QUARANTINE_CREATED')]")
                        .exists());
        mockMvc.perform(get("/api/v1/boundary-graph"))
                .andExpect(jsonPath("$.edges[?(@.destination=='RIVERTIDE')].status")
                        .value(org.hamcrest.Matchers.hasItem("QUARANTINED")));
    }

    @Test
    void replayRejectDoesNotDeliver() throws Exception {
        int before = mockMvc.perform(get("/api/v1/boundary-graph"))
                .andReturn()
                .getResponse()
                .getContentAsString()
                .length();
        mockMvc.perform(post("/api/v1/dev/delivery-lab/replay-reject")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(jsonPath("$.decision.outcome").value("REJECT"))
                .andExpect(jsonPath("$.delivery.kind").value("NOT_ATTEMPTED"))
                .andExpect(jsonPath("$.instruction").doesNotExist());
        mockMvc.perform(get("/api/v1/audit/chains/quayforge-orders"))
                .andExpect(jsonPath("$.entries[?(@.attributes.replay=='FAIL')]").exists());
        mockMvc.perform(get("/api/v1/boundary-graph"))
                .andExpect(jsonPath("$.edges[?(@.id=='quayforge-orders')].status")
                        .value(org.hamcrest.Matchers.hasItem("BLOCKED")))
                .andExpect(jsonPath("$.edges[?(@.id=='quayforge-orders')].recent_outcome")
                        .value(org.hamcrest.Matchers.hasItem("REJECT")));
        assertThat(before).isGreaterThan(0);
    }

    @Test
    void isolatedTamperDemosDoNotMutateLiveRepositories() throws Exception {
        mockMvc.perform(post("/api/v1/dev/delivery-lab/transformed-delivery")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/dev/audit-lab/tamper-demo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.banner").value(org.hamcrest.Matchers.containsString("SYNTHETIC DEMO")))
                .andExpect(jsonPath("$.original.status").value("VALID"))
                .andExpect(jsonPath("$.tampered.status").value("HASH_MISMATCH"));
        mockMvc.perform(get("/api/v1/audit/chains/quayforge-orders/verify"))
                .andExpect(jsonPath("$.status").value("VALID"));
        mockMvc.perform(get("/api/v1/dev/attestation-lab/tamper-demo"))
                .andExpect(jsonPath("$.original.status").value("VALID"))
                .andExpect(jsonPath("$.tampered.status").value("INVALID"));
    }

    private String bearer() {
        return "Bearer " + tokens.issue(TokenScenario.VALID_MACHINE);
    }

    private static String json(String body, String key) {
        int start = body.indexOf("\"" + key + "\":\"");
        assertThat(start).isGreaterThanOrEqualTo(0);
        int from = start + key.length() + 4;
        int to = body.indexOf('"', from);
        return body.substring(from, to);
    }
}
