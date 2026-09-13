package dev.adellaghmari.boundarymesh.application.delivery;

import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryResult;

/**
 * Application-facing delivery abstraction. Camel is an adapter behind this port. Pub/Sub transport
 * is implemented in {@code backend/gcp} and is not the local default.
 */
public interface DeliveryPort {

    DeliveryResult deliver(DeliveryRequest request);
}
