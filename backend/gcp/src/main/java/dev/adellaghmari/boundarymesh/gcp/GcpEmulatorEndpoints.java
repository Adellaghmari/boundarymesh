package dev.adellaghmari.boundarymesh.gcp;

import java.util.Objects;

/**
 * Local emulator host:port placeholders for later Firestore and Pub/Sub adapter tests.
 *
 * <p>Ports avoid the local API (8080) and worker (8081). No client is opened from this type.
 */
public record GcpEmulatorEndpoints(String firestoreHost, String pubsubHost) {

    public static final String DEFAULT_FIRESTORE_HOST = "127.0.0.1:18085";
    public static final String DEFAULT_PUBSUB_HOST = "127.0.0.1:18086";

    public GcpEmulatorEndpoints {
        Objects.requireNonNull(firestoreHost, "firestoreHost");
        Objects.requireNonNull(pubsubHost, "pubsubHost");
        if (firestoreHost.isBlank() || pubsubHost.isBlank()) {
            throw new IllegalArgumentException("Emulator hosts must be non-blank.");
        }
    }

    public static GcpEmulatorEndpoints localDefaults() {
        return new GcpEmulatorEndpoints(DEFAULT_FIRESTORE_HOST, DEFAULT_PUBSUB_HOST);
    }
}
