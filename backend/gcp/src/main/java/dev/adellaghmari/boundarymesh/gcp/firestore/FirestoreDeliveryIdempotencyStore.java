package dev.adellaghmari.boundarymesh.gcp.firestore;

import dev.adellaghmari.boundarymesh.application.delivery.DeliveryIdempotencyStore;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryResult;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Delivery idempotency. First writer wins under concurrency. */
public final class FirestoreDeliveryIdempotencyStore implements DeliveryIdempotencyStore {

    private final FirestoreFacade firestore;

    public FirestoreDeliveryIdempotencyStore(FirestoreFacade firestore) {
        this.firestore = Objects.requireNonNull(firestore, "firestore");
    }

    @Override
    public Optional<DeliveryResult> previous(String deliveryInstructionId) {
        return firestore
                .get(FirestoreCollections.deliveryIdempotency(deliveryInstructionId))
                .map(FirestoreCodecs::deliveryResultFromDocument);
    }

    @Override
    public void remember(String deliveryInstructionId, DeliveryResult result) {
        Objects.requireNonNull(deliveryInstructionId, "deliveryInstructionId");
        Objects.requireNonNull(result, "result");
        firestore.runTransaction(transaction -> {
            var path = FirestoreCollections.deliveryIdempotency(deliveryInstructionId);
            if (transaction.get(path).isPresent()) {
                return null;
            }
            transaction.create(path, FirestoreCodecs.deliveryIdempotency(deliveryInstructionId, result, Instant.now()));
            return null;
        });
    }

    @Override
    public void clear() {
        for (String id : firestore.listDocumentIds(FirestoreCollections.DELIVERY_IDEMPOTENCY)) {
            firestore.delete(FirestoreCollections.deliveryIdempotency(id));
        }
    }
}
