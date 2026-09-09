package dev.adellaghmari.boundarymesh.api.web;

import dev.adellaghmari.boundarymesh.application.webhook.QuayForgeSigningContract;
import dev.adellaghmari.boundarymesh.application.webhook.QuayForgeWebhookRequest;
import dev.adellaghmari.boundarymesh.application.webhook.WebhookDefenseResult;
import dev.adellaghmari.boundarymesh.application.webhook.WebhookDefenseService;
import dev.adellaghmari.boundarymesh.application.webhook.WebhookSecretProvider;
import dev.adellaghmari.boundarymesh.domain.IngressKind;
import dev.adellaghmari.boundarymesh.domain.TransactionId;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/dev/webhook-lab", produces = MediaType.APPLICATION_JSON_VALUE)
@ConditionalOnProperty(name = "boundarymesh.dev-security-tools-enabled", havingValue = "true")
public class DevWebhookLabController {

    private final WebhookDefenseService defense;
    private final WebhookSecretProvider secrets;
    private final Clock clock;
    private volatile LabState lastValid;

    public DevWebhookLabController(WebhookDefenseService defense, WebhookSecretProvider secrets, Clock clock) {
        this.defense = defense;
        this.secrets = secrets;
        this.clock = clock;
    }

    @PostMapping("/{scenario}")
    public DecisionResponse run(@PathVariable String scenario) {
        LabRequest built = build(scenario);
        WebhookDefenseResult result = defense.evaluate(
                TransactionId.of(UUID.randomUUID().toString()),
                new QuayForgeWebhookRequest(
                        built.connectorId, built.timestamp, built.nonce, built.signature, built.body));
        if ("valid".equals(scenario) || "duplicate".equals(scenario)) {
            lastValid = new LabState(built.eventId, built.nonce, built.timestamp, built.body);
        }
        return DecisionResponse.from(
                result.decision(), result.vector(), IngressKind.SIGNED_WEBHOOK, null, result.duplicateDelivery());
    }

    private LabRequest build(String scenario) {
        String connectorId = "quayforge-orders";
        String eventId = lastValid == null || !"duplicate".equals(scenario) && !"replay".equals(scenario)
                ? "evt-" + UUID.randomUUID()
                : lastValid.eventId;
        if (("replay".equals(scenario) || "duplicate".equals(scenario)) && lastValid == null) {
            LabRequest first = signed(
                    connectorId,
                    "evt-" + UUID.randomUUID(),
                    clock.instant().getEpochSecond(),
                    UUID.randomUUID().toString());
            defense.evaluate(
                    TransactionId.of(UUID.randomUUID().toString()),
                    new QuayForgeWebhookRequest(
                            first.connectorId, first.timestamp, first.nonce, first.signature, first.body));
            lastValid = new LabState(first.eventId, first.nonce, first.timestamp, first.body);
            eventId = first.eventId;
        }
        long timestamp = clock.instant().getEpochSecond();
        String nonce = UUID.randomUUID().toString();
        return switch (scenario) {
            case "invalid-signature" -> {
                LabRequest valid = signed(connectorId, eventId, timestamp, nonce);
                yield new LabRequest(
                        valid.connectorId,
                        valid.timestamp,
                        valid.nonce,
                        "v1=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                        valid.body,
                        valid.eventId);
            }
            case "expired-timestamp" -> signed(connectorId, eventId, timestamp - 1200, nonce);
            case "future-timestamp" -> signed(connectorId, eventId, timestamp + 1200, nonce);
            case "replay" ->
                signed(
                        connectorId,
                        lastValid.eventId,
                        Long.parseLong(lastValid.timestamp),
                        lastValid.nonce,
                        lastValid.body);
            case "duplicate" -> signed(connectorId, lastValid.eventId, timestamp, nonce);
            case "missing-signature" -> {
                LabRequest valid = signed(connectorId, eventId, timestamp, nonce);
                yield new LabRequest(valid.connectorId, valid.timestamp, valid.nonce, "", valid.body, valid.eventId);
            }
            default -> signed(connectorId, eventId, timestamp, nonce);
        };
    }

    private LabRequest signed(String connectorId, String eventId, long timestamp, String nonce) {
        byte[] body = ("{\"event_id\":\"" + eventId + "\",\"event_type\":\"order.updated\",\"order_id\":\"ord-1001\"}")
                .getBytes(StandardCharsets.UTF_8);
        return signed(connectorId, eventId, timestamp, nonce, body);
    }

    private LabRequest signed(String connectorId, String eventId, long timestamp, String nonce, byte[] body) {
        String signature = QuayForgeSigningContract.signatureHeader(
                secrets.secretBytes(connectorId), String.valueOf(timestamp), nonce, body);
        return new LabRequest(connectorId, String.valueOf(timestamp), nonce, signature, body, eventId);
    }

    private record LabRequest(
            String connectorId, String timestamp, String nonce, String signature, byte[] body, String eventId) {}

    private record LabState(String eventId, String nonce, String timestamp, byte[] body) {}
}
