package dev.adellaghmari.boundarymesh.gcp.firestore;

import dev.adellaghmari.boundarymesh.application.outbox.OutboxRecord;
import dev.adellaghmari.boundarymesh.application.outbox.OutboxRepository;
import dev.adellaghmari.boundarymesh.application.outbox.OutboxStatus;
import dev.adellaghmari.boundarymesh.application.outbox.PublishFailureKind;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Firestore delivery outbox. Create-pending and status updates are transactional. Pending lookup is
 * a single-field equality query ({@code status == PENDING}); no composite index.
 */
public final class FirestoreOutboxRepository implements OutboxRepository {

    private final FirestoreFacade firestore;

    public FirestoreOutboxRepository(FirestoreFacade firestore) {
        this.firestore = Objects.requireNonNull(firestore, "firestore");
    }

    @Override
    public void savePending(OutboxRecord record) {
        firestore.runTransaction(transaction -> {
            var path = FirestoreCollections.deliveryOutbox(record.instructionId());
            if (transaction.get(path).isPresent()) {
                return null;
            }
            transaction.create(path, FirestoreCodecs.outbox(record));
            return null;
        });
    }

    @Override
    public Optional<OutboxRecord> find(String instructionId) {
        return firestore.get(FirestoreCollections.deliveryOutbox(instructionId)).map(FirestoreCodecs::outbox);
    }

    @Override
    public List<OutboxRecord> pending(int limit) {
        List<OutboxRecord> pending = new ArrayList<>();
        for (var row :
                firestore.listWhere(FirestoreCollections.DELIVERY_OUTBOX, "status", OutboxStatus.PENDING.name(), 0)) {
            pending.add(FirestoreCodecs.outbox(row));
        }
        pending.sort(Comparator.comparing(OutboxRecord::createdAt).thenComparing(OutboxRecord::instructionId));
        if (limit > 0 && pending.size() > limit) {
            return List.copyOf(pending.subList(0, limit));
        }
        return List.copyOf(pending);
    }

    @Override
    public boolean markPublished(String instructionId, Instant publishedAt) {
        return firestore.runTransaction(transaction -> {
            var path = FirestoreCollections.deliveryOutbox(instructionId);
            var existing = transaction.get(path);
            if (existing.isEmpty()) {
                return false;
            }
            OutboxRecord current = FirestoreCodecs.outbox(existing.get());
            if (current.status() == OutboxStatus.PUBLISHED) {
                return true;
            }
            transaction.set(path, FirestoreCodecs.outbox(current.published(publishedAt)));
            return true;
        });
    }

    @Override
    public void recordPublishFailure(String instructionId, Instant attemptedAt, PublishFailureKind kind) {
        firestore.runTransaction(transaction -> {
            var path = FirestoreCollections.deliveryOutbox(instructionId);
            var existing = transaction.get(path);
            if (existing.isEmpty()) {
                return null;
            }
            OutboxRecord current = FirestoreCodecs.outbox(existing.get());
            if (current.status() == OutboxStatus.PUBLISHED) {
                return null;
            }
            transaction.set(path, FirestoreCodecs.outbox(current.failedAttempt(attemptedAt, kind)));
            return null;
        });
    }
}
