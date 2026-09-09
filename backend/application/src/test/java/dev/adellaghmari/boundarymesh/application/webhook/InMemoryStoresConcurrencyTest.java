package dev.adellaghmari.boundarymesh.application.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class InMemoryStoresConcurrencyTest {

    @Test
    void onlyOneCallerWinsANonceRace() throws Exception {
        ConcurrentReplayStore store = new ConcurrentReplayStore();
        Instant now = Instant.parse("2026-09-10T00:00:00Z");
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger winners = new AtomicInteger();
        Thread first = new Thread(() -> {
            await(start);
            if (store.recordIfAbsent("same", now.plusSeconds(300), now)) {
                winners.incrementAndGet();
            }
        });
        Thread second = new Thread(() -> {
            await(start);
            if (store.recordIfAbsent("same", now.plusSeconds(300), now)) {
                winners.incrementAndGet();
            }
        });
        first.start();
        second.start();
        start.countDown();
        first.join();
        second.join();
        assertThat(winners.get()).isEqualTo(1);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private static final class ConcurrentReplayStore implements ReplayNonceStore {
        private final java.util.concurrent.ConcurrentHashMap<String, Instant> seen =
                new java.util.concurrent.ConcurrentHashMap<>();

        @Override
        public boolean recordIfAbsent(String nonceHash, Instant expiresAt, Instant now) {
            return seen.putIfAbsent(nonceHash, expiresAt) == null;
        }
    }
}
