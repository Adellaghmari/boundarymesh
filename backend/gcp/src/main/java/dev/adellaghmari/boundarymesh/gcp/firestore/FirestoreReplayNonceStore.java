package dev.adellaghmari.boundarymesh.gcp.firestore;

import dev.adellaghmari.boundarymesh.application.webhook.ReplayNonceStore;
import java.time.Instant;
import java.util.Objects;

/** Atomic create-if-absent replay store. Concurrent identical claims yield exactly one success. */
public final class FirestoreReplayNonceStore implements ReplayNonceStore {

    private final FirestoreFacade firestore;

    public FirestoreReplayNonceStore(FirestoreFacade firestore) {
        this.firestore = Objects.requireNonNull(firestore, "firestore");
    }

    @Override
    public boolean recordIfAbsent(String nonceHash, Instant expiresAt, Instant now) {
        Objects.requireNonNull(nonceHash, "nonceHash");
        Objects.requireNonNull(expiresAt, "expiresAt");
        Objects.requireNonNull(now, "now");
        return firestore.runTransaction(transaction -> {
            var path = FirestoreCollections.replayNonce(nonceHash);
            var existing = transaction.get(path);
            if (existing.isPresent()) {
                Instant existingExpiry = FirestoreCodecs.replayExpiresAt(existing.get());
                if (existingExpiry.isAfter(now)) {
                    return false;
                }
                transaction.delete(path);
            }
            transaction.create(path, FirestoreCodecs.replayNonce(nonceHash, expiresAt, now));
            return true;
        });
    }
}
