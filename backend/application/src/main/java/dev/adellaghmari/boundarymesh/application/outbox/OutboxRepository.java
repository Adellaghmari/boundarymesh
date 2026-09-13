package dev.adellaghmari.boundarymesh.application.outbox;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Cloud-agnostic delivery outbox. Implementations must not require a process-local lock. */
public interface OutboxRepository {

    void savePending(OutboxRecord record);

    Optional<OutboxRecord> find(String instructionId);

    List<OutboxRecord> pending(int limit);

    boolean markPublished(String instructionId, Instant publishedAt);

    void recordPublishFailure(String instructionId, Instant attemptedAt, PublishFailureKind kind);
}
