package dev.adellaghmari.boundarymesh.gcp;

/**
 * Planned mapping from existing application ports to later GCP adapters. No production adapter
 * listed here is implemented in Phase 15C.
 */
public enum GcpPortMapping {
    AUDIT_REPOSITORY("AuditRepository", "InMemoryAuditRepository", "Firestore Native"),
    ATTESTATION_REPOSITORY("AttestationRepository", "InMemoryAttestationRepository", "Firestore Native"),
    ATTESTATION_SIGNER("AttestationSigner", "JcaP256AttestationCrypto", "Cloud KMS EC_SIGN_P256_SHA256"),
    QUARANTINE_REPOSITORY("QuarantineRepository", "InMemoryQuarantineRepository", "Firestore Native"),
    REPLAY_NONCE_STORE("ReplayNonceStore", "InMemoryReplayNonceStore", "Firestore Native TTL"),
    IDEMPOTENCY_STORE("IdempotencyStore", "InMemoryIdempotencyStore", "Firestore Native"),
    DELIVERY_IDEMPOTENCY_STORE("DeliveryIdempotencyStore", "InMemoryDeliveryIdempotencyStore", "Firestore Native"),
    SECRET_VALUE_PROVIDER("SecretValueProvider", "LocalSecretValueProvider", "Secret Manager (worker only)"),
    WEBHOOK_SECRET_PROVIDER("WebhookSecretProvider", "LocalWebhookSecretProvider", "Secret Manager"),
    OBSERVED_CROSSING_STORE("ObservedCrossingStore", "ObservedCrossingStore (in-process)", "Firestore Native"),
    OUTBOX("DeliveryOutbox (not a port yet)", "ABSENT", "Firestore outbox then Pub/Sub publish");

    private final String port;
    private final String localImplementation;
    private final String plannedGcpImplementation;

    GcpPortMapping(String port, String localImplementation, String plannedGcpImplementation) {
        this.port = port;
        this.localImplementation = localImplementation;
        this.plannedGcpImplementation = plannedGcpImplementation;
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
        return false;
    }
}
