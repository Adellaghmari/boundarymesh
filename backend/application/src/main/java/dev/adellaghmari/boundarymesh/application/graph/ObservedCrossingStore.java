package dev.adellaghmari.boundarymesh.application.graph;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Latest observed crossing per destination. Derived from live pipeline, not Vue fixtures. */
public final class ObservedCrossingStore {

    public record Observation(
            String integrationId,
            String source,
            String destination,
            String outcome,
            String deliveryResult,
            String auditChainId,
            String attestationId,
            boolean quarantined,
            Instant occurredAt) {}

    private final Map<String, Observation> latest = new ConcurrentHashMap<>();

    public void record(Observation observation) {
        latest.put(key(observation.destination()), observation);
    }

    public Optional<Observation> find(String integrationId) {
        return latest.values().stream()
                .filter(item -> integrationId.equals(item.integrationId()))
                .findFirst();
    }

    public Optional<Observation> findByDestination(String destination) {
        return Optional.ofNullable(latest.get(key(destination)));
    }

    private static String key(String destination) {
        return destination == null ? "" : destination.trim().toUpperCase(Locale.ROOT);
    }

    public Map<String, Observation> all() {
        return Map.copyOf(latest);
    }
}
