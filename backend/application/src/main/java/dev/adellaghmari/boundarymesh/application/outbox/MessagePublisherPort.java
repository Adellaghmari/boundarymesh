package dev.adellaghmari.boundarymesh.application.outbox;

/**
 * Cloud-neutral publisher. Production adapter uses Pub/Sub. Failures must not leak payloads or
 * secrets.
 */
public interface MessagePublisherPort {

    void publish(DeliveryInstructionMessage message);
}
