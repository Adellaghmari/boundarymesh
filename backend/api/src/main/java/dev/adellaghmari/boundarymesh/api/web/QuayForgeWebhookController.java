package dev.adellaghmari.boundarymesh.api.web;

import dev.adellaghmari.boundarymesh.application.webhook.QuayForgeSigningContract;
import dev.adellaghmari.boundarymesh.application.webhook.QuayForgeWebhookRequest;
import dev.adellaghmari.boundarymesh.application.webhook.WebhookDefenseResult;
import dev.adellaghmari.boundarymesh.application.webhook.WebhookDefenseService;
import dev.adellaghmari.boundarymesh.domain.IngressKind;
import dev.adellaghmari.boundarymesh.domain.TransactionId;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/webhooks")
public class QuayForgeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(QuayForgeWebhookController.class);

    private final WebhookDefenseService defense;

    public QuayForgeWebhookController(WebhookDefenseService defense) {
        this.defense = defense;
    }

    @PostMapping(path = "/quayforge", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DecisionResponse> quayforge(HttpServletRequest request) {
        if (request.getContentType() == null
                || !request.getContentType().startsWith(MediaType.APPLICATION_JSON_VALUE)) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
        }
        byte[] raw = CachedBodyRequest.rawBody(request);
        QuayForgeWebhookRequest inbound = new QuayForgeWebhookRequest(
                request.getHeader(QuayForgeSigningContract.CONNECTOR_HEADER),
                request.getHeader(QuayForgeSigningContract.TIMESTAMP_HEADER),
                request.getHeader(QuayForgeSigningContract.NONCE_HEADER),
                request.getHeader(QuayForgeSigningContract.SIGNATURE_HEADER),
                raw);
        if (inbound.connectorId() == null || inbound.timestamp() == null || inbound.nonce() == null) {
            return ResponseEntity.badRequest().build();
        }
        MDC.put("provider", "QUAYFORGE");
        TransactionId transactionId =
                TransactionId.of(java.util.UUID.randomUUID().toString());
        WebhookDefenseResult result = defense.evaluate(transactionId, inbound);
        MDC.put("transaction_id", result.decision().transactionId().value());
        MDC.put("boundary_outcome", result.decision().outcome().name());
        log.info("QuayForge webhook evaluated");
        return ResponseEntity.ok(DecisionResponse.from(
                result.decision(), result.vector(), IngressKind.SIGNED_WEBHOOK, null, result.duplicateDelivery()));
    }
}
