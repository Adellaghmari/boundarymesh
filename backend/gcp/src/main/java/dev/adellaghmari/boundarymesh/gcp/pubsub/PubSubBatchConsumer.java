package dev.adellaghmari.boundarymesh.gcp.pubsub;

import dev.adellaghmari.boundarymesh.application.outbox.ConsumeDisposition;
import java.util.List;

/** Narrow pull surface used by the continuous worker lifecycle. */
interface PubSubBatchConsumer extends AutoCloseable {

    List<ConsumeDisposition> pullAndHandle(int maxMessages);

    @Override
    void close();
}
