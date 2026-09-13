package dev.adellaghmari.boundarymesh.gcp.firestore;

import dev.adellaghmari.boundarymesh.application.graph.ObservedCrossingStore;
import dev.adellaghmari.boundarymesh.domain.audit.AuditChainId;

/** Stable collection names. Outbox documents live in {@link #DELIVERY_OUTBOX}. */
public final class FirestoreCollections {

    public static final String AUDIT_CHAINS = "auditChains";
    public static final String AUDIT_ENTRIES = "entries";
    public static final String ATTESTATIONS = "attestations";
    public static final String QUARANTINE = "quarantine";
    public static final String REPLAY_NONCES = "replayNonces";
    public static final String IDEMPOTENCY = "idempotency";
    public static final String DELIVERY_IDEMPOTENCY = "deliveryIdempotency";
    public static final String OBSERVED_CROSSINGS = "observedCrossings";
    public static final String DELIVERY_OUTBOX = "deliveryOutbox";

    private FirestoreCollections() {}

    public static FirestoreDocumentPath auditHead(AuditChainId chainId) {
        return FirestoreDocumentPath.of(AUDIT_CHAINS, FirestoreDocumentIds.requireSafe(chainId.value()));
    }

    public static FirestoreDocumentPath auditEntry(AuditChainId chainId, long sequence) {
        return FirestoreDocumentPath.nested(
                AUDIT_CHAINS,
                FirestoreDocumentIds.requireSafe(chainId.value()),
                AUDIT_ENTRIES,
                FirestoreDocumentIds.sequence(sequence));
    }

    public static FirestoreDocumentPath attestation(String attestationId) {
        return FirestoreDocumentPath.of(ATTESTATIONS, FirestoreDocumentIds.requireSafe(attestationId));
    }

    public static FirestoreDocumentPath quarantine(String quarantineId) {
        return FirestoreDocumentPath.of(QUARANTINE, FirestoreDocumentIds.requireSafe(quarantineId));
    }

    public static FirestoreDocumentPath replayNonce(String nonceHash) {
        return FirestoreDocumentPath.of(REPLAY_NONCES, FirestoreDocumentIds.requireSafe(nonceHash));
    }

    public static FirestoreDocumentPath idempotency(String eventKey) {
        return FirestoreDocumentPath.of(IDEMPOTENCY, FirestoreDocumentIds.requireSafe(eventKey));
    }

    public static FirestoreDocumentPath deliveryIdempotency(String deliveryInstructionId) {
        return FirestoreDocumentPath.of(DELIVERY_IDEMPOTENCY, FirestoreDocumentIds.requireSafe(deliveryInstructionId));
    }

    public static FirestoreDocumentPath observedCrossing(String destination) {
        return FirestoreDocumentPath.of(
                OBSERVED_CROSSINGS,
                FirestoreDocumentIds.requireSafe(ObservedCrossingStore.destinationKey(destination)));
    }

    public static FirestoreDocumentPath deliveryOutbox(String instructionId) {
        return FirestoreDocumentPath.of(DELIVERY_OUTBOX, FirestoreDocumentIds.requireSafe(instructionId));
    }
}
