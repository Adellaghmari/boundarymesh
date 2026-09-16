package dev.adellaghmari.boundarymesh.gcp.pubsub;

import static org.assertj.core.api.Assertions.assertThat;

import dev.adellaghmari.boundarymesh.application.outbox.ConsumeDisposition;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ContinuousPubSubWorkerTest {

    @Test
    void startsConsumesAndClosesWithoutLeakingThePullThread() throws Exception {
        FakeConsumer consumer = new FakeConsumer();
        ContinuousPubSubWorker worker = new ContinuousPubSubWorker(consumer);

        worker.start();
        assertThat(consumer.firstPull.await(2, TimeUnit.SECONDS)).isTrue();
        worker.close();

        assertThat(worker.running()).isFalse();
        assertThat(consumer.closed).isTrue();
        assertThat(consumer.pulls.get()).isPositive();
    }

    private static final class FakeConsumer implements PubSubBatchConsumer {
        private final CountDownLatch firstPull = new CountDownLatch(1);
        private final AtomicInteger pulls = new AtomicInteger();
        private final AtomicBoolean open = new AtomicBoolean(true);
        private volatile boolean closed;

        @Override
        public List<ConsumeDisposition> pullAndHandle(int maxMessages) {
            pulls.incrementAndGet();
            firstPull.countDown();
            while (open.get()) {
                try {
                    Thread.sleep(Duration.ofMillis(10));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            return List.of();
        }

        @Override
        public void close() {
            closed = true;
            open.set(false);
        }
    }
}
