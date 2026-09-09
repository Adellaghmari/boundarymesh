package dev.adellaghmari.boundarymesh.application.webhook;

/** Local/test secret source. Future Secret Manager adapter can implement this port. */
public interface WebhookSecretProvider {

    byte[] secretBytes(String connectorId);
}
