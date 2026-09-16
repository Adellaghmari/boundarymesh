package dev.adellaghmari.boundarymesh.gcp.runtime;

import dev.adellaghmari.boundarymesh.gcp.firestore.FirestoreSettings;
import dev.adellaghmari.boundarymesh.gcp.kms.KmsSettings;
import dev.adellaghmari.boundarymesh.gcp.pubsub.PubSubSettings;
import java.util.Objects;

/** Exact production coordinates accepted by the Phase 15I cloud composition. */
public record GcpProductionCoordinates(
        String projectId,
        String location,
        String firestoreDatabase,
        String pubSubTopic,
        String pubSubSubscription,
        String kmsKeyRing,
        String kmsCryptoKey,
        String kmsCryptoKeyVersion) {

    public static final String PROJECT = "boundarymesh-dev";
    public static final String LOCATION = "europe-west4";

    public GcpProductionCoordinates {
        require(projectId, PROJECT, "project");
        require(location, LOCATION, "location");
        require(firestoreDatabase, FirestoreSettings.DEFAULT_DATABASE_ID, "Firestore database");
        require(pubSubTopic, PubSubSettings.DELIVERY_TOPIC, "Pub/Sub topic");
        require(pubSubSubscription, PubSubSettings.WORKER_SUBSCRIPTION, "Pub/Sub subscription");
        require(kmsKeyRing, KmsSettings.DEFAULT_KEY_RING, "KMS key ring");
        require(kmsCryptoKey, KmsSettings.DEFAULT_CRYPTO_KEY, "KMS crypto key");
        require(kmsCryptoKeyVersion, KmsSettings.DEFAULT_VERSION, "KMS key version");
    }

    public static GcpProductionCoordinates boundaryMeshDev() {
        return new GcpProductionCoordinates(
                PROJECT,
                LOCATION,
                FirestoreSettings.DEFAULT_DATABASE_ID,
                PubSubSettings.DELIVERY_TOPIC,
                PubSubSettings.WORKER_SUBSCRIPTION,
                KmsSettings.DEFAULT_KEY_RING,
                KmsSettings.DEFAULT_CRYPTO_KEY,
                KmsSettings.DEFAULT_VERSION);
    }

    public FirestoreSettings firestoreSettings() {
        return FirestoreSettings.production(projectId);
    }

    public PubSubSettings pubSubSettings() {
        return PubSubSettings.production(projectId);
    }

    public KmsSettings kmsSettings() {
        return new KmsSettings(projectId, location, kmsKeyRing, kmsCryptoKey, kmsCryptoKeyVersion);
    }

    private static void require(String actual, String expected, String label) {
        Objects.requireNonNull(actual, label);
        if (!expected.equals(actual)) {
            throw new IllegalArgumentException(label + " must be " + expected);
        }
    }
}
