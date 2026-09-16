package dev.adellaghmari.boundarymesh.gcp.runtime;

import dev.adellaghmari.boundarymesh.application.attestation.AttestationRepository;
import dev.adellaghmari.boundarymesh.application.attestation.AttestationSigner;
import dev.adellaghmari.boundarymesh.application.attestation.AttestationVerifier;
import dev.adellaghmari.boundarymesh.application.audit.AuditRepository;
import dev.adellaghmari.boundarymesh.application.delivery.DeliveryIdempotencyStore;
import dev.adellaghmari.boundarymesh.application.graph.ObservedCrossingStore;
import dev.adellaghmari.boundarymesh.application.outbox.MessagePublisherPort;
import dev.adellaghmari.boundarymesh.application.outbox.OutboxRepository;
import dev.adellaghmari.boundarymesh.application.quarantine.QuarantineRepository;
import dev.adellaghmari.boundarymesh.application.webhook.IdempotencyStore;
import dev.adellaghmari.boundarymesh.application.webhook.ReplayNonceStore;
import dev.adellaghmari.boundarymesh.application.webhook.WebhookSecretProvider;
import dev.adellaghmari.boundarymesh.gcp.firestore.FirestoreAttestationRepository;
import dev.adellaghmari.boundarymesh.gcp.firestore.FirestoreAuditRepository;
import dev.adellaghmari.boundarymesh.gcp.firestore.FirestoreDeliveryIdempotencyStore;
import dev.adellaghmari.boundarymesh.gcp.firestore.FirestoreFacade;
import dev.adellaghmari.boundarymesh.gcp.firestore.FirestoreIdempotencyStore;
import dev.adellaghmari.boundarymesh.gcp.firestore.FirestoreObservedCrossingStore;
import dev.adellaghmari.boundarymesh.gcp.firestore.FirestoreOutboxRepository;
import dev.adellaghmari.boundarymesh.gcp.firestore.FirestoreQuarantineRepository;
import dev.adellaghmari.boundarymesh.gcp.firestore.FirestoreReplayNonceStore;
import dev.adellaghmari.boundarymesh.gcp.firestore.GoogleFirestoreFacade;
import dev.adellaghmari.boundarymesh.gcp.kms.CloudKmsAttestationSigner;
import dev.adellaghmari.boundarymesh.gcp.pubsub.GooglePubSubPublisher;
import dev.adellaghmari.boundarymesh.gcp.secret.GoogleSecretAccessor;
import dev.adellaghmari.boundarymesh.gcp.secret.SecretIdCatalog;
import dev.adellaghmari.boundarymesh.gcp.secret.SecretManagerWebhookSecretProvider;
import java.util.Objects;

/** Owns the production API's Google clients and exposes only existing application ports. */
public final class GcpApiRuntime implements AutoCloseable {

    private final FirestoreFacade firestore;
    private final GoogleSecretAccessor secrets;
    private final GooglePubSubPublisher publisher;
    private final CloudKmsAttestationSigner attestationCrypto;
    private final AuditRepository auditRepository;
    private final DeliveryIdempotencyStore deliveryIdempotencyStore;
    private final AttestationRepository attestationRepository;
    private final QuarantineRepository quarantineRepository;
    private final ReplayNonceStore replayNonceStore;
    private final IdempotencyStore idempotencyStore;
    private final ObservedCrossingStore observedCrossingStore;
    private final OutboxRepository outboxRepository;
    private final WebhookSecretProvider webhookSecretProvider;

    private GcpApiRuntime(
            FirestoreFacade firestore,
            GoogleSecretAccessor secrets,
            GooglePubSubPublisher publisher,
            CloudKmsAttestationSigner attestationCrypto,
            String projectId) {
        this.firestore = Objects.requireNonNull(firestore, "firestore");
        this.secrets = Objects.requireNonNull(secrets, "secrets");
        this.publisher = Objects.requireNonNull(publisher, "publisher");
        this.attestationCrypto = Objects.requireNonNull(attestationCrypto, "attestationCrypto");
        this.auditRepository = new FirestoreAuditRepository(firestore);
        this.deliveryIdempotencyStore = new FirestoreDeliveryIdempotencyStore(firestore);
        this.attestationRepository = new FirestoreAttestationRepository(firestore);
        this.quarantineRepository = new FirestoreQuarantineRepository(firestore);
        this.replayNonceStore = new FirestoreReplayNonceStore(firestore);
        this.idempotencyStore = new FirestoreIdempotencyStore(firestore);
        this.observedCrossingStore = new FirestoreObservedCrossingStore(firestore);
        this.outboxRepository = new FirestoreOutboxRepository(firestore);
        this.webhookSecretProvider = new SecretManagerWebhookSecretProvider(new SecretIdCatalog(projectId), secrets);
    }

    public static GcpApiRuntime connect(GcpProductionCoordinates coordinates) {
        Objects.requireNonNull(coordinates, "coordinates");
        FirestoreFacade firestore = GoogleFirestoreFacade.openProduction(coordinates.firestoreSettings());
        GoogleSecretAccessor secrets = null;
        GooglePubSubPublisher publisher = null;
        CloudKmsAttestationSigner attestationCrypto = null;
        try {
            secrets = GoogleSecretAccessor.connect();
            publisher = GooglePubSubPublisher.connect(coordinates.pubSubSettings());
            attestationCrypto = CloudKmsAttestationSigner.connect(coordinates.kmsSettings());
            return new GcpApiRuntime(firestore, secrets, publisher, attestationCrypto, coordinates.projectId());
        } catch (RuntimeException ex) {
            closeQuietly(attestationCrypto);
            closeQuietly(publisher);
            closeQuietly(secrets);
            closeQuietly(firestore);
            throw ex;
        }
    }

    public AuditRepository auditRepository() {
        return auditRepository;
    }

    public DeliveryIdempotencyStore deliveryIdempotencyStore() {
        return deliveryIdempotencyStore;
    }

    public AttestationRepository attestationRepository() {
        return attestationRepository;
    }

    public AttestationSigner attestationSigner() {
        return attestationCrypto;
    }

    public AttestationVerifier attestationVerifier() {
        return attestationCrypto;
    }

    public QuarantineRepository quarantineRepository() {
        return quarantineRepository;
    }

    public ReplayNonceStore replayNonceStore() {
        return replayNonceStore;
    }

    public IdempotencyStore idempotencyStore() {
        return idempotencyStore;
    }

    public ObservedCrossingStore observedCrossingStore() {
        return observedCrossingStore;
    }

    public OutboxRepository outboxRepository() {
        return outboxRepository;
    }

    public MessagePublisherPort messagePublisher() {
        return publisher;
    }

    public WebhookSecretProvider webhookSecretProvider() {
        return webhookSecretProvider;
    }

    @Override
    public void close() {
        closeQuietly(attestationCrypto);
        closeQuietly(publisher);
        closeQuietly(secrets);
        closeQuietly(firestore);
    }

    private static void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignored) {
            // Best-effort shutdown; each owned client is still given a close attempt.
        }
    }
}
