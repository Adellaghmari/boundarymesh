package dev.adellaghmari.boundarymesh.gcp.firestore;

import static org.assertj.core.api.Assertions.assertThat;

import dev.adellaghmari.boundarymesh.application.audit.AuditAppender;
import dev.adellaghmari.boundarymesh.application.audit.AuditChainVerifier;
import dev.adellaghmari.boundarymesh.application.graph.ObservedCrossingStore;
import dev.adellaghmari.boundarymesh.application.quarantine.QuarantineOrchestrator;
import dev.adellaghmari.boundarymesh.domain.BoundaryDecision;
import dev.adellaghmari.boundarymesh.domain.BoundaryOutcome;
import dev.adellaghmari.boundarymesh.domain.attestation.AttestationId;
import dev.adellaghmari.boundarymesh.domain.attestation.ExchangeAttestation;
import dev.adellaghmari.boundarymesh.domain.audit.AuditChainId;
import dev.adellaghmari.boundarymesh.domain.audit.AuditEntry;
import dev.adellaghmari.boundarymesh.domain.audit.AuditEventType;
import dev.adellaghmari.boundarymesh.domain.audit.AuditVerificationStatus;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryResult;
import dev.adellaghmari.boundarymesh.domain.quarantine.QuarantineId;
import dev.adellaghmari.boundarymesh.domain.quarantine.QuarantineItem;
import dev.adellaghmari.boundarymesh.domain.quarantine.QuarantineStatus;
import dev.adellaghmari.boundarymesh.gcp.GcpLiveGuard;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Opt-in live Java SDK verification against {@code projects/boundarymesh-dev/databases/boundarymesh}.
 * Default {@code mvnw verify} excludes {@code gcp-live}. Cleanup deletes only this run's documents.
 */
@Tag(GcpLiveGuard.LIVE_TEST_TAG)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FirestorePersistenceLiveTest {

    private static final Clock CLOCK = Clock.fixed(FirestorePersistenceFixtures.FIXED, ZoneOffset.UTC);
    private static final Set<String> EXPECTED_COLLECTIONS = Set.of(
            FirestoreCollections.AUDIT_CHAINS,
            FirestoreCollections.ATTESTATIONS,
            FirestoreCollections.QUARANTINE,
            FirestoreCollections.REPLAY_NONCES,
            FirestoreCollections.IDEMPOTENCY,
            FirestoreCollections.DELIVERY_IDEMPOTENCY,
            FirestoreCollections.DELIVERY_OUTBOX,
            FirestoreCollections.OBSERVED_CROSSINGS);
    private static final int CONCURRENT_APPENDS = 8;

    static String runId;
    static AuditChainId chainId;
    static String firstReplayNonce;
    static String concurrentReplayNonce;
    static String requestKey;
    static String concurrentRequestKey;
    static String deliveryKey;
    static String concurrentDeliveryKey;
    static String attestationId;
    static String quarantineId;
    static String destination;
    static String observationIntegration;
    static GoogleFirestoreFacade facade;
    static FirestoreSettings settings;
    static FirestoreAuditRepository audits;
    static FirestoreReplayNonceStore replay;
    static FirestoreIdempotencyStore idempotency;
    static FirestoreDeliveryIdempotencyStore delivery;
    static FirestoreQuarantineRepository quarantine;
    static FirestoreAttestationRepository attestations;
    static FirestoreObservedCrossingStore observations;
    static boolean cleaned;

    @BeforeAll
    static void openProductionClient() {
        GcpLiveGuard.refuseEmulatorHost();
        assertThat(System.getenv(GcpLiveGuard.EMULATOR_HOST_ENV) == null
                        || System.getenv(GcpLiveGuard.EMULATOR_HOST_ENV).isBlank())
                .isTrue();
        settings = GcpLiveGuard.productionSettings();
        assertThat(settings.usesEmulator()).isFalse();
        assertThat(settings.projectId()).isEqualTo("boundarymesh-dev");
        assertThat(settings.databaseId()).isEqualTo("boundarymesh");
        assertThat(settings.databaseId()).isNotEqualTo("(default)");
        assertThat(settings.databaseResourceName()).isEqualTo("projects/boundarymesh-dev/databases/boundarymesh");
        facade = GoogleFirestoreFacade.openProduction(settings);
        assertThat(facade).isInstanceOf(GoogleFirestoreFacade.class);
        assertThat(facade).isNotInstanceOf(EmulatorHttpFirestoreFacade.class);
        assertThat(facade.addressedDatabaseResource()).isEqualTo("projects/boundarymesh-dev/databases/boundarymesh");
        assertThat(facade.sdkClientType().getName()).startsWith("com.google.cloud.firestore");
        System.out.println("LIVE_CLIENT=" + facade.getClass().getSimpleName());
        System.out.println("LIVE_SDK=" + facade.sdkClientType().getName());
        System.out.println("LIVE_TARGET=" + facade.addressedDatabaseResource());
        bindAdapters(facade);
        runId = "liveverify-"
                + DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                        .withZone(ZoneOffset.UTC)
                        .format(Instant.now())
                + "-"
                + randomSuffix();
        chainId = new AuditChainId(runId);
        firstReplayNonce = "liveverify-replay-" + runId;
        concurrentReplayNonce = "liveverify-replay-concurrent-" + runId;
        requestKey = "liveverify-request-" + runId;
        concurrentRequestKey = "liveverify-request-concurrent-" + runId;
        deliveryKey = "liveverify-delivery-" + runId;
        concurrentDeliveryKey = "liveverify-delivery-concurrent-" + runId;
        attestationId = "liveverify-attestation-" + runId;
        quarantineId = "liveverify-quarantine-" + runId;
        destination = "liveverify-destination-" + runId;
        observationIntegration = runId;
        System.out.println("LIVE_RUN=" + runId);
    }

    @AfterAll
    static void closeClient() {
        try {
            cleanupRunScopedDocuments();
        } finally {
            if (facade != null) {
                facade.close();
            }
        }
    }

    @Test
    @Order(1)
    void productionClientTargetsNamedDatabase() {
        assertThat(facade.settings().databaseResourceName())
                .isEqualTo("projects/boundarymesh-dev/databases/boundarymesh");
        assertThat(facade.addressedDatabaseResource()).doesNotContain("(default)");
        assertThat(facade.listRootCollectionIds()).doesNotContain("outbox");
    }

    @Test
    @Order(2)
    void liveSequentialAuditAppend() {
        AuditAppender appender = new AuditAppender(audits, CLOCK);
        AuditEntry first = appender.append(chainId, AuditEventType.BOUNDARY_DECISION, Map.of("step", "first"));
        AuditEntry second = appender.append(chainId, AuditEventType.DELIVERY_RESULT, Map.of("step", "second"));
        AuditEntry third = appender.append(chainId, AuditEventType.QUARANTINE_CREATED, Map.of("step", "third"));
        assertThat(first.sequence()).isEqualTo(1);
        assertThat(second.sequence()).isEqualTo(2);
        assertThat(third.sequence()).isEqualTo(3);
        assertThat(second.previousHash()).isEqualTo(first.currentHash());
        assertThat(third.previousHash()).isEqualTo(second.currentHash());
        assertThat(audits.head(chainId).orElseThrow().sequence()).isEqualTo(3);
        List<AuditEntry> entries = audits.entries(chainId, 100);
        assertThat(entries).hasSize(3);
        assertThat(new AuditChainVerifier().verify(chainId, entries).status()).isEqualTo(AuditVerificationStatus.VALID);
    }

    @Test
    @Order(3)
    void liveConcurrentAuditAppendOnSameChain() throws Exception {
        AuditAppender appender = new AuditAppender(audits, Clock.systemUTC());
        runConcurrent(
                CONCURRENT_APPENDS,
                index -> appender.append(
                        chainId, AuditEventType.BOUNDARY_DECISION, Map.of("i", Integer.toString(index))));
        List<AuditEntry> entries = audits.entries(chainId, 100);
        int expected = 3 + CONCURRENT_APPENDS;
        assertThat(entries).hasSize(expected);
        assertThat(entries.stream().map(AuditEntry::sequence).collect(Collectors.toSet()))
                .hasSize(expected);
        assertThat(audits.head(chainId).orElseThrow().sequence()).isEqualTo(expected);
        assertThat(new AuditChainVerifier().verify(chainId, entries).valid()).isTrue();
    }

    @Test
    @Order(4)
    void liveTamperEvidenceUsesInMemoryCopyOnly() {
        List<AuditEntry> live = audits.entries(chainId, 100);
        assertThat(new AuditChainVerifier().verify(chainId, live).status()).isEqualTo(AuditVerificationStatus.VALID);
        AuditEntry victim = live.getFirst();
        Map<String, String> mutated = new LinkedHashMap<>(victim.attributes());
        mutated.put("outcome", "TAMPERED");
        AuditEntry altered = new AuditEntry(
                victim.id(),
                victim.chainId(),
                victim.sequence(),
                victim.occurredAt(),
                victim.eventType(),
                mutated,
                victim.previousHash(),
                victim.currentHash());
        List<AuditEntry> alteredChain = new ArrayList<>(live);
        alteredChain.set(0, altered);
        assertThat(new AuditChainVerifier().verify(chainId, alteredChain).status())
                .isEqualTo(AuditVerificationStatus.HASH_MISMATCH);
        assertThat(new AuditChainVerifier()
                        .verify(chainId, audits.entries(chainId, 100))
                        .valid())
                .isTrue();
    }

    @Test
    @Order(5)
    void liveReplayNonceFirstDuplicateAndConcurrentClaim() throws Exception {
        Instant now = FirestorePersistenceFixtures.FIXED;
        Instant expires = now.plus(Duration.ofMinutes(5));
        assertThat(replay.recordIfAbsent(firstReplayNonce, expires, now)).isTrue();
        assertThat(replay.recordIfAbsent(firstReplayNonce, expires, now)).isFalse();
        Map<String, Object> stored =
                facade.get(FirestoreCollections.replayNonce(firstReplayNonce)).orElseThrow();
        assertThat(stored.get("expires_at")).isEqualTo(expires.toString());
        assertThat(stored.keySet()).contains("expires_at", "expires_at_ms", "claimed_at", "nonce_hash");

        AtomicInteger successes = new AtomicInteger();
        runConcurrent(8, ignored -> {
            if (replay.recordIfAbsent(concurrentReplayNonce, expires, now)) {
                successes.incrementAndGet();
            }
        });
        assertThat(successes.get()).isEqualTo(1);
        assertThat(replay.recordIfAbsent(concurrentReplayNonce, expires, now)).isFalse();
    }

    @Test
    @Order(6)
    void liveRequestIdempotencyFirstWriterWins() throws Exception {
        BoundaryDecision first = FirestorePersistenceFixtures.decision(BoundaryOutcome.ALLOW);
        idempotency.putIfAbsent(requestKey, first);
        idempotency.putIfAbsent(requestKey, FirestorePersistenceFixtures.decision(BoundaryOutcome.REJECT));
        assertThat(idempotency.find(requestKey)).contains(first);

        List<BoundaryDecision> attempts = new CopyOnWriteArrayList<>();
        runConcurrent(8, ignored -> {
            BoundaryDecision decision = FirestorePersistenceFixtures.decision(BoundaryOutcome.ALLOW);
            attempts.add(decision);
            idempotency.putIfAbsent(concurrentRequestKey, decision);
        });
        BoundaryDecision stored = idempotency.find(concurrentRequestKey).orElseThrow();
        assertThat(attempts).contains(stored);
        assertThat(idempotency.find(concurrentRequestKey)).contains(stored);
    }

    @Test
    @Order(7)
    void liveDeliveryIdempotencyFirstWriterWins() throws Exception {
        DeliveryResult first = FirestorePersistenceFixtures.deliveryResult("fake-first-delivery");
        delivery.remember(deliveryKey, first);
        delivery.remember(deliveryKey, FirestorePersistenceFixtures.deliveryResult("fake-second-delivery"));
        assertThat(delivery.previous(deliveryKey)).contains(first);

        List<DeliveryResult> attempts = new CopyOnWriteArrayList<>();
        runConcurrent(8, ignored -> {
            DeliveryResult result = FirestorePersistenceFixtures.deliveryResult("fake-concurrent-" + ignored);
            attempts.add(result);
            delivery.remember(concurrentDeliveryKey, result);
        });
        DeliveryResult stored = delivery.previous(concurrentDeliveryKey).orElseThrow();
        assertThat(attempts).contains(stored);
        assertThat(delivery.previous(concurrentDeliveryKey)).contains(stored);
    }

    @Test
    @Order(8)
    void liveQuarantineLifecycleWithoutSecretFields() {
        QuarantineOrchestrator orchestrator = new QuarantineOrchestrator(quarantine, CLOCK);
        QuarantineItem opened = FirestorePersistenceFixtures.quarantine(quarantineId, observationIntegration);
        quarantine.save(opened);
        assertThat(quarantine.find(opened.id())).contains(opened);
        orchestrator.beginReprocess(opened.id());
        QuarantineItem held = orchestrator.remain(opened.id(), "fake-provider-still-unavailable");
        assertThat(held.id()).isEqualTo(new QuarantineId(quarantineId));
        assertThat(held.status()).isEqualTo(QuarantineStatus.OPEN);
        assertThat(held.recoveryAttempts()).isEqualTo(1);
        assertThat(held.reason().name()).isEqualTo(opened.reason().name());
        Map<String, Object> raw =
                facade.get(FirestoreCollections.quarantine(quarantineId)).orElseThrow();
        assertThat(raw.keySet())
                .noneMatch(key -> key.toLowerCase(Locale.ROOT).contains("secret")
                        || key.toLowerCase(Locale.ROOT).contains("token")
                        || key.toLowerCase(Locale.ROOT).contains("authorization"));
        assertThat(raw.values().stream().map(String::valueOf).toList())
                .noneMatch(value -> value.contains("BEGIN PRIVATE") || value.contains("Bearer "));
        System.out.println("LIVE_QUARANTINE_KEYS=" + new TreeSet<>(raw.keySet()));
    }

    @Test
    @Order(9)
    void liveAttestationPersistsPublicMaterialOnly() {
        ExchangeAttestation attestation =
                FirestorePersistenceFixtures.attestation(attestationId, observationIntegration);
        attestations.save(attestation);
        assertThat(attestations.find(attestation.id())).contains(attestation);
        assertThat(attestations.list(10)).contains(attestation);
        Map<String, Object> raw =
                facade.get(FirestoreCollections.attestation(attestationId)).orElseThrow();
        assertThat(raw).doesNotContainKeys("private_key", "privateKey", "client_secret");
        assertThat(String.valueOf(raw.get("signer_key_id"))).isEqualTo("local-jca-p256");
        assertThat(String.valueOf(raw.get("signature_base64"))).doesNotContain("BEGIN PRIVATE");
        System.out.println("LIVE_ATTESTATION_KEYS=" + new TreeSet<>(raw.keySet()));
    }

    @Test
    @Order(10)
    void liveObservedCrossingPersistsStableDestination() {
        ObservedCrossingStore.Observation first =
                FirestorePersistenceFixtures.observation(observationIntegration, destination);
        observations.record(first);
        assertThat(observations.findByDestination(destination)).contains(first);
        assertThat(observations.find(observationIntegration)).contains(first);
        ObservedCrossingStore.Observation updated =
                FirestorePersistenceFixtures.observation(observationIntegration, destination.toUpperCase(Locale.ROOT));
        observations.record(updated);
        assertThat(observations.findByDestination(destination)).contains(updated);
        assertThat(observations.all()).containsEntry(ObservedCrossingStore.destinationKey(destination), updated);
    }

    @Test
    @Order(11)
    void dataSurvivesProductionClientRecreation() {
        facade.close();
        GoogleFirestoreFacade restarted = GoogleFirestoreFacade.openProduction(settings);
        try {
            assertThat(restarted).isInstanceOf(GoogleFirestoreFacade.class);
            assertThat(restarted.addressedDatabaseResource())
                    .isEqualTo("projects/boundarymesh-dev/databases/boundarymesh");
            FirestoreAuditRepository nextAudits = new FirestoreAuditRepository(restarted);
            FirestoreAttestationRepository nextAttestations = new FirestoreAttestationRepository(restarted);
            FirestoreQuarantineRepository nextQuarantine = new FirestoreQuarantineRepository(restarted);
            FirestoreObservedCrossingStore nextObservations = new FirestoreObservedCrossingStore(restarted);
            FirestoreReplayNonceStore nextReplay = new FirestoreReplayNonceStore(restarted);
            assertThat(nextAudits.head(chainId).orElseThrow().sequence()).isEqualTo(3 + CONCURRENT_APPENDS);
            assertThat(new AuditChainVerifier()
                            .verify(chainId, nextAudits.entries(chainId, 100))
                            .valid())
                    .isTrue();
            assertThat(nextAttestations.find(new AttestationId(attestationId))).isPresent();
            assertThat(nextQuarantine.find(new QuarantineId(quarantineId))).isPresent();
            assertThat(nextObservations.findByDestination(destination)).isPresent();
            assertThat(nextReplay.recordIfAbsent(
                            firstReplayNonce,
                            FirestorePersistenceFixtures.FIXED.plus(Duration.ofMinutes(5)),
                            FirestorePersistenceFixtures.FIXED))
                    .isFalse();
        } finally {
            restarted.close();
            facade = GoogleFirestoreFacade.openProduction(settings);
            bindAdapters(facade);
        }
    }

    @Test
    @Order(12)
    void liveDocumentsUseExpectedCollectionsAndSafeKeys() {
        List<String> collections = facade.listRootCollectionIds();
        assertThat(collections).doesNotContain("outbox");
        assertThat(collections).isSubsetOf(EXPECTED_COLLECTIONS);
        for (String collection : collections) {
            for (String id : facade.listDocumentIds(collection)) {
                if (!belongsToRun(id)) {
                    continue;
                }
                Map<String, Object> raw =
                        facade.get(FirestoreDocumentPath.of(collection, id)).orElseThrow();
                assertNoSensitiveMaterial(collection, raw);
                System.out.println(
                        "LIVE_DOC=" + collection + "/" + redactId(id) + " keys=" + new TreeSet<>(raw.keySet()));
            }
        }
        List<Map<String, Object>> entries = facade.listSubcollection(
                FirestoreCollections.auditHead(chainId), FirestoreCollections.AUDIT_ENTRIES, "sequence", false, 0);
        assertThat(entries).isNotEmpty();
        entries.forEach(row -> assertNoSensitiveMaterial("auditChains/entries", row));
        System.out.println(
                "LIVE_AUDIT_ENTRY_KEYS=" + new TreeSet<>(entries.getFirst().keySet()));
    }

    @Test
    @Order(13)
    void cleanupRemovesOnlyThisRunAndLeavesDatabase() {
        cleanupRunScopedDocuments();
        assertThat(remainingRunScopedPaths()).isEmpty();
        assertThat(facade.get(FirestoreCollections.auditHead(chainId))).isEmpty();
        assertThat(facade.get(FirestoreCollections.attestation(attestationId))).isEmpty();
        assertThat(facade.get(FirestoreCollections.quarantine(quarantineId))).isEmpty();
        assertThat(facade.get(FirestoreCollections.replayNonce(firstReplayNonce)))
                .isEmpty();
        assertThat(facade.get(FirestoreCollections.replayNonce(concurrentReplayNonce)))
                .isEmpty();
        assertThat(facade.get(FirestoreCollections.idempotency(requestKey))).isEmpty();
        assertThat(facade.get(FirestoreCollections.idempotency(concurrentRequestKey)))
                .isEmpty();
        assertThat(facade.get(FirestoreCollections.deliveryIdempotency(deliveryKey)))
                .isEmpty();
        assertThat(facade.get(FirestoreCollections.deliveryIdempotency(concurrentDeliveryKey)))
                .isEmpty();
        assertThat(facade.get(FirestoreCollections.observedCrossing(destination)))
                .isEmpty();
        assertThat(settings.databaseResourceName()).isEqualTo("projects/boundarymesh-dev/databases/boundarymesh");
    }

    private static void bindAdapters(GoogleFirestoreFacade client) {
        audits = new FirestoreAuditRepository(client);
        replay = new FirestoreReplayNonceStore(client);
        idempotency = new FirestoreIdempotencyStore(client);
        delivery = new FirestoreDeliveryIdempotencyStore(client);
        quarantine = new FirestoreQuarantineRepository(client);
        attestations = new FirestoreAttestationRepository(client);
        observations = new FirestoreObservedCrossingStore(client);
    }

    private static void cleanupRunScopedDocuments() {
        if (facade == null || runId == null || cleaned) {
            return;
        }
        List<AuditEntry> entries = List.of();
        try {
            entries = audits.entries(chainId, 100);
        } catch (RuntimeException ignored) {
            // Cleanup still deletes known document ids below.
        }
        for (AuditEntry entry : entries) {
            facade.delete(FirestoreCollections.auditEntry(chainId, entry.sequence()));
        }
        facade.delete(FirestoreCollections.auditHead(chainId));
        facade.delete(FirestoreCollections.attestation(attestationId));
        facade.delete(FirestoreCollections.quarantine(quarantineId));
        facade.delete(FirestoreCollections.replayNonce(firstReplayNonce));
        facade.delete(FirestoreCollections.replayNonce(concurrentReplayNonce));
        facade.delete(FirestoreCollections.idempotency(requestKey));
        facade.delete(FirestoreCollections.idempotency(concurrentRequestKey));
        facade.delete(FirestoreCollections.deliveryIdempotency(deliveryKey));
        facade.delete(FirestoreCollections.deliveryIdempotency(concurrentDeliveryKey));
        facade.delete(FirestoreCollections.observedCrossing(destination));
        cleaned = true;
    }

    private static List<String> remainingRunScopedPaths() {
        List<String> remaining = new ArrayList<>();
        for (String collection : facade.listRootCollectionIds()) {
            if ("outbox".equals(collection)) {
                remaining.add(collection);
                continue;
            }
            for (String id : facade.listDocumentIds(collection)) {
                if (belongsToRun(id)) {
                    remaining.add(collection + "/" + redactId(id));
                }
            }
        }
        if (facade.get(FirestoreCollections.auditHead(chainId)).isPresent()) {
            remaining.add("auditChains/" + redactId(chainId.value()));
        }
        return remaining;
    }

    private static boolean belongsToRun(String documentId) {
        String value = documentId.toLowerCase(Locale.ROOT);
        return value.contains(runId.toLowerCase(Locale.ROOT))
                || value.contains(
                        ObservedCrossingStore.destinationKey(destination).toLowerCase(Locale.ROOT));
    }

    private static void assertNoSensitiveMaterial(String collection, Map<String, Object> raw) {
        assertThat(raw.keySet()).noneMatch(key -> {
            String lower = key.toLowerCase(Locale.ROOT);
            return lower.contains("token")
                    || lower.contains("authorization")
                    || lower.contains("private")
                    || lower.contains("client_secret")
                    || lower.contains("adc");
        });
        assertThat(flatten(collection, raw))
                .noneMatch(value -> value.contains("BEGIN PRIVATE")
                        || value.contains("Bearer ")
                        || value.toLowerCase(Locale.ROOT).contains("client_secret")
                        || value.contains("eyJ"));
    }

    private static List<String> flatten(String prefix, Object value) {
        List<String> values = new ArrayList<>();
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                values.addAll(flatten(prefix + "." + entry.getKey(), entry.getValue()));
            }
            return values;
        }
        if (value instanceof List<?> list) {
            for (Object item : list) {
                values.addAll(flatten(prefix, item));
            }
            return values;
        }
        if (value != null) {
            values.add(String.valueOf(value));
        }
        return values;
    }

    private static String redactId(String id) {
        if (id == null || id.length() < 12) {
            return "liveverify-*";
        }
        return id.substring(0, Math.min(id.length(), 22)) + "…";
    }

    private static String randomSuffix() {
        byte[] bytes = new byte[4];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private static void runConcurrent(int n, ConcurrentWork work) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(n, 8));
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(n);
        List<Throwable> errors = new CopyOnWriteArrayList<>();
        for (int i = 0; i < n; i++) {
            int index = i;
            pool.submit(() -> {
                try {
                    start.await();
                    work.run(index);
                } catch (Throwable error) {
                    errors.add(error);
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertThat(done.await(180, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();
        assertThat(errors).isEmpty();
    }

    @FunctionalInterface
    private interface ConcurrentWork {
        void run(int index) throws Exception;
    }
}
