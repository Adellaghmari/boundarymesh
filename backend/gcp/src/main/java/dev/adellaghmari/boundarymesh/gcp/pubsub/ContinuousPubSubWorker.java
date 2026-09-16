package dev.adellaghmari.boundarymesh.gcp.pubsub;

import dev.adellaghmari.boundarymesh.application.outbox.DeliveryInstructionHandler;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

/** Long-running pull lifecycle for a Cloud Run Worker Pool instance. */
public final class ContinuousPubSubWorker implements AutoCloseable {

    static final int BATCH_SIZE = 10;
    static final Duration FAILURE_BACKOFF = Duration.ofSeconds(1);

    private final PubSubBatchConsumer consumer;
    private final AtomicBoolean running = new AtomicBoolean();
    private volatile Thread thread;

    ContinuousPubSubWorker(PubSubBatchConsumer consumer) {
        this.consumer = Objects.requireNonNull(consumer, "consumer");
    }

    public static ContinuousPubSubWorker connect(PubSubSettings settings, DeliveryInstructionHandler handler) {
        return new ContinuousPubSubWorker(GooglePubSubPullConsumer.connect(settings, handler));
    }

    public synchronized void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        thread = Thread.ofPlatform().name("boundarymesh-pubsub-pull").start(this::run);
    }

    public boolean running() {
        return running.get();
    }

    private void run() {
        while (running.get()) {
            try {
                consumer.pullAndHandle(BATCH_SIZE);
            } catch (RuntimeException ex) {
                if (running.get()) {
                    LockSupport.parkNanos(FAILURE_BACKOFF.toNanos());
                }
            }
        }
    }

    @Override
    public synchronized void close() {
        running.set(false);
        try {
            consumer.close();
        } catch (Exception ignored) {
            // Shutdown must continue to interrupt and join the pull thread.
        }
        Thread current = thread;
        if (current == null) {
            return;
        }
        current.interrupt();
        try {
            current.join(Duration.ofSeconds(5).toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
