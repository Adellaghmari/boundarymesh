package dev.adellaghmari.boundarymesh.gcp;

/**
 * Mapping from existing application ports to GCP adapters. Secret Manager ports are implemented in
 * this module and not activated by default. Remaining production adapters are planned.
 */
public enum GcpPortMapping {
    AUDIT_REPOSITORY("AuditRepository", "InMemoryAuditRepository", "Firestore Native", false),
    ATTESTATION_REPOSITORY("AttestationRepository", "InMemoryAttestationRepository", "Firestore Native", false),
    ATTESTATION_SIGNER("AttestationSigner", "JcaP256AttestationCrypto", "Cloud KMS EC_SIGN_P256_SHA256", false),
    QUARANTINE_REPOSITORY("QuarantineRepository", "InMemoryQuarantineRepository", "Firestore Native", false),
    REPLAY_NONCE_STORE("ReplayNonceStore", "InMemoryReplayNonceStore", "Firestore Native TTL", false),
    IDEMPOTENCY_STORE("IdempotencyStore", "InMemoryIdempotencyStore", "Firestore Native", false),
    DELIVERY_IDEMPOTENCY_STORE(
            "DeliveryIdempotencyStore", "InMemoryDeliveryIdempotencyStore", "Firestore Native", false),
    SECRET_VALUE_PROVIDER("SecretValueProvider", "LocalSecretValueProvider", "Secret Manager (worker only)", true),
    WEBHOOK_SECRET_PROVIDER("WebhookSecretProvider", "LocalWebhookSecretProvider", "Secret Manager", true),
    OBSERVED_CROSSING_STORE("ObservedCrossingStore", "ObservedCrossingStore (in-process)", "Firestore Native", false),
    OUTBOX("DeliveryOutbox (not a port yet)", "ABSENT", "Firestore outbox then Pub/Sub publish", false);

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
