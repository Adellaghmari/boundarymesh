package dev.adellaghmari.boundarymesh.gcp;

import dev.adellaghmari.boundarymesh.gcp.firestore.FirestoreSettings;
import dev.adellaghmari.boundarymesh.gcp.pubsub.PubSubSettings;

/**
 * Keeps default {@code mvnw verify} and the emulator suite independent of live GCP. Opt-in live
 * proofs use the JUnit tag {@code gcp-live} and refuse emulator / {@code (default)} targets.
 */
public final class GcpLiveGuard {

    public static final String LIVE_TEST_TAG = "gcp-live";
    public static final String LIVE_PUBSUB_TEST_TAG = "gcp-live-pubsub";
    public static final String REQUIRED_PROJECT = "boundarymesh-dev";
    public static final String REQUIRED_DATABASE = "boundarymesh";
    public static final String EMULATOR_HOST_ENV = "FIRESTORE_EMULATOR_HOST";
    public static final String PUBSUB_EMULATOR_HOST_ENV = "PUBSUB_EMULATOR_HOST";

    private GcpLiveGuard() {}

    public static void refuseEmulatorHost() {
        refuseEmulatorHost(System.getenv(EMULATOR_HOST_ENV));
        refusePubSubEmulatorHost(System.getenv(PUBSUB_EMULATOR_HOST_ENV));
    }

    static void refuseEmulatorHost(String emulatorHost) {
        if (emulatorHost != null && !emulatorHost.isBlank()) {
            throw new IllegalStateException("FIRESTORE_EMULATOR_HOST must be unset for live Firestore verification");
        }
    }

    static void refusePubSubEmulatorHost(String emulatorHost) {
        if (emulatorHost != null && !emulatorHost.isBlank()) {
            throw new IllegalStateException("PUBSUB_EMULATOR_HOST must be unset for live Pub/Sub verification");
        }
    }

    public static FirestoreSettings productionSettings() {
        refuseEmulatorHost();
        FirestoreSettings settings = FirestoreSettings.production(REQUIRED_PROJECT);
        if (!REQUIRED_DATABASE.equals(settings.databaseId())) {
            throw new IllegalStateException("Live verification requires named database boundarymesh");
        }
        return settings;
    }

    public static PubSubSettings productionPubSubSettings() {
        refuseEmulatorHost();
        PubSubSettings settings = PubSubSettings.production(REQUIRED_PROJECT);
        if (!PubSubSettings.DELIVERY_TOPIC.equals(settings.topicId())
                || !PubSubSettings.WORKER_SUBSCRIPTION.equals(settings.subscriptionId())) {
            throw new IllegalStateException(
                    "Live verification requires boundarymesh-deliveries / boundarymesh-delivery-worker");
        }
        return settings;
    }
}
