package dev.adellaghmari.boundarymesh.application.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class OutboxPublisherTest {

    @Test
    void refusesToPublishANonDurableInstruction() {
        InMemoryOutboxRepository outbox = new InMemoryOutboxRepository();
        RecordingMessagePublisher publisher = new RecordingMessagePublisher();
        OutboxPublisher coordinator = new OutboxPublisher(outbox, publisher, OutboxFixtures.CLOCK);
        assertThatThrownBy(() -> coordinator.publish("dlv-missing"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OUTBOX_NOT_DURABLE");
        assertThat(publisher.publishCount()).isZero();
    }

    @Test
    void successfulPublishMarksPublished() {
        InMemoryOutboxRepository outbox = new InMemoryOutboxRepository();
        RecordingMessagePublisher publisher = new RecordingMessagePublisher();
        OutboxRecord pending = OutboxRecord.pending(OutboxFixtures.allowInstruction(), OutboxFixtures.FIXED);
        outbox.savePending(pending);
        OutboxRecord published =
                new OutboxPublisher(outbox, publisher, OutboxFixtures.CLOCK).publish(pending.instructionId());
        assertThat(published.status()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(publisher.publishCount()).isEqualTo(1);
        assertThat(outbox.pending(10)).isEmpty();
    }

    @Test
    void publishFailureLeavesRecordRetryable() {
        InMemoryOutboxRepository outbox = new InMemoryOutboxRepository();
        RecordingMessagePublisher publisher = new RecordingMessagePublisher();
        publisher.failNext(new MessagePublishException(PublishFailureKind.TRANSIENT, "PUBLISH_UNAVAILABLE"));
        OutboxRecord pending = OutboxRecord.pending(OutboxFixtures.allowInstruction(), OutboxFixtures.FIXED);
        outbox.savePending(pending);
        OutboxPublisher coordinator = new OutboxPublisher(outbox, publisher, OutboxFixtures.CLOCK);
        assertThatThrownBy(() -> coordinator.publish(pending.instructionId()))
                .isInstanceOf(MessagePublishException.class);
        OutboxRecord stored = outbox.find(pending.instructionId()).orElseThrow();
        assertThat(stored.status()).isEqualTo(OutboxStatus.PENDING);
        assertThat(stored.publishAttempts()).isEqualTo(1);
        assertThat(stored.lastFailureKind()).isEqualTo(PublishFailureKind.TRANSIENT);
        coordinator.publish(pending.instructionId());
        assertThat(outbox.find(pending.instructionId()).orElseThrow().status()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(publisher.publishCount()).isEqualTo(1);
    }

    @Test
    void publishSuccessMarkFailureLeavesPendingForDuplicatePublish() {
        RecordingMessagePublisher publisher = new RecordingMessagePublisher();
        FailingMarkOutboxRepository outbox = new FailingMarkOutboxRepository();
        OutboxRecord pending = OutboxRecord.pending(OutboxFixtures.allowInstruction(), OutboxFixtures.FIXED);
        outbox.savePending(pending);
        OutboxPublisher coordinator = new OutboxPublisher(outbox, publisher, OutboxFixtures.CLOCK);
        assertThatThrownBy(() -> coordinator.publish(pending.instructionId()))
                .isInstanceOf(MessagePublishException.class)
                .hasMessageContaining("PUBLISH_MARK_FAILED");
        assertThat(publisher.publishCount()).isEqualTo(1);
        assertThat(outbox.find(pending.instructionId()).orElseThrow().status()).isEqualTo(OutboxStatus.PENDING);
        outbox.allowMark();
        coordinator.publish(pending.instructionId());
        assertThat(publisher.publishCount()).isEqualTo(2);
        assertThat(outbox.find(pending.instructionId()).orElseThrow().status()).isEqualTo(OutboxStatus.PUBLISHED);
    }

    private static final class FailingMarkOutboxRepository implements OutboxRepository {
        private final InMemoryOutboxRepository delegate = new InMemoryOutboxRepository();
        private boolean failMark = true;

        void allowMark() {
            failMark = false;
        }

        @Override
        public void savePending(OutboxRecord record) {
            delegate.savePending(record);
        }

        @Override
        public Optional<OutboxRecord> find(String instructionId) {
            return delegate.find(instructionId);
        }

        @Override
        public List<OutboxRecord> pending(int limit) {
            return delegate.pending(limit);
        }

        @Override
        public boolean markPublished(String instructionId, Instant publishedAt) {
            if (failMark) {
                return false;
            }
            return delegate.markPublished(instructionId, publishedAt);
        }

        @Override
        public void recordPublishFailure(String instructionId, Instant attemptedAt, PublishFailureKind kind) {
            delegate.recordPublishFailure(instructionId, attemptedAt, kind);
        }
    }
}
