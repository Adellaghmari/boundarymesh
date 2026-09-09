package dev.adellaghmari.boundarymesh.application.webhook;

public record QuayForgeWebhookRequest(
        String connectorId, String timestamp, String nonce, String signatureHeader, byte[] rawBody) {}
