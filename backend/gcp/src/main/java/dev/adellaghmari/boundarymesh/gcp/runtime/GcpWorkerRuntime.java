package dev.adellaghmari.boundarymesh.gcp.runtime;

import dev.adellaghmari.boundarymesh.application.credential.SecretValueProvider;
import dev.adellaghmari.boundarymesh.application.delivery.DeliveryIdempotencyStore;
import dev.adellaghmari.boundarymesh.application.delivery.DeliveryPort;
import dev.adellaghmari.boundarymesh.application.outbox.DeliveryInstructionHandler;
import dev.adellaghmari.boundarymesh.gcp.firestore.FirestoreDeliveryIdempotencyStore;
import dev.adellaghmari.boundarymesh.gcp.firestore.FirestoreFacade;
import dev.adellaghmari.boundarymesh.gcp.firestore.GoogleFirestoreFacade;
import dev.adellaghmari.boundarymesh.gcp.pubsub.ContinuousPubSubWorker;
import dev.adellaghmari.boundarymesh.gcp.secret.GoogleSecretAccessor;
import dev.adellaghmari.boundarymesh.gcp.secret.SecretIdCatalog;
import dev.adellaghmari.boundarymesh.gcp.secret.SecretManagerSecretValueProvider;
import java.util.Objects;

/** Owns the worker's Firestore and Secret Manager clients and creates its pull lifecycle. */
public final class GcpWorkerRuntime implements AutoCloseable {

    private final GcpProductionCoordinates coordinates;
    private final FirestoreFacade firestore;
    private final GoogleSecretAccessor secrets;
    private final DeliveryIdempotencyStore idempotencyStore;
    private final SecretValueProvider secretValueProvider;

    private GcpWorkerRuntime(
            GcpProductionCoordinates coordinates, FirestoreFacade firestore, GoogleSecretAccessor secrets) {
        this.coordinates = Objects.requireNonNull(coordinates, "coordinates");
        this.firestore = Objects.requireNonNull(firestore, "firestore");
        this.secrets = Objects.requireNonNull(secrets, "secrets");
        this.idempotencyStore = new FirestoreDeliveryIdempotencyStore(firestore);
        this.secretValueProvider =
                new SecretManagerSecretValueProvider(new SecretIdCatalog(coordinates.projectId()), secrets);
    }

    public static GcpWorkerRuntime connect(GcpProductionCoordinates coordinates) {
        Objects.requireNonNull(coordinates, "coordinates");
        FirestoreFacade firestore = GoogleFirestoreFacade.openProduction(coordinates.firestoreSettings());
        GoogleSecretAccessor secrets = null;
        try {
            secrets = GoogleSecretAccessor.connect();
            return new GcpWorkerRuntime(coordinates, firestore, secrets);
        } catch (RuntimeException ex) {
            closeQuietly(secrets);
            closeQuietly(firestore);
            throw ex;
        }
    }

    public DeliveryIdempotencyStore idempotencyStore() {
        return idempotencyStore;
    }

    public SecretValueProvider secretValueProvider() {
        return secretValueProvider;
    }

    public ContinuousPubSubWorker newPullWorker(DeliveryPort deliveryPort) {
        DeliveryInstructionHandler handler = new DeliveryInstructionHandler(deliveryPort, idempotencyStore);
        return ContinuousPubSubWorker.connect(coordinates.pubSubSettings(), handler);
    }

    @Override
    public void close() {
        closeQuietly(secrets);
        closeQuietly(firestore);
    }

    private static void closeQuietly(AutoCloseable closeable) {
        try {
            closeable.close();
        } catch (Exception ignored) {
            // Best-effort shutdown of independent clients.
        }
    }
}
