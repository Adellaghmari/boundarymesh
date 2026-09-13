package dev.adellaghmari.boundarymesh.gcp.pubsub;

import java.util.Objects;
import java.util.Optional;

/** Pub/Sub topology names. Emulator host selects the local emulator and disables ADC. */
public record PubSubSettings(String projectId, String topicId, String subscriptionId, Optional<String> emulatorHost) {

    public static final String DELIVERY_TOPIC = "boundarymesh-deliveries";
    public static final String WORKER_SUBSCRIPTION = "boundarymesh-delivery-worker";

    public PubSubSettings {
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(topicId, "topicId");
        Objects.requireNonNull(subscriptionId, "subscriptionId");
        Objects.requireNonNull(emulatorHost, "emulatorHost");
        if (projectId.isBlank() || topicId.isBlank() || subscriptionId.isBlank()) {
            throw new IllegalArgumentException("projectId, topicId, and subscriptionId are required");
        }
        emulatorHost = emulatorHost.map(String::trim).filter(host -> !host.isBlank());
    }

    public static PubSubSettings emulator(String projectId, String emulatorHost) {
        return new PubSubSettings(projectId, DELIVERY_TOPIC, WORKER_SUBSCRIPTION, Optional.of(emulatorHost));
    }

    public static PubSubSettings production(String projectId) {
        return new PubSubSettings(projectId, DELIVERY_TOPIC, WORKER_SUBSCRIPTION, Optional.empty());
    }

    public boolean usesEmulator() {
        return emulatorHost.isPresent();
    }
}
