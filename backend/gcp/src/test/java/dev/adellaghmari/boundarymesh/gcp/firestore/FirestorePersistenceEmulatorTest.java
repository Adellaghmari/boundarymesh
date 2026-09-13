package dev.adellaghmari.boundarymesh.gcp.firestore;

import static org.assertj.core.api.Assertions.assertThat;

import dev.adellaghmari.boundarymesh.application.audit.AuditAppender;
import dev.adellaghmari.boundarymesh.application.audit.AuditChainVerifier;
import dev.adellaghmari.boundarymesh.application.graph.ObservedCrossingStore;
import dev.adellaghmari.boundarymesh.application.quarantine.QuarantineOrchestrator;
import dev.adellaghmari.boundarymesh.application.webhook.ReplayNonceStore;
import dev.adellaghmari.boundarymesh.domain.BoundaryDecision;
import dev.adellaghmari.boundarymesh.domain.BoundaryOutcome;
import dev.adellaghmari.boundarymesh.domain.audit.AuditChainId;
import dev.adellaghmari.boundarymesh.domain.audit.AuditEntry;
import dev.adellaghmari.boundarymesh.domain.audit.AuditEventType;
import dev.adellaghmari.boundarymesh.domain.audit.AuditVerificationStatus;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryResult;
import dev.adellaghmari.boundarymesh.domain.quarantine.QuarantineItem;
import dev.adellaghmari.boundarymesh.domain.quarantine.QuarantineStatus;
import dev.adellaghmari.boundarymesh.gcp.GcpEmulatorGuard;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

@Tag(GcpEmulatorGuard.EMULATOR_TEST_TAG)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FirestorePersistenceEmulatorTest {

    private static final Clock CLOCK = Clock.fixed(FirestorePersistenceFixtures.FIXED, ZoneOffset.UTC);

    static FirestoreEmulatorSupport emulator;
    static FirestoreSettings settings;
    static FirestoreFacade facade;
    static FirestoreAuditRepository audits;
    static FirestoreReplayNonceStore replay;
    static FirestoreIdempotencyStore idempotency;
    static FirestoreDeliveryIdempotencyStore delivery;
    static FirestoreQuarantineRepository quarantine;
    static FirestoreAttestationRepository attestations;
    static FirestoreObservedCrossingStore observations;

    @BeforeAll
    static void openClient() {
        emulator = FirestoreEmulatorSupport.start();
        settings = FirestoreSettings.emulator("boundarymesh-dev", emulator.endpoint());
        assertThat(settings.databaseId()).isEqualTo("boundarymesh");
        assertThat(settings.usesEmulator()).isTrue();
        facade = GoogleFirestoreFacade.openEmulator(settings);
        assertThat(facade.settings().databaseId()).isNotEqualTo("(default)");
        audits = new FirestoreAuditRepository(facade);
        replay = new FirestoreReplayNonceStore(facade);
        idempotency = new FirestoreIdempotencyStore(facade);
        delivery = new FirestoreDeliveryIdempotencyStore(facade);
        quarantine = new FirestoreQuarantineRepository(facade);
        attestations = new FirestoreAttestationRepository(facade);
        observations = new FirestoreObservedCrossingStore(facade);
    }

    @AfterAll
    static void closeClient() {
        if (facade != null) {
            facade.close();
        }
        if (emulator != null) {
            emulator.close();
        }
    }

    @Test
    @Order(1)
    void auditFirstSequentialConcurrentAndTamperEvidence() throws Exception {
        AuditChainId chain = new AuditChainId("audit-first-seq");
        AuditAppender appender = new AuditAppender(audits, CLOCK);
        AuditEntry first = appender.append(chain, AuditEventType.BOUNDARY_DECISION, Map.of("step", "first"));
        AuditEntry second = appender.append(chain, AuditEventType.DELIVERY_RESULT, Map.of("step", "second"));
        assertThat(first.sequence()).isEqualTo(1);
        assertThat(second.sequence()).isEqualTo(2);
        assertThat(second.previousHash()).isEqualTo(first.currentHash());
        assertThat(audits.head(chain).orElseThrow().sequence()).isEqualTo(2);
        assertThat(new AuditChainVerifier()
                        .verify(chain, audits.entries(chain, 100))
                        .status())
                .isEqualTo(AuditVerificationStatus.VALID);

        AuditChainId concurrent = new AuditChainId("audit-concurrent");
        AuditAppender concurrentAppender = new AuditAppender(audits, Clock.systemUTC());
        int n = 12;
        runConcurrent(
                n,
                index -> concurrentAppender.append(
                        concurrent, AuditEventType.BOUNDARY_DECISION, Map.of("i", Integer.toString(index))));
        List<AuditEntry> entries = audits.entries(concurrent, 100);
        assertThat(entries).hasSize(n);
        assertThat(entries.stream().map(AuditEntry::sequence).collect(Collectors.toSet()))
                .hasSize(n);
        assertThat(audits.head(concurrent).orElseThrow().sequence()).isEqualTo(n);
        assertThat(new AuditChainVerifier().verify(concurrent, entries).valid()).isTrue();

        AuditEntry victim = entries.getFirst();
        Map<String, Object> mutated = new LinkedHashMap<>(FirestoreCodecs.auditEntry(victim));
        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = new LinkedHashMap<>((Map<String, Object>) mutated.get("attributes"));
        attributes.put("outcome", "TAMPERED");
        mutated.put("attributes", attributes);
        facade.set(FirestoreCollections.auditEntry(concurrent, victim.sequence()), mutated);
        assertThat(new AuditChainVerifier()
                        .verify(concurrent, audits.entries(concurrent, 100))
                        .status())
                .isEqualTo(AuditVerificationStatus.HASH_MISMATCH);
        assertThat(mutated.values().stream().map(String::valueOf).toList())
                .noneMatch(value -> value.contains("BEGIN PRIVATE") || value.contains("TEST-SECRET"));
    }

    @Test
    @Order(2)
    void replayNonceFirstDuplicateAndConcurrentClaim() throws Exception {
        Instant now = FirestorePersistenceFixtures.FIXED;
        Instant expires = now.plus(Duration.ofMinutes(5));
        String firstNonce = "fake-replay-nonce-15fa-first";
        assertThat(replay.recordIfAbsent(firstNonce, expires, now)).isTrue();
        assertThat(replay.recordIfAbsent(firstNonce, expires, now)).isFalse();

        String concurrentNonce = "fake-replay-nonce-15fa-concurrent";
        AtomicInteger successes = new AtomicInteger();
        runConcurrent(16, ignored -> {
            if (replay.recordIfAbsent(concurrentNonce, expires, now)) {
                successes.incrementAndGet();
            }
        });
        assertThat(successes.get()).isEqualTo(1);
        assertThat(replay.recordIfAbsent(concurrentNonce, expires, now)).isFalse();
    }

    @Test
    @Order(3)
    void requestIdempotencySuppressesDuplicatesUnderConcurrency() throws Exception {
        String eventKey = "fake-request-event-15fa";
        BoundaryDecision first = FirestorePersistenceFixtures.decision(BoundaryOutcome.ALLOW);
        idempotency.putIfAbsent(eventKey, first);
        idempotency.putIfAbsent(eventKey, FirestorePersistenceFixtures.decision(BoundaryOutcome.REJECT));
        assertThat(idempotency.find(eventKey)).contains(first);

        String concurrentKey = "fake-request-event-15fa-concurrent";
        List<BoundaryDecision> attempts = new CopyOnWriteArrayList<>();
        runConcurrent(12, ignored -> {
            BoundaryDecision decision = FirestorePersistenceFixtures.decision(BoundaryOutcome.ALLOW);
            attempts.add(decision);
            idempotency.putIfAbsent(concurrentKey, decision);
        });
        BoundaryDecision stored = idempotency.find(concurrentKey).orElseThrow();
        assertThat(attempts).contains(stored);
        assertThat(idempotency.find(concurrentKey)).contains(stored);
    }

    @Test
    @Order(4)
    void deliveryIdempotencySuppressesDuplicatesUnderConcurrency() throws Exception {
        String instruction = "fake-delivery-instruction-15fa";
        DeliveryResult first = FirestorePersistenceFixtures.deliveryResult("fake-first-delivery");
        delivery.remember(instruction, first);
        delivery.remember(instruction, FirestorePersistenceFixtures.deliveryResult("fake-second-delivery"));
        assertThat(delivery.previous(instruction)).contains(first);

        String concurrentInstruction = "fake-delivery-instruction-15fa-concurrent";
        List<DeliveryResult> attempts = new CopyOnWriteArrayList<>();
        runConcurrent(12, ignored -> {
            DeliveryResult result = FirestorePersistenceFixtures.deliveryResult("fake-concurrent-" + ignored);
            attempts.add(result);
            delivery.remember(concurrentInstruction, result);
        });
        DeliveryResult stored = delivery.previous(concurrentInstruction).orElseThrow();
        assertThat(attempts).contains(stored);
        assertThat(delivery.previous(concurrentInstruction)).contains(stored);
    }

    @Test
    @Order(5)
    void quarantinePersistsLifecycleWithoutSecretFields() {
        QuarantineOrchestrator orchestrator = new QuarantineOrchestrator(quarantine, CLOCK);
        QuarantineItem opened = FirestorePersistenceFixtures.quarantine("quayforge-orders");
        quarantine.save(opened);
        assertThat(quarantine.find(opened.id())).contains(opened);
        orchestrator.beginReprocess(opened.id());
        QuarantineItem held = orchestrator.remain(opened.id(), "fake-provider-still-unavailable");
        assertThat(held.status()).isEqualTo(QuarantineStatus.OPEN);
        assertThat(held.recoveryAttempts()).isEqualTo(1);
        assertThat(quarantine.list()).extracting(QuarantineItem::id).contains(opened.id());
        Map<String, Object> raw =
                facade.get(FirestoreCollections.quarantine(opened.id().value())).orElseThrow();
        assertThat(raw.keySet())
                .noneMatch(key -> key.toLowerCase().contains("secret")
                        || key.toLowerCase().contains("token")
                        || key.toLowerCase().contains("authorization"));
        assertThat(raw.values().stream().map(String::valueOf).toList())
                .noneMatch(value -> value.contains("BEGIN PRIVATE") || value.contains("Bearer "));
    }

    @Test
    @Order(6)
    void attestationPersistsWithoutPrivateKeyMaterial() {
        var attestation = FirestorePersistenceFixtures.attestation("quayforge-orders");
        attestations.save(attestation);
        assertThat(attestations.find(attestation.id())).contains(attestation);
        assertThat(attestations.list(10)).contains(attestation);
        Map<String, Object> raw = facade.get(
                        FirestoreCollections.attestation(attestation.id().value()))
                .orElseThrow();
        assertThat(raw).doesNotContainKeys("private_key", "privateKey", "client_secret");
        assertThat(String.valueOf(raw.get("signer_key_id"))).isEqualTo("local-jca-p256");
        assertThat(String.valueOf(raw.get("signature_base64"))).doesNotContain("BEGIN PRIVATE");
    }

    @Test
    @Order(7)
    void observedCrossingPersistsStableDestinationIdentity() {
        ObservedCrossingStore.Observation first =
                FirestorePersistenceFixtures.observation("quayforge-orders", "COVECRM");
        observations.record(first);
        assertThat(observations.findByDestination("covecrm")).contains(first);
        assertThat(observations.find("quayforge-orders")).contains(first);
        ObservedCrossingStore.Observation updated =
                FirestorePersistenceFixtures.observation("quayforge-orders", "CoveCRM");
        observations.record(updated);
        assertThat(observations.findByDestination("COVECRM")).contains(updated);
        assertThat(observations.all()).containsEntry("COVECRM", updated);
    }

    @Test
    @Order(8)
    void dataSurvivesAdapterRecreationWhileEmulatorRuns() {
        var attestation = FirestorePersistenceFixtures.attestation("m365-calendar");
        attestations.save(attestation);
        ReplayNonceStore replayStore = replay;
        assertThat(replayStore.recordIfAbsent(
                        "fake-replay-nonce-15fa-restart",
                        FirestorePersistenceFixtures.FIXED.plusSeconds(60),
                        FirestorePersistenceFixtures.FIXED))
                .isTrue();
        facade.close();

        FirestoreFacade restarted = GoogleFirestoreFacade.openEmulator(settings);
        try {
            FirestoreAttestationRepository nextAttestations = new FirestoreAttestationRepository(restarted);
            FirestoreReplayNonceStore nextReplay = new FirestoreReplayNonceStore(restarted);
            assertThat(nextAttestations.find(attestation.id())).contains(attestation);
            assertThat(nextReplay.recordIfAbsent(
                            "fake-replay-nonce-15fa-restart",
                            FirestorePersistenceFixtures.FIXED.plusSeconds(60),
                            FirestorePersistenceFixtures.FIXED))
                    .isFalse();
        } finally {
            restarted.close();
            facade = GoogleFirestoreFacade.openEmulator(settings);
            audits = new FirestoreAuditRepository(facade);
            replay = new FirestoreReplayNonceStore(facade);
            idempotency = new FirestoreIdempotencyStore(facade);
            delivery = new FirestoreDeliveryIdempotencyStore(facade);
            quarantine = new FirestoreQuarantineRepository(facade);
            attestations = new FirestoreAttestationRepository(facade);
            observations = new FirestoreObservedCrossingStore(facade);
        }
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
        assertThat(done.await(90, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();
        assertThat(errors).isEmpty();
    }

    @FunctionalInterface
    private interface ConcurrentWork {
        void run(int index) throws Exception;
    }
}
