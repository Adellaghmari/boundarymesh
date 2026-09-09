package dev.adellaghmari.boundarymesh.application.webhook;

public record QuayForgeWebhookRequest(
        String connectorId,
        String timestamp,
        String nonce,
        String signatureHeader,
        byte[] rawBody,
        String contractVersion) {

    public QuayForgeWebhookRequest(
            String connectorId, String timestamp, String nonce, String signatureHeader, byte[] rawBody) {
        this(connectorId, timestamp, nonce, signatureHeader, rawBody, "v1");
    }
}
