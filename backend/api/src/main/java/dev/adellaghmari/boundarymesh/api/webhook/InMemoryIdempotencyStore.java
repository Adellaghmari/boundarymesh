package dev.adellaghmari.boundarymesh.api.webhook;

import dev.adellaghmari.boundarymesh.application.webhook.IdempotencyStore;
import dev.adellaghmari.boundarymesh.domain.BoundaryDecision;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "test"})
public class InMemoryIdempotencyStore implements IdempotencyStore {

    private final ConcurrentHashMap<String, BoundaryDecision> events = new ConcurrentHashMap<>();

    @Override
    public Optional<BoundaryDecision> find(String eventKey) {
        return Optional.ofNullable(events.get(eventKey));
    }

    @Override
    public void putIfAbsent(String eventKey, BoundaryDecision decision) {
        events.putIfAbsent(eventKey, decision);
    }
}
