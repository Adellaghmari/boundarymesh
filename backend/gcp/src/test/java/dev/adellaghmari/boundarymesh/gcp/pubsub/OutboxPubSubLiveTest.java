package dev.adellaghmari.boundarymesh.gcp.pubsub;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import dev.adellaghmari.boundarymesh.application.delivery.DeliveryIdempotencyStore;
import dev.adellaghmari.boundarymesh.application.delivery.RecordingDeliveryPort;
import dev.adellaghmari.boundarymesh.application.outbox.ConsumeDisposition;
import dev.adellaghmari.boundarymesh.application.outbox.DeliveryInstructionHandler;
import dev.adellaghmari.boundarymesh.application.outbox.DeliveryInstructionMessage;
import dev.adellaghmari.boundarymesh.application.outbox.DeliveryOutboxService;
import dev.adellaghmari.boundarymesh.application.outbox.MessagePublisherPort;
import dev.adellaghmari.boundarymesh.application.outbox.OutboxPublisher;
import dev.adellaghmari.boundarymesh.application.outbox.OutboxRecord;
import dev.adellaghmari.boundarymesh.application.outbox.OutboxStatus;
import dev.adellaghmari.boundarymesh.domain.BoundaryOutcome;
import dev.adellaghmari.boundarymesh.domain.ReasonCode;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryResult;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryResultKind;
import dev.adellaghmari.boundarymesh.domain.delivery.ProviderFailureCategory;
import dev.adellaghmari.boundarymesh.gcp.GcpLiveGuard;
import dev.adellaghmari.boundarymesh.gcp.firestore.FirestoreCollections;
import dev.adellaghmari.boundarymesh.gcp.firestore.FirestoreDeliveryIdempotencyStore;
import dev.adellaghmari.boundarymesh.gcp.firestore.FirestoreDocumentPath;
import dev.adellaghmari.boundarymesh.gcp.firestore.FirestoreOutboxRepository;
import dev.adellaghmari.boundarymesh.gcp.firestore.FirestoreSettings;
import dev.adellaghmari.boundarymesh.gcp.firestore.GoogleFirestoreFacade;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Opt-in live persist → publish → consume against real Pub/Sub and named Firestore. Default {@code
 * mvnw verify} excludes {@code gcp-live} / {@code gcp-live-pubsub}.
 */
@Tag(GcpLiveGuard.LIVE_TEST_TAG)
@Tag(GcpLiveGuard.LIVE_PUBSUB_TEST_TAG)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OutboxPubSubLiveTest {

    static String runId;
    static FirestoreSettings firestoreSettings;
    static PubSubSettings pubsubSettings;
    static GoogleFirestoreFacade firestore;
    static FirestoreOutboxRepository outbox;
    static FirestoreDeliveryIdempotencyStore idempotency;
    static DeliveryOutboxService outboxService;
    static GooglePubSubPublisher publisher;
    static TrackingPublisher tracking;
    static RecordingDeliveryPort port;
    static OutboxRecord allowRecord;
    static OutboxRecord transformRecord;
    static OutboxRecord retryRecord;
    static boolean cleaned;
    static boolean harnessReady;

    @BeforeAll
    static void openProductionClients() {
        assertThat(System.getenv("GOOGLE_APPLICATION_CREDENTIALS") == null
                        || System.getenv("GOOGLE_APPLICATION_CREDENTIALS").isBlank())
                .isTrue();
        assertThat(System.getenv(GcpLiveGuard.EMULATOR_HOST_ENV) == null
                        || System.getenv(GcpLiveGuard.EMULATOR_HOST_ENV).isBlank())
                .isTrue();
        assertThat(System.getenv(GcpLiveGuard.PUBSUB_EMULATOR_HOST_ENV) == null
                        || System.getenv(GcpLiveGuard.PUBSUB_EMULATOR_HOST_ENV).isBlank())
                .isTrue();
        firestoreSettings = GcpLiveGuard.productionSettings();
        pubsubSettings = GcpLiveGuard.productionPubSubSettings();
        assertThat(firestoreSettings.usesEmulator()).isFalse();
        assertThat(pubsubSettings.usesEmulator()).isFalse();
        assertThat(firestoreSettings.databaseResourceName())
                .isEqualTo("projects/boundarymesh-dev/databases/boundarymesh");
        firestore = GoogleFirestoreFacade.openProduction(firestoreSettings);
        assertThat(firestore).isInstanceOf(GoogleFirestoreFacade.class);
        assertThat(firestore.sdkClientType().getName()).startsWith("com.google.cloud.firestore");
        publisher = GooglePubSubPublisher.connect(pubsubSettings);
        assertThat(publisher.settings().usesEmulator()).isFalse();
        assertThat(publisher.sdkClientType().getName()).isEqualTo("com.google.cloud.pubsub.v1.Publisher");
        tracking = new TrackingPublisher(publisher);
        bindAdapters();
        port = new RecordingDeliveryPort();
        runId = "livepubsub-"
                + DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                        .withZone(ZoneOffset.UTC)
                        .format(Instant.now())
                + "-"
                + randomSuffix();
        System.out.println("LIVE_PUBSUB_RUN=" + runId);
        System.out.println("LIVE_FIRESTORE_CLIENT=" + firestore.getClass().getSimpleName());
        System.out.println("LIVE_FIRESTORE_SDK=" + firestore.sdkClientType().getName());
        System.out.println("LIVE_PUBSUB_PUBLISHER=" + publisher.getClass().getSimpleName());
        System.out.println("LIVE_PUBSUB_SDK=" + publisher.sdkClientType().getName());
        System.out.println("LIVE_PUBSUB_TOPIC=" + pubsubSettings.topicId());
        System.out.println("LIVE_PUBSUB_SUBSCRIPTION=" + pubsubSettings.subscriptionId());
        try (GooglePubSubPullConsumer inspect =
                GooglePubSubPullConsumer.connect(pubsubSettings, new DeliveryInstructionHandler(port, idempotency))) {
            assertThat(inspect.sdkClientType().getName()).contains("pubsub");
            int leftover = inspect.nackUnprocessed(10);
            if (leftover > 0) {
                throw new IllegalStateException(
                        "Worker subscription already had " + leftover + " unread message(s); refusing to ACK blindly");
            }
        }
        harnessReady = true;
    }

    @AfterAll
    static void closeProductionClients() {
        try {
            if (harnessReady) {
                drainOwnedMessages();
            }
        } finally {
            try {
                cleanupRunScopedDocuments();
            } finally {
                if (publisher != null) {
                    publisher.close();
                }
                if (firestore != null) {
                    firestore.close();
                }
            }
        }
    }

    @Test
    @Order(1)
    void productionClientsTargetRealNamedResources() {
        assertThat(firestore.addressedDatabaseResource()).isEqualTo("projects/boundarymesh-dev/databases/boundarymesh");
        assertThat(pubsubSettings.projectId()).isEqualTo("boundarymesh-dev");
        assertThat(pubsubSettings.topicId()).isEqualTo("boundarymesh-deliveries");
        assertThat(pubsubSettings.subscriptionId()).isEqualTo("boundarymesh-delivery-worker");
        assertThat(publisher.settings().usesEmulator()).isFalse();
    }

    @Test
    @Order(2)
    void liveAllowPersistsThenPublishesThenConsumesOnce() throws Exception {
        int publishedBefore = tracking.publishCount();
        allowRecord = outboxService
                .persistApprovedInstruction(
                        OutboxLiveFixtures.result(
                                BoundaryOutcome.ALLOW,
                                ReasonCode.IDENTITY_INVALID,
                                "{\"order_id\":\"ord-live-allow\"}"),
                        OutboxLiveFixtures.command(runId, "{\"order_id\":\"ord-live-allow\"}"))
                .orElseThrow();
        assertThat(allowRecord.status()).isEqualTo(OutboxStatus.PENDING);
        assertThat(allowRecord.instructionId()).startsWith("dlv-");
        assertThat(outbox.find(allowRecord.instructionId())).isPresent();
        assertThat(tracking.publishCount()).isEqualTo(publishedBefore);
        OutboxRecord published =
                new OutboxPublisher(outbox, tracking, OutboxLiveFixtures.CLOCK).publish(allowRecord.instructionId());
        assertThat(published.status()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(tracking.publishedIds()).contains(allowRecord.instructionId());
        try (GooglePubSubPullConsumer consumer = consumer()) {
            assertThat(pullUntil(consumer, 1)).containsOnly(ConsumeDisposition.ACK);
        }
        assertThat(port.invocationCount()).isEqualTo(1);
        assertThat(idempotency.previous(allowRecord.instructionId())).isPresent();
        assertThat(outbox.find(allowRecord.instructionId()).orElseThrow().status())
                .isEqualTo(OutboxStatus.PUBLISHED);
    }

    @Test
    @Order(3)
    void liveAllowWithTransformationPublishesTransformedPayload() throws Exception {
        int invocationsBefore = port.invocationCount();
        transformRecord = outboxService
                .persistApprovedInstruction(
                        OutboxLiveFixtures.result(
                                BoundaryOutcome.ALLOW_WITH_TRANSFORMATION,
                                ReasonCode.EGRESS_REDACTION_REQUIRED,
                                "{\"order_id\":\"ord-live-transform\",\"note\":\"[REDACTED]\"}"),
                        OutboxLiveFixtures.command(
                                runId, "{\"order_id\":\"ord-live-transform\",\"note\":\"synthetic-plain\"}"))
                .orElseThrow();
        assertThat(transformRecord.instruction().payloadJson()).contains("[REDACTED]");
        assertThat(transformRecord.instruction().payloadJson()).doesNotContain("synthetic-plain");
        assertThat(transformRecord.status()).isEqualTo(OutboxStatus.PENDING);
        new OutboxPublisher(outbox, tracking, OutboxLiveFixtures.CLOCK).publish(transformRecord.instructionId());
        assertThat(outbox.find(transformRecord.instructionId()).orElseThrow().status())
                .isEqualTo(OutboxStatus.PUBLISHED);
        try (GooglePubSubPullConsumer consumer = consumer()) {
            assertThat(pullUntil(consumer, 1)).containsOnly(ConsumeDisposition.ACK);
        }
        assertThat(port.invocationCount()).isEqualTo(invocationsBefore + 1);
        assertThat(idempotency.previous(transformRecord.instructionId())).isPresent();
    }

    @Test
    @Order(4)
    void liveRejectCreatesZeroOutboxAndZeroPublish() {
        int publishedBefore = tracking.publishCount();
        List<String> before = firestore.listDocumentIds(FirestoreCollections.DELIVERY_OUTBOX);
        assertThat(outboxService.persistApprovedInstruction(
                        OutboxLiveFixtures.result(BoundaryOutcome.REJECT, ReasonCode.IDENTITY_INVALID, null),
                        OutboxLiveFixtures.command(runId, "{\"order_id\":\"ord-live-reject\"}")))
                .isEmpty();
        assertThat(firestore.listDocumentIds(FirestoreCollections.DELIVERY_OUTBOX))
                .isEqualTo(before);
        assertThat(tracking.publishCount()).isEqualTo(publishedBefore);
    }

    @Test
    @Order(5)
    void liveQuarantineCreatesZeroOutboxAndZeroPublish() {
        int publishedBefore = tracking.publishCount();
        List<String> before = firestore.listDocumentIds(FirestoreCollections.DELIVERY_OUTBOX);
        assertThat(outboxService.persistApprovedInstruction(
                        OutboxLiveFixtures.result(BoundaryOutcome.QUARANTINE, ReasonCode.CONTRACT_DRIFT_DETECTED, null),
                        OutboxLiveFixtures.command(runId, "{\"order_id\":\"ord-live-quarantine\"}")))
                .isEmpty();
        assertThat(firestore.listDocumentIds(FirestoreCollections.DELIVERY_OUTBOX))
                .isEqualTo(before);
        assertThat(tracking.publishCount()).isEqualTo(publishedBefore);
    }

    @Test
    @Order(6)
    void liveDuplicatePubSubDeliveryIsIdempotentlySuppressed() throws Exception {
        int invocationsBefore = port.invocationCount();
        tracking.publish(DeliveryInstructionMessage.of(allowRecord.instruction()));
        try (GooglePubSubPullConsumer consumer = consumer()) {
            assertThat(pullUntil(consumer, 1)).containsOnly(ConsumeDisposition.ACK);
        }
        assertThat(port.invocationCount()).isEqualTo(invocationsBefore);
        assertThat(idempotency.previous(allowRecord.instructionId())).isPresent();
    }

    @Test
    @Order(7)
    void liveMalformedMessageIsTerminalAckWithoutDelivery() throws Exception {
        int invocationsBefore = port.invocationCount();
        publishMalformed();
        try (GooglePubSubPullConsumer consumer = consumer()) {
            assertThat(pullUntil(consumer, 1)).containsOnly(ConsumeDisposition.ACK);
        }
        assertThat(port.invocationCount()).isEqualTo(invocationsBefore);
    }

    @Test
    @Order(8)
    void liveTransientNackThenAckDoesNotLeavePoison() throws Exception {
        retryRecord = outboxService
                .persistApprovedInstruction(
                        OutboxLiveFixtures.result(
                                BoundaryOutcome.ALLOW,
                                ReasonCode.IDENTITY_INVALID,
                                "{\"order_id\":\"ord-live-retry\"}"),
                        OutboxLiveFixtures.command(runId, "{\"order_id\":\"ord-live-retry\"}"))
                .orElseThrow();
        new OutboxPublisher(outbox, tracking, OutboxLiveFixtures.CLOCK).publish(retryRecord.instructionId());
        RecordingDeliveryPort isolated = new RecordingDeliveryPort();
        isolated.succeedWith(new DeliveryResult(
                DeliveryResultKind.QUARANTINED,
                1,
                ProviderFailureCategory.TEMPORARY_FAILURE,
                ReasonCode.PROVIDER_TEMPORARY_FAILURE,
                "fake-provider-unavailable",
                Duration.ZERO,
                "livepubsub",
                "recording.delivery",
                "CLOSED",
                503,
                null,
                null,
                true));
        FirestoreDeliveryIdempotencyStore store = new FirestoreDeliveryIdempotencyStore(firestore);
        try (GooglePubSubPullConsumer consumer = consumer(isolated, store)) {
            assertThat(pullUntil(consumer, 1)).containsOnly(ConsumeDisposition.NACK);
            isolated.reset();
            assertThat(pullUntil(consumer, 1)).containsOnly(ConsumeDisposition.ACK);
            assertThat(isolated.invocationCount()).isEqualTo(1);
        }
        assertThat(store.previous(retryRecord.instructionId())).isPresent();
    }

    @Test
    @Order(9)
    void liveStateSurvivesClientRecreation() throws Exception {
        publisher.close();
        firestore.close();
        firestore = GoogleFirestoreFacade.openProduction(firestoreSettings);
        bindAdapters();
        publisher = GooglePubSubPublisher.connect(pubsubSettings);
        tracking = new TrackingPublisher(publisher);
        assertThat(outbox.find(allowRecord.instructionId()).orElseThrow().status())
                .isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(idempotency.previous(allowRecord.instructionId())).isPresent();
        tracking.publish(DeliveryInstructionMessage.of(allowRecord.instruction()));
        RecordingDeliveryPort isolated = new RecordingDeliveryPort();
        try (GooglePubSubPullConsumer consumer = consumer(isolated, idempotency)) {
            assertThat(pullUntil(consumer, 1)).containsOnly(ConsumeDisposition.ACK);
            assertThat(isolated.invocationCount()).isZero();
        }
    }

    @Test
    @Order(10)
    void liveOutboxAndMessagesContainNoSecrets() {
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
        System.out.println("LIVE_OUTBOX_KEYS=" + new TreeSet<>(raw.keySet()));
    }

    @Test
    @Order(11)
    void cleanupRemovesOnlyThisRun() {
        cleanupRunScopedDocuments();
        assertThat(remainingRunScopedPaths()).isEmpty();
        assertThat(firestoreSettings.databaseResourceName())
                .isEqualTo("projects/boundarymesh-dev/databases/boundarymesh");
        assertThat(pubsubSettings.topicId()).isEqualTo("boundarymesh-deliveries");
        assertThat(pubsubSettings.subscriptionId()).isEqualTo("boundarymesh-delivery-worker");
    }

    private static void bindAdapters() {
        outbox = new FirestoreOutboxRepository(firestore);
        idempotency = new FirestoreDeliveryIdempotencyStore(firestore);
        outboxService = new DeliveryOutboxService(OutboxLiveFixtures.factory(), outbox, OutboxLiveFixtures.CLOCK);
    }

    private static GooglePubSubPullConsumer consumer() {
        return consumer(port, idempotency);
    }

    private static GooglePubSubPullConsumer consumer(
            RecordingDeliveryPort deliveryPort, DeliveryIdempotencyStore store) {
        return GooglePubSubPullConsumer.connect(pubsubSettings, new DeliveryInstructionHandler(deliveryPort, store));
    }

    private static List<ConsumeDisposition> pullUntil(GooglePubSubPullConsumer consumer, int expected)
            throws InterruptedException {
        List<ConsumeDisposition> all = new ArrayList<>();
        for (int attempt = 0; attempt < 40 && all.size() < expected; attempt++) {
            all.addAll(consumer.pullAndHandle(expected));
            if (all.size() < expected) {
                Thread.sleep(500);
            }
        }
        assertThat(all).hasSize(expected);
        return all;
    }

    private static void publishMalformed() throws Exception {
        PubSubTransport transport = PubSubTransport.open(pubsubSettings);
        try {
            Publisher.Builder builder = Publisher.newBuilder(transport.topicName());
            Publisher raw = builder.build();
            try {
                raw.publish(PubsubMessage.newBuilder()
                                .setData(ByteString.copyFromUtf8("{\"schema_version\":\"nope\"}"))
                                .putAttributes("schema_version", "nope")
                                .putAttributes("instruction_id", "livepubsub-malformed-" + runId)
                                .build())
                        .get();
            } finally {
                raw.shutdown();
            }
        } finally {
            transport.close();
        }
    }

    private static void drainOwnedMessages() {
        if (!harnessReady || pubsubSettings == null || runId == null) {
            return;
        }
        try (GooglePubSubPullConsumer consumer = consumer()) {
            for (int attempt = 0; attempt < 8; attempt++) {
                int foreign = consumer.ackOwnedNackForeign(10, OutboxPubSubLiveTest::payloadBelongsToRun);
                if (foreign > 0) {
                    throw new IllegalStateException(
                            "Pulled " + foreign + " unrelated Pub/Sub message(s); NACKed and preserved");
                }
            }
        }
    }

    private static boolean payloadBelongsToRun(String payload) {
        if (payload == null || runId == null) {
            return false;
        }
        if (payload.contains(runId)) {
            return true;
        }
        for (String id : trackedInstructionIds()) {
            if (payload.contains(id)) {
                return true;
            }
        }
        return payload.contains("livepubsub-malformed-" + runId);
    }

    private static void cleanupRunScopedDocuments() {
        if (firestore == null || runId == null || cleaned) {
            return;
        }
        for (String id : trackedInstructionIds()) {
            firestore.delete(FirestoreCollections.deliveryOutbox(id));
            firestore.delete(FirestoreCollections.deliveryIdempotency(id));
        }
        for (String id : firestore.listDocumentIds(FirestoreCollections.DELIVERY_OUTBOX)) {
            if (belongsToRun(id)
                    || firestore
                            .get(FirestoreCollections.deliveryOutbox(id))
                            .map(OutboxPubSubLiveTest::documentBelongsToRun)
                            .orElse(false)) {
                firestore.delete(FirestoreCollections.deliveryOutbox(id));
            }
        }
        for (String id : firestore.listDocumentIds(FirestoreCollections.DELIVERY_IDEMPOTENCY)) {
            if (belongsToRun(id)
                    || firestore
                            .get(FirestoreCollections.deliveryIdempotency(id))
                            .map(OutboxPubSubLiveTest::documentBelongsToRun)
                            .orElse(false)) {
                firestore.delete(FirestoreCollections.deliveryIdempotency(id));
            }
        }
        cleaned = true;
    }

    private static List<String> trackedInstructionIds() {
        List<String> ids = new ArrayList<>();
        if (allowRecord != null) {
            ids.add(allowRecord.instructionId());
        }
        if (transformRecord != null) {
            ids.add(transformRecord.instructionId());
        }
        if (retryRecord != null) {
            ids.add(retryRecord.instructionId());
        }
        return ids;
    }

    private static List<String> remainingRunScopedPaths() {
        List<String> remaining = new ArrayList<>();
        for (String collection :
                List.of(FirestoreCollections.DELIVERY_OUTBOX, FirestoreCollections.DELIVERY_IDEMPOTENCY)) {
            for (String id : firestore.listDocumentIds(collection)) {
                if (belongsToRun(id)
                        || firestore
                                .get(FirestoreDocumentPath.of(collection, id))
                                .map(OutboxPubSubLiveTest::documentBelongsToRun)
                                .orElse(false)) {
                    remaining.add(collection + "/" + id);
                }
            }
        }
        return remaining;
    }

    private static boolean belongsToRun(String documentId) {
        return documentId != null
                && runId != null
                && documentId.toLowerCase(Locale.ROOT).contains(runId.toLowerCase(Locale.ROOT));
    }

    private static boolean documentBelongsToRun(Map<String, Object> raw) {
        return flatten(raw).stream().anyMatch(value -> value.contains(runId));
    }

    private static List<String> flatten(Object value) {
        List<String> values = new ArrayList<>();
        if (value instanceof Map<?, ?> map) {
            for (Object nested : map.values()) {
                values.addAll(flatten(nested));
            }
            return values;
        }
        if (value instanceof List<?> list) {
            for (Object item : list) {
                values.addAll(flatten(item));
            }
            return values;
        }
        if (value != null) {
            values.add(String.valueOf(value));
        }
        return values;
    }

    private static String randomSuffix() {
        byte[] bytes = new byte[4];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private static final class TrackingPublisher implements MessagePublisherPort {
        private final GooglePubSubPublisher delegate;
        private final List<String> publishedIds = new CopyOnWriteArrayList<>();

        private TrackingPublisher(GooglePubSubPublisher delegate) {
            this.delegate = delegate;
        }

        @Override
        public void publish(DeliveryInstructionMessage message) {
            publishedIds.add(message.instructionId());
            delegate.publish(message);
        }

        private int publishCount() {
            return publishedIds.size();
        }

        private List<String> publishedIds() {
            return List.copyOf(publishedIds);
        }
    }
}
