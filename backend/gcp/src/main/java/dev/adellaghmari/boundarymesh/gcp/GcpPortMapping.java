package dev.adellaghmari.boundarymesh.gcp;

/**
 * Mapping from existing application ports to GCP adapters. Secret Manager and Firestore persistence
 * adapters are implemented in this module and not activated by default. The delivery outbox,
 * Pub/Sub adapters, and Cloud KMS attestation signer are implemented and not default-wired.
 */
public enum GcpPortMapping {
    AUDIT_REPOSITORY("AuditRepository", "InMemoryAuditRepository", "Firestore Native", true),
    ATTESTATION_REPOSITORY("AttestationRepository", "InMemoryAttestationRepository", "Firestore Native", true),
    ATTESTATION_SIGNER("AttestationSigner", "JcaP256AttestationCrypto", "Cloud KMS EC_SIGN_P256_SHA256", true),
    QUARANTINE_REPOSITORY("QuarantineRepository", "InMemoryQuarantineRepository", "Firestore Native", true),
    REPLAY_NONCE_STORE("ReplayNonceStore", "InMemoryReplayNonceStore", "Firestore Native TTL field", true),
    IDEMPOTENCY_STORE("IdempotencyStore", "InMemoryIdempotencyStore", "Firestore Native", true),
    DELIVERY_IDEMPOTENCY_STORE(
            "DeliveryIdempotencyStore", "InMemoryDeliveryIdempotencyStore", "Firestore Native", true),
    SECRET_VALUE_PROVIDER("SecretValueProvider", "LocalSecretValueProvider", "Secret Manager (worker only)", true),
    WEBHOOK_SECRET_PROVIDER("WebhookSecretProvider", "LocalWebhookSecretProvider", "Secret Manager", true),
    OBSERVED_CROSSING_STORE("ObservedCrossingStore", "InMemoryObservedCrossingStore", "Firestore Native", true),
    OUTBOX("OutboxRepository", "InMemoryOutboxRepository", "Firestore outbox then Pub/Sub publish", true),
    MESSAGE_PUBLISHER("MessagePublisherPort", "RecordingMessagePublisher", "Google Pub/Sub topic", true),
    MESSAGE_CONSUMER("DeliveryInstructionHandler + pull consumer", "in-process handler", "Google Pub/Sub pull", true);

    private final String port;
    private final String localImplementation;
    private final String plannedGcpImplementation;
    private final boolean productionAdapterImplemented;

    GcpPortMapping(
            String port,
            String localImplementation,
            String plannedGcpImplementation,
            boolean productionAdapterImplemented) {
        this.port = port;
        this.localImplementation = localImplementation;
        this.plannedGcpImplementation = plannedGcpImplementation;
        this.productionAdapterImplemented = productionAdapterImplemented;
    }

    public String port() {
        return port;
    }

    public String localImplementation() {
        return localImplementation;
    }

    public String plannedGcpImplementation() {
        return plannedGcpImplementation;
    }

    public boolean productionAdapterImplemented() {
        return productionAdapterImplemented;
    }
}
