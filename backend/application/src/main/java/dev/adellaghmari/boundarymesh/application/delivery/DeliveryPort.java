package dev.adellaghmari.boundarymesh.application.delivery;

import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryResult;

/**
 * Application-facing delivery abstraction. Camel is an adapter behind this port. Pub/Sub remains a
 * future transport adapter.
 */
public interface DeliveryPort {

    DeliveryResult deliver(DeliveryRequest request);
}
