package dev.adellaghmari.boundarymesh.application.outbox;

import dev.adellaghmari.boundarymesh.application.delivery.DeliveryPort;
import dev.adellaghmari.boundarymesh.application.delivery.DeliveryRequest;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryResult;
import java.time.Clock;
import java.util.Objects;

/**
 * Production submission boundary. An approved instruction is made durable before it is published
 * to the worker queue. Provider delivery happens only in the worker.
 */
public final class OutboxDeliveryPort implements DeliveryPort {

    private final OutboxRepository outbox;
    private final OutboxPublisher publisher;
    private final Clock clock;

    public OutboxDeliveryPort(OutboxRepository outbox, MessagePublisherPort publisher, Clock clock) {
        this.outbox = Objects.requireNonNull(outbox, "outbox");
        this.publisher = new OutboxPublisher(outbox, Objects.requireNonNull(publisher, "publisher"), clock);
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public DeliveryResult deliver(DeliveryRequest request) {
        Objects.requireNonNull(request, "request");
        OutboxRecord pending = OutboxRecord.pending(request.instruction(), clock.instant());
        outbox.savePending(pending);
        publisher.publish(pending.instructionId());
        return DeliveryResult.notAttempted("Approved delivery queued for asynchronous worker processing.");
    }
}
