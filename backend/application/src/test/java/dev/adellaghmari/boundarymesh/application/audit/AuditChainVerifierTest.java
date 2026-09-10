package dev.adellaghmari.boundarymesh.application.audit;

import static org.assertj.core.api.Assertions.assertThat;

import dev.adellaghmari.boundarymesh.domain.audit.AuditChainId;
import dev.adellaghmari.boundarymesh.domain.audit.AuditEntry;
import dev.adellaghmari.boundarymesh.domain.audit.AuditEventType;
import dev.adellaghmari.boundarymesh.domain.audit.AuditHash;
import dev.adellaghmari.boundarymesh.domain.audit.AuditVerificationStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class AuditChainVerifierTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-10T12:00:00Z"), ZoneOffset.UTC);
    private final AuditChainId chain = new AuditChainId("quayforge-orders");

    @Test
    void untouchedChainIsValid() {
        List<AuditEntry> entries = appendThree();
        var result = new AuditChainVerifier().verify(chain, entries);
        assertThat(result.status()).isEqualTo(AuditVerificationStatus.VALID);
        assertThat(result.entriesChecked()).isEqualTo(3);
    }

    @Test
    void modifiedMetadataInvalidatesHash() {
        List<AuditEntry> entries = appendThree();
        AuditEntry first = entries.getFirst();
        Map<String, String> mutated = new LinkedHashMap<>(first.attributes());
        mutated.put("outcome", "TAMPERED");
        entries.set(
                0,
                new AuditEntry(
                        first.id(),
                        first.chainId(),
                        first.sequence(),
                        first.occurredAt(),
                        first.eventType(),
                        mutated,
                        first.previousHash(),
                        first.currentHash()));
        assertThat(new AuditChainVerifier().verify(chain, entries).status())
                .isEqualTo(AuditVerificationStatus.HASH_MISMATCH);
    }

    @Test
    void modifiedTimestampInvalidatesHash() {
        List<AuditEntry> entries = appendThree();
        AuditEntry first = entries.getFirst();
        entries.set(
                0,
                new AuditEntry(
                        first.id(),
                        first.chainId(),
                        first.sequence(),
                        first.occurredAt().plusSeconds(9),
                        first.eventType(),
                        first.attributes(),
                        first.previousHash(),
                        first.currentHash()));
        assertThat(new AuditChainVerifier().verify(chain, entries).status())
                .isEqualTo(AuditVerificationStatus.HASH_MISMATCH);
    }

    @Test
    void changedSequenceFails() {
        List<AuditEntry> entries = appendThree();
        AuditEntry third = entries.get(2);
        entries.set(
                2,
                new AuditEntry(
                        third.id(),
                        third.chainId(),
                        4,
                        third.occurredAt(),
                        third.eventType(),
                        third.attributes(),
                        third.previousHash(),
                        third.currentHash()));
        assertThat(new AuditChainVerifier().verify(chain, entries).status())
                .isEqualTo(AuditVerificationStatus.SEQUENCE_GAP);
    }

    @Test
    void changedPreviousHashFails() {
        List<AuditEntry> entries = appendThree();
        AuditEntry second = entries.get(1);
        entries.set(
                1,
                new AuditEntry(
                        second.id(),
                        second.chainId(),
                        second.sequence(),
                        second.occurredAt(),
                        second.eventType(),
                        second.attributes(),
                        AuditHash.GENESIS,
                        second.currentHash()));
        assertThat(new AuditChainVerifier().verify(chain, entries).status())
                .isEqualTo(AuditVerificationStatus.LINK_MISMATCH);
    }

    @Test
    void removedEntryFails() {
        List<AuditEntry> entries = appendThree();
        entries.remove(1);
        assertThat(new AuditChainVerifier().verify(chain, entries).status())
                .isEqualTo(AuditVerificationStatus.SEQUENCE_GAP);
    }

    @Test
    void reorderedEntriesFail() {
        List<AuditEntry> entries = appendThree();
        Collections.swap(entries, 0, 2);
        assertThat(new AuditChainVerifier().verify(chain, entries).status())
                .isEqualTo(AuditVerificationStatus.REORDERED);
    }

    @Test
    void foreignChainEntryFails() {
        List<AuditEntry> entries = appendThree();
        AuditEntry first = entries.getFirst();
        entries.set(
                0,
                new AuditEntry(
                        first.id(),
                        new AuditChainId("other-integration"),
                        first.sequence(),
                        first.occurredAt(),
                        first.eventType(),
                        first.attributes(),
                        first.previousHash(),
                        first.currentHash()));
        assertThat(new AuditChainVerifier().verify(chain, entries).status())
                .isEqualTo(AuditVerificationStatus.FOREIGN_CHAIN);
    }

    @Test
    void concurrentAppendDoesNotReuseSequence() throws Exception {
        InMemoryAuditRepository repository = new InMemoryAuditRepository();
        AuditAppender appender = new AuditAppender(repository, clock);
        int n = 20;
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(n);
        for (int i = 0; i < n; i++) {
            int index = i;
            pool.submit(() -> {
                try {
                    start.await();
                    appender.append(chain, AuditEventType.BOUNDARY_DECISION, Map.of("i", Integer.toString(index)));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();
        List<AuditEntry> entries = repository.entries(chain, 100);
        assertThat(entries).hasSize(n);
        assertThat(entries.stream().map(AuditEntry::sequence).collect(Collectors.toSet()))
                .hasSize(n);
        assertThat(new AuditChainVerifier().verify(chain, entries).valid()).isTrue();
        assertThat(entries.stream()
                        .flatMap(item -> item.attributes().values().stream())
                        .toList())
                .noneMatch(value -> value.contains("TEST-SECRET") || value.contains("BEGIN PRIVATE"));
    }

    private List<AuditEntry> appendThree() {
        InMemoryAuditRepository repository = new InMemoryAuditRepository();
        AuditAppender appender = new AuditAppender(repository, clock);
        appender.append(chain, AuditEventType.BOUNDARY_DECISION, Map.of("outcome", "ALLOW"));
        appender.append(chain, AuditEventType.DELIVERY_RESULT, Map.of("kind", "DELIVERED"));
        appender.append(chain, AuditEventType.ATTESTATION_CREATED, Map.of("status", "SIGNED"));
        return new ArrayList<>(repository.entries(chain, 100));
    }
}
