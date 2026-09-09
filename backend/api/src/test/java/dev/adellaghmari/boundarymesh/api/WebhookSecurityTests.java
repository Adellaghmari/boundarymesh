package dev.adellaghmari.boundarymesh.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.adellaghmari.boundarymesh.application.webhook.QuayForgeSigningContract;
import dev.adellaghmari.boundarymesh.application.webhook.WebhookSecretProvider;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebhookSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebhookSecretProvider secrets;

    @Autowired
    private Clock clock;

    @Test
    void validSignedWebhookAllows() throws Exception {
        SignedRequest request = signed(
                "evt-" + UUID.randomUUID(),
                clock.instant().getEpochSecond(),
                UUID.randomUUID().toString());
        mockMvc.perform(webhook(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("ALLOW"))
                .andExpect(jsonPath("$.applied_controls.AUTHENTICITY").value("PASS"))
                .andExpect(jsonPath("$.applied_controls.FRESHNESS").value("PASS"))
                .andExpect(jsonPath("$.applied_controls.REPLAY").value("PASS"));
    }

    @Test
    void invalidAndMissingSignaturesReject() throws Exception {
        SignedRequest valid = signed(
                "evt-" + UUID.randomUUID(),
                clock.instant().getEpochSecond(),
                UUID.randomUUID().toString());
        mockMvc.perform(webhook(
                        valid.withSignature("v1=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("REJECT"))
                .andExpect(jsonPath("$.reason_codes[0].code").value("AUTHENTICITY_INVALID_SIGNATURE"));
        mockMvc.perform(webhook(valid.withSignature("")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reason_codes[0].code").value("AUTHENTICITY_MISSING_SIGNATURE"));
    }

    @Test
    void expiredAndFutureTimestampsReject() throws Exception {
        long now = clock.instant().getEpochSecond();
        mockMvc.perform(webhook(signed("evt-old", now - 1200, UUID.randomUUID().toString())))
                .andExpect(jsonPath("$.reason_codes[0].code").value("FRESHNESS_EXPIRED"));
        mockMvc.perform(webhook(
                        signed("evt-future", now + 1200, UUID.randomUUID().toString())))
                .andExpect(jsonPath("$.reason_codes[0].code").value("FRESHNESS_FUTURE"));
    }

    @Test
    void replayRejectsAndDuplicateIsNotReplay() throws Exception {
        String eventId = "evt-dup-" + UUID.randomUUID();
        String nonce = UUID.randomUUID().toString();
        long now = clock.instant().getEpochSecond();
        SignedRequest first = signed(eventId, now, nonce);
        mockMvc.perform(webhook(first)).andExpect(jsonPath("$.outcome").value("ALLOW"));
        mockMvc.perform(webhook(first))
                .andExpect(jsonPath("$.outcome").value("REJECT"))
                .andExpect(jsonPath("$.reason_codes[0].code").value("REPLAY_DETECTED"))
                .andExpect(jsonPath("$.duplicate_delivery").value(false));
        SignedRequest retry = signed(eventId, now, UUID.randomUUID().toString());
        mockMvc.perform(webhook(retry))
                .andExpect(jsonPath("$.duplicate_delivery").value(true))
                .andExpect(jsonPath("$.outcome").value("ALLOW"));
    }

    @Test
    void wrongContentTypeAndOversizedPayloadAreRejected() throws Exception {
        mockMvc.perform(post("/api/v1/webhooks/quayforge")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("nope"))
                .andExpect(status().isUnsupportedMediaType());
        mockMvc.perform(post("/api/v1/webhooks/quayforge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("x".repeat(70_000)))
                .andExpect(status().isPayloadTooLarge());
    }

    @Test
    void webhookLabDoesNotExposeSecret() throws Exception {
        String body = mockMvc.perform(post("/api/v1/dev/webhook-lab/valid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("ALLOW"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        org.assertj.core.api.Assertions.assertThat(body.toLowerCase()).doesNotContain("secret");
        org.assertj.core.api.Assertions.assertThat(body).doesNotContain("v1=");
    }

    private MockHttpServletRequestBuilder webhook(SignedRequest request) {
        return post("/api/v1/webhooks/quayforge")
                .contentType(MediaType.APPLICATION_JSON)
                .header(QuayForgeSigningContract.CONNECTOR_HEADER, request.connectorId)
                .header(QuayForgeSigningContract.TIMESTAMP_HEADER, request.timestamp)
                .header(QuayForgeSigningContract.NONCE_HEADER, request.nonce)
                .header(QuayForgeSigningContract.SIGNATURE_HEADER, request.signature)
                .content(request.body);
    }

    private SignedRequest signed(String eventId, long timestamp, String nonce) {
        byte[] body = ("{\"event_id\":\"" + eventId + "\",\"event_type\":\"order.updated\"}")
                .getBytes(StandardCharsets.UTF_8);
        String signature = QuayForgeSigningContract.signatureHeader(
                secrets.secretBytes("quayforge-orders"), String.valueOf(timestamp), nonce, body);
        return new SignedRequest("quayforge-orders", String.valueOf(timestamp), nonce, signature, body);
    }

    private record SignedRequest(String connectorId, String timestamp, String nonce, String signature, byte[] body) {
        SignedRequest withSignature(String value) {
            return new SignedRequest(connectorId, timestamp, nonce, value, body);
        }
    }
}
