package dev.adellaghmari.boundarymesh.gcp.firestore;

import dev.adellaghmari.boundarymesh.application.webhook.IdempotencyStore;
import dev.adellaghmari.boundarymesh.domain.BoundaryDecision;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Request idempotency. First writer wins under concurrency. */
public final class FirestoreIdempotencyStore implements IdempotencyStore {

    private final FirestoreFacade firestore;

    public FirestoreIdempotencyStore(FirestoreFacade firestore) {
        this.firestore = Objects.requireNonNull(firestore, "firestore");
    }

    @Override
    public Optional<BoundaryDecision> find(String eventKey) {
        return firestore.get(FirestoreCollections.idempotency(eventKey)).map(FirestoreCodecs::decision);
    }

    @Override
    public void putIfAbsent(String eventKey, BoundaryDecision decision) {
        Objects.requireNonNull(eventKey, "eventKey");
        Objects.requireNonNull(decision, "decision");
        firestore.runTransaction(transaction -> {
            var path = FirestoreCollections.idempotency(eventKey);
            if (transaction.get(path).isPresent()) {
                return null;
            }
            transaction.create(path, FirestoreCodecs.idempotency(eventKey, decision, Instant.now()));
            return null;
        });
    }
}
