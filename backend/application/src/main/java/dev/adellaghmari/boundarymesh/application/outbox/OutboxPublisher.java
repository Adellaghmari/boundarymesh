package dev.adellaghmari.boundarymesh.application.outbox;

import java.time.Clock;
import java.util.Objects;

/**
 * Persist-then-publish coordinator. A successful publish followed by a failed mark-PUBLISHED may
 * emit a duplicate message later. That is at-least-once; the worker must deduplicate.
 */
public final class OutboxPublisher {

    private final OutboxRepository outbox;
    private final MessagePublisherPort publisher;
    private final Clock clock;

    public OutboxPublisher(OutboxRepository outbox, MessagePublisherPort publisher, Clock clock) {
        this.outbox = Objects.requireNonNull(outbox, "outbox");
        this.publisher = Objects.requireNonNull(publisher, "publisher");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public OutboxRecord publish(String instructionId) {
        Objects.requireNonNull(instructionId, "instructionId");
        OutboxRecord record =
                outbox.find(instructionId).orElseThrow(() -> new IllegalStateException("OUTBOX_NOT_DURABLE"));
        if (record.status() == OutboxStatus.PUBLISHED) {
            return record;
        }
        try {
            publisher.publish(DeliveryInstructionMessage.of(record.instruction()));
        } catch (RuntimeException ex) {
            PublishFailureKind kind =
                    ex instanceof MessagePublishException publish ? publish.kind() : PublishFailureKind.TRANSIENT;
            outbox.recordPublishFailure(instructionId, clock.instant(), kind);
            throw ex;
        }
        if (!outbox.markPublished(instructionId, clock.instant())) {
            outbox.recordPublishFailure(instructionId, clock.instant(), PublishFailureKind.MARK_FAILED);
            throw new MessagePublishException(PublishFailureKind.MARK_FAILED, "PUBLISH_MARK_FAILED");
        }
        return outbox.find(instructionId).orElseThrow();
    }

    public int publishPending(int limit) {
        int published = 0;
        for (OutboxRecord record : outbox.pending(limit)) {
            publish(record.instructionId());
            published++;
        }
        return published;
    }
}
