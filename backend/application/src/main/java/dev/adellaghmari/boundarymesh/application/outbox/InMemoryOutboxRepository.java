package dev.adellaghmari.boundarymesh.application.outbox;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Local / test outbox. Production uses the Firestore adapter. */
public final class InMemoryOutboxRepository implements OutboxRepository {

    private final ConcurrentHashMap<String, OutboxRecord> records = new ConcurrentHashMap<>();

    @Override
    public void savePending(OutboxRecord record) {
        records.putIfAbsent(record.instructionId(), record);
    }

    @Override
    public Optional<OutboxRecord> find(String instructionId) {
        return Optional.ofNullable(records.get(instructionId));
    }

    @Override
    public List<OutboxRecord> pending(int limit) {
        return records.values().stream()
                .filter(record -> record.status() == OutboxStatus.PENDING)
                .sorted(Comparator.comparing(OutboxRecord::createdAt).thenComparing(OutboxRecord::instructionId))
                .limit(limit <= 0 ? Long.MAX_VALUE : limit)
                .toList();
    }

    @Override
    public boolean markPublished(String instructionId, Instant publishedAt) {
        OutboxRecord current = records.get(instructionId);
        if (current == null) {
            return false;
        }
        if (current.status() == OutboxStatus.PUBLISHED) {
            return true;
        }
        records.put(instructionId, current.published(publishedAt));
        return true;
    }

    @Override
    public void recordPublishFailure(String instructionId, Instant attemptedAt, PublishFailureKind kind) {
        records.computeIfPresent(instructionId, (id, current) -> current.failedAttempt(attemptedAt, kind));
    }
}
