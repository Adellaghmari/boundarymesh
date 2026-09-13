package dev.adellaghmari.boundarymesh.gcp.pubsub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import dev.adellaghmari.boundarymesh.application.delivery.DeliveryIdempotencyStore;
import dev.adellaghmari.boundarymesh.application.delivery.RecordingDeliveryPort;
import dev.adellaghmari.boundarymesh.application.outbox.ConsumeDisposition;
import dev.adellaghmari.boundarymesh.application.outbox.DeliveryInstructionHandler;
import dev.adellaghmari.boundarymesh.application.outbox.DeliveryInstructionMessage;
import dev.adellaghmari.boundarymesh.application.outbox.DeliveryOutboxService;
import dev.adellaghmari.boundarymesh.application.outbox.MessagePublishException;
import dev.adellaghmari.boundarymesh.application.outbox.OutboxPublisher;
import dev.adellaghmari.boundarymesh.application.outbox.OutboxRecord;
import dev.adellaghmari.boundarymesh.application.outbox.OutboxRepository;
import dev.adellaghmari.boundarymesh.application.outbox.OutboxStatus;
import dev.adellaghmari.boundarymesh.application.outbox.PublishFailureKind;
import dev.adellaghmari.boundarymesh.application.outbox.RecordingMessagePublisher;
import dev.adellaghmari.boundarymesh.domain.BoundaryOutcome;
import dev.adellaghmari.boundarymesh.domain.ReasonCode;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryResult;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryResultKind;
import dev.adellaghmari.boundarymesh.domain.delivery.ProviderFailureCategory;
import dev.adellaghmari.boundarymesh.gcp.GcpEmulatorGuard;
import dev.adellaghmari.boundarymesh.gcp.firestore.FirestoreCollections;
import dev.adellaghmari.boundarymesh.gcp.firestore.FirestoreDeliveryIdempotencyStore;
import dev.adellaghmari.boundarymesh.gcp.firestore.FirestoreFacade;
import dev.adellaghmari.boundarymesh.gcp.firestore.FirestoreOutboxRepository;
import dev.adellaghmari.boundarymesh.gcp.firestore.FirestoreSettings;
import dev.adellaghmari.boundarymesh.gcp.firestore.GoogleFirestoreFacade;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@Tag(GcpEmulatorGuard.EMULATOR_TEST_TAG)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OutboxPubSubEmulatorTest {

    static FirestorePubSubEmulatorSupport emulator;
    static FirestoreFacade firestore;
    static FirestoreOutboxRepository outbox;
    static DeliveryOutboxService outboxService;
    static GooglePubSubPublisher publisher;
    static PubSubSettings pubsubSettings;
    static OutboxRecord allowRecord;
    static OutboxRecord transformRecord;

    @BeforeAll
    static void startEmulators() {
        assertThat(System.getenv("FIRESTORE_EMULATOR_HOST") == null
                        || System.getenv("FIRESTORE_EMULATOR_HOST").isBlank())
                .isTrue();
        emulator = FirestorePubSubEmulatorSupport.start();
        FirestoreSettings firestoreSettings =
                FirestoreSettings.emulator(FirestorePubSubEmulatorSupport.PROJECT_ID, emulator.firestoreEndpoint());
        firestore = GoogleFirestoreFacade.openEmulator(firestoreSettings);
        outbox = new FirestoreOutboxRepository(firestore);
        outboxService =
                new DeliveryOutboxService(OutboxEmulatorFixtures.factory(), outbox, OutboxEmulatorFixtures.CLOCK);
        pubsubSettings = PubSubSettings.emulator(FirestorePubSubEmulatorSupport.PROJECT_ID, emulator.pubsubEndpoint());
        publisher = GooglePubSubPublisher.connect(pubsubSettings);
    }

    @AfterAll
    static void stopEmulators() {
        if (publisher != null) {
            publisher.close();
        }
        if (firestore != null) {
            firestore.close();
        }
        if (emulator != null) {
            emulator.close();
        }
    }

    @Test
    @Order(1)
    void allowCreatesExactlyOnePendingOutboxInstruction() {
        allowRecord = outboxService
                .persistApprovedInstruction(
                        OutboxEmulatorFixtures.result(
                                BoundaryOutcome.ALLOW, ReasonCode.IDENTITY_INVALID, "{\"order_id\":\"ord-15ga\"}"),
                        OutboxEmulatorFixtures.command())
                .orElseThrow();
        assertThat(allowRecord.status()).isEqualTo(OutboxStatus.PENDING);
        assertThat(outbox.find(allowRecord.instructionId())).isPresent();
        assertThat(outbox.pending(10).stream()
                        .filter(record -> record.instructionId().equals(allowRecord.instructionId())))
                .hasSize(1);
    }

    @Test
    @Order(2)
    void allowWithTransformationCreatesExactlyOneTransformedInstruction() {
        transformRecord = outboxService
                .persistApprovedInstruction(
                        OutboxEmulatorFixtures.result(
                                BoundaryOutcome.ALLOW_WITH_TRANSFORMATION,
                                ReasonCode.EGRESS_REDACTION_REQUIRED,
                                "{\"order_id\":\"ord-15ga\",\"email\":\"[REDACTED]\"}"),
                        OutboxEmulatorFixtures.command())
                .orElseThrow();
        assertThat(transformRecord.instruction().payloadJson()).contains("[REDACTED]");
        assertThat(transformRecord.instruction().payloadJson()).doesNotContain("ada.lovelace@example.test");
        assertThat(outbox.find(transformRecord.instructionId())).isPresent();
    }

    @Test
    @Order(3)
    void quarantineAndRejectCreateZeroOutboxRecords() {
        int before = outbox.pending(100).size();
        assertThat(outboxService.persistApprovedInstruction(
                        OutboxEmulatorFixtures.result(
                                BoundaryOutcome.QUARANTINE, ReasonCode.CONTRACT_DRIFT_DETECTED, null),
                        OutboxEmulatorFixtures.command()))
                .isEmpty();
        assertThat(outboxService.persistApprovedInstruction(
                        OutboxEmulatorFixtures.result(BoundaryOutcome.REJECT, ReasonCode.IDENTITY_INVALID, null),
                        OutboxEmulatorFixtures.command()))
                .isEmpty();
        assertThat(outbox.pending(100)).hasSize(before);
    }

    @Test
    @Order(4)
    void publisherCannotPublishANonDurableInstruction() {
        OutboxPublisher coordinator = new OutboxPublisher(outbox, publisher, OutboxEmulatorFixtures.CLOCK);
        assertThatThrownBy(() -> coordinator.publish("dlv-missing-15ga"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OUTBOX_NOT_DURABLE");
    }

    @Test
    @Order(5)
    void successfulPublishMovesPendingToPublished() throws Exception {
        OutboxPublisher coordinator = new OutboxPublisher(outbox, publisher, OutboxEmulatorFixtures.CLOCK);
        OutboxRecord published = coordinator.publish(allowRecord.instructionId());
        assertThat(published.status()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(outbox.find(allowRecord.instructionId()).orElseThrow().status())
                .isEqualTo(OutboxStatus.PUBLISHED);
        RecordingDeliveryPort port = new RecordingDeliveryPort();
        try (GooglePubSubPullConsumer consumer = consumer(port, new FirestoreDeliveryIdempotencyStore(firestore))) {
            assertThat(pullUntil(consumer, 1)).containsOnly(ConsumeDisposition.ACK);
            assertThat(port.invocationCount()).isEqualTo(1);
        }
    }

    @Test
    @Order(6)
    void publishFailureLeavesRetryablePendingRecord() throws Exception {
        RecordingMessagePublisher failing = new RecordingMessagePublisher();
        failing.failNext(new MessagePublishException(PublishFailureKind.TRANSIENT, "PUBLISH_UNAVAILABLE"));
        OutboxPublisher coordinator = new OutboxPublisher(outbox, failing, OutboxEmulatorFixtures.CLOCK);
        assertThatThrownBy(() -> coordinator.publish(transformRecord.instructionId()))
                .isInstanceOf(MessagePublishException.class);
        OutboxRecord stored = outbox.find(transformRecord.instructionId()).orElseThrow();
        assertThat(stored.status()).isEqualTo(OutboxStatus.PENDING);
        assertThat(stored.lastFailureKind()).isEqualTo(PublishFailureKind.TRANSIENT);
        new OutboxPublisher(outbox, publisher, OutboxEmulatorFixtures.CLOCK).publish(transformRecord.instructionId());
        assertThat(outbox.find(transformRecord.instructionId()).orElseThrow().status())
                .isEqualTo(OutboxStatus.PUBLISHED);
        RecordingDeliveryPort port = new RecordingDeliveryPort();
        try (GooglePubSubPullConsumer consumer = consumer(port, new FirestoreDeliveryIdempotencyStore(firestore))) {
            assertThat(pullUntil(consumer, 1)).containsOnly(ConsumeDisposition.ACK);
        }
    }

    @Test
    @Order(7)
    void publishSuccessMarkFailureCanDuplicateAndWorkerDedupes() throws Exception {
        OutboxRecord extra = outboxService
                .persistApprovedInstruction(
                        OutboxEmulatorFixtures.result(
                                BoundaryOutcome.ALLOW, ReasonCode.IDENTITY_INVALID, "{\"order_id\":\"ord-dup\"}"),
                        OutboxEmulatorFixtures.command())
                .orElseThrow();
        FailingMarkOutboxRepository failingMark = new FailingMarkOutboxRepository(outbox);
        OutboxPublisher coordinator = new OutboxPublisher(failingMark, publisher, OutboxEmulatorFixtures.CLOCK);
        assertThatThrownBy(() -> coordinator.publish(extra.instructionId()))
                .isInstanceOf(MessagePublishException.class)
                .hasMessageContaining("PUBLISH_MARK_FAILED");
        assertThat(outbox.find(extra.instructionId()).orElseThrow().status()).isEqualTo(OutboxStatus.PENDING);
        failingMark.allowMark();
        coordinator.publish(extra.instructionId());
        RecordingDeliveryPort port = new RecordingDeliveryPort();
        FirestoreDeliveryIdempotencyStore store = new FirestoreDeliveryIdempotencyStore(firestore);
        try (GooglePubSubPullConsumer consumer = consumer(port, store)) {
            List<ConsumeDisposition> first = pullUntil(consumer, 1);
            List<ConsumeDisposition> second = pullUntil(consumer, 1);
            assertThat(first).containsOnly(ConsumeDisposition.ACK);
            assertThat(second).containsOnly(ConsumeDisposition.ACK);
            assertThat(port.invocationCount()).isEqualTo(1);
        }
    }

    @Test
    @Order(8)
    void workerProcessesDuplicatePubSubDeliveryOnce() throws Exception {
        OutboxRecord duplicate = outboxService
                .persistApprovedInstruction(
                        OutboxEmulatorFixtures.result(
                                BoundaryOutcome.ALLOW, ReasonCode.IDENTITY_INVALID, "{\"order_id\":\"ord-once\"}"),
                        OutboxEmulatorFixtures.command())
                .orElseThrow();
        new OutboxPublisher(outbox, publisher, OutboxEmulatorFixtures.CLOCK).publish(duplicate.instructionId());
        publisher.publish(DeliveryInstructionMessage.of(duplicate.instruction()));
        RecordingDeliveryPort port = new RecordingDeliveryPort();
        FirestoreDeliveryIdempotencyStore store = new FirestoreDeliveryIdempotencyStore(firestore);
        try (GooglePubSubPullConsumer consumer = consumer(port, store)) {
            assertThat(pullUntil(consumer, 1)).containsOnly(ConsumeDisposition.ACK);
            assertThat(pullUntil(consumer, 1)).containsOnly(ConsumeDisposition.ACK);
            assertThat(port.invocationCount()).isEqualTo(1);
        }
    }

    @Test
    @Order(9)
    void workerRestartKeepsDedupStateInFirestore() throws Exception {
        firestore.close();
        firestore = GoogleFirestoreFacade.openEmulator(
                FirestoreSettings.emulator(FirestorePubSubEmulatorSupport.PROJECT_ID, emulator.firestoreEndpoint()));
        outbox = new FirestoreOutboxRepository(firestore);
        RecordingDeliveryPort port = new RecordingDeliveryPort();
        FirestoreDeliveryIdempotencyStore store = new FirestoreDeliveryIdempotencyStore(firestore);
        publisher.publish(DeliveryInstructionMessage.of(allowRecord.instruction()));
        try (GooglePubSubPullConsumer consumer = consumer(port, store)) {
            assertThat(pullUntil(consumer, 1)).containsOnly(ConsumeDisposition.ACK);
            assertThat(port.invocationCount()).isZero();
            assertThat(store.previous(allowRecord.instructionId())).isPresent();
        }
    }

    @Test
    @Order(10)
    void malformedMessageIsTerminalAckWithoutDelivery() throws Exception {
        RecordingDeliveryPort port = new RecordingDeliveryPort();
        publishMalformed();
        try (GooglePubSubPullConsumer consumer = consumer(port, new FirestoreDeliveryIdempotencyStore(firestore))) {
            assertThat(pullUntil(consumer, 1)).containsOnly(ConsumeDisposition.ACK);
            assertThat(port.invocationCount()).isZero();
        }
    }

    @Test
    @Order(11)
    void transientWorkerFailureNacksThenRetries() throws Exception {
        OutboxRecord retry = outboxService
                .persistApprovedInstruction(
                        OutboxEmulatorFixtures.result(
                                BoundaryOutcome.ALLOW, ReasonCode.IDENTITY_INVALID, "{\"order_id\":\"ord-retry\"}"),
                        OutboxEmulatorFixtures.command())
                .orElseThrow();
        new OutboxPublisher(outbox, publisher, OutboxEmulatorFixtures.CLOCK).publish(retry.instructionId());
        RecordingDeliveryPort port = new RecordingDeliveryPort();
        port.succeedWith(new DeliveryResult(
                DeliveryResultKind.QUARANTINED,
                1,
                ProviderFailureCategory.TEMPORARY_FAILURE,
                ReasonCode.PROVIDER_TEMPORARY_FAILURE,
                "fake-provider-unavailable",
                Duration.ZERO,
                "covecrm",
                "recording.delivery",
                "CLOSED",
                503,
                null,
                null,
                true));
        FirestoreDeliveryIdempotencyStore store = new FirestoreDeliveryIdempotencyStore(firestore);
        try (GooglePubSubPullConsumer consumer = consumer(port, store)) {
            assertThat(pullUntil(consumer, 1)).containsOnly(ConsumeDisposition.NACK);
            port.reset();
            assertThat(pullUntil(consumer, 1)).containsOnly(ConsumeDisposition.ACK);
            assertThat(port.invocationCount()).isEqualTo(1);
        }
    }

    @Test
    @Order(12)
    void outboxDocumentsAndMessagesContainNoSecrets() {
        Map<String, Object> raw = firestore
                .get(FirestoreCollections.deliveryOutbox(allowRecord.instructionId()))
                .orElseThrow();
        assertThat(raw.keySet())
                .noneMatch(key -> key.toLowerCase(Locale.ROOT).contains("token")
                        || key.toLowerCase(Locale.ROOT).contains("authorization")
                        || key.toLowerCase(Locale.ROOT).contains("secret"));
        String json = DeliveryInstructionJson.write(DeliveryInstructionMessage.of(allowRecord.instruction()));
        assertThat(json.toUpperCase(Locale.ROOT))
                .doesNotContain("BEGIN PRIVATE")
                .doesNotContain("BEARER ")
                .doesNotContain("CLIENT_SECRET")
                .doesNotContain("AUTHORIZATION");
        System.out.println("OUTBOX_KEYS=" + new TreeSet<>(raw.keySet()));
    }

    private static GooglePubSubPullConsumer consumer(RecordingDeliveryPort port, DeliveryIdempotencyStore store) {
        return GooglePubSubPullConsumer.connect(pubsubSettings, new DeliveryInstructionHandler(port, store));
    }

    private static List<ConsumeDisposition> pullUntil(GooglePubSubPullConsumer consumer, int expected)
            throws InterruptedException {
        List<ConsumeDisposition> all = new ArrayList<>();
        for (int attempt = 0; attempt < 20 && all.size() < expected; attempt++) {
            all.addAll(consumer.pullAndHandle(expected));
            if (all.size() < expected) {
                Thread.sleep(250);
            }
        }
        assertThat(all).hasSize(expected);
        return all;
    }

    private static void publishMalformed() throws Exception {
        PubSubTransport transport = PubSubTransport.open(pubsubSettings);
        try {
            transport.ensureTopicAndPullSubscription();
            Publisher.Builder builder = Publisher.newBuilder(transport.topicName());
            if (transport.channelProvider() != null) {
                builder.setChannelProvider(transport.channelProvider())
                        .setCredentialsProvider(transport.credentialsProvider());
            }
            Publisher raw = builder.build();
            try {
                raw.publish(PubsubMessage.newBuilder()
                                .setData(ByteString.copyFromUtf8("{\"schema_version\":\"nope\"}"))
                                .putAttributes("schema_version", "nope")
                                .build())
                        .get();
            } finally {
                raw.shutdown();
            }
        } finally {
            transport.close();
        }
    }

    private static final class FailingMarkOutboxRepository implements OutboxRepository {
        private final OutboxRepository delegate;
        private boolean failMark = true;

        private FailingMarkOutboxRepository(OutboxRepository delegate) {
            this.delegate = delegate;
        }

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
