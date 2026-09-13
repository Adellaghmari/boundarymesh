package dev.adellaghmari.boundarymesh.application.outbox;

import java.util.ArrayList;
import java.util.List;

/** Test publisher. Never talks to Pub/Sub. */
public final class RecordingMessagePublisher implements MessagePublisherPort {

    private final List<DeliveryInstructionMessage> published = new ArrayList<>();
    private RuntimeException nextFailure;

    public void failNext(RuntimeException failure) {
        this.nextFailure = failure;
    }

    @Override
    public void publish(DeliveryInstructionMessage message) {
        if (nextFailure != null) {
            RuntimeException failure = nextFailure;
            nextFailure = null;
            throw failure;
        }
        published.add(message);
    }

    public List<DeliveryInstructionMessage> published() {
        return List.copyOf(published);
    }

    public int publishCount() {
        return published.size();
    }
}
