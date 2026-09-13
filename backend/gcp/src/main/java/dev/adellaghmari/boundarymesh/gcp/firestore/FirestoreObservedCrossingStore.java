package dev.adellaghmari.boundarymesh.gcp.firestore;

import dev.adellaghmari.boundarymesh.application.graph.ObservedCrossingStore;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Derived operational evidence. Latest observation per destination. Not an independently mutable
 * graph source of truth.
 */
public final class FirestoreObservedCrossingStore implements ObservedCrossingStore {

    private final FirestoreFacade firestore;

    public FirestoreObservedCrossingStore(FirestoreFacade firestore) {
        this.firestore = Objects.requireNonNull(firestore, "firestore");
    }

    @Override
    public void record(Observation observation) {
        firestore.set(
                FirestoreCollections.observedCrossing(observation.destination()),
                FirestoreCodecs.observation(observation));
    }

    @Override
    public Optional<Observation> find(String integrationId) {
        return firestore.listWhere(FirestoreCollections.OBSERVED_CROSSINGS, "integration_id", integrationId, 1).stream()
                .findFirst()
                .map(FirestoreCodecs::observation);
    }

    @Override
    public Optional<Observation> findByDestination(String destination) {
        return firestore.get(FirestoreCollections.observedCrossing(destination)).map(FirestoreCodecs::observation);
    }

    @Override
    public Map<String, Observation> all() {
        Map<String, Observation> latest = new LinkedHashMap<>();
        for (var row : firestore.list(FirestoreCollections.OBSERVED_CROSSINGS, null, false, 0)) {
            Observation observation = FirestoreCodecs.observation(row);
            latest.put(ObservedCrossingStore.destinationKey(observation.destination()), observation);
        }
        return Map.copyOf(latest);
    }
}
