package dev.adellaghmari.boundarymesh.application.graph;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Latest observed crossing per destination. Derived from the live pipeline, not Vue fixtures. */
public interface ObservedCrossingStore {

    record Observation(
            String integrationId,
            String source,
            String destination,
            String outcome,
            String deliveryResult,
            String auditChainId,
            String attestationId,
            boolean quarantined,
            Instant occurredAt) {}

    void record(Observation observation);

    Optional<Observation> find(String integrationId);

    Optional<Observation> findByDestination(String destination);

    Map<String, Observation> all();

    static String destinationKey(String destination) {
        return destination == null ? "" : destination.trim().toUpperCase(Locale.ROOT);
    }
}
