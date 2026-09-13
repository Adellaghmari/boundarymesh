package dev.adellaghmari.boundarymesh.application.graph;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** In-process observation store. Does not survive restart. Firestore remains a separate adapter. */
public final class InMemoryObservedCrossingStore implements ObservedCrossingStore {

    private final Map<String, Observation> latest = new ConcurrentHashMap<>();

    @Override
    public void record(Observation observation) {
        latest.put(ObservedCrossingStore.destinationKey(observation.destination()), observation);
    }

    @Override
    public Optional<Observation> find(String integrationId) {
        return latest.values().stream()
                .filter(item -> integrationId.equals(item.integrationId()))
                .findFirst();
    }

    @Override
    public Optional<Observation> findByDestination(String destination) {
        return Optional.ofNullable(latest.get(ObservedCrossingStore.destinationKey(destination)));
    }

    @Override
    public Map<String, Observation> all() {
        return Map.copyOf(latest);
    }
}
