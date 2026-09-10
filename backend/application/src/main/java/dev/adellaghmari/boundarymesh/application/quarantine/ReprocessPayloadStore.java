package dev.adellaghmari.boundarymesh.application.quarantine;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bounded local store for synthetic reprocess payloads. Not a public API model. Future cloud
 * storage remains PLANNED.
 */
public final class ReprocessPayloadStore {

    private final ConcurrentHashMap<String, StoredCrossing> payloads = new ConcurrentHashMap<>();

    public void put(String quarantineId, StoredCrossing crossing) {
        payloads.put(quarantineId, crossing);
    }

    public Optional<StoredCrossing> find(String quarantineId) {
        return Optional.ofNullable(payloads.get(quarantineId));
    }

    public record StoredCrossing(
            String scenario,
            String destination,
            String credentialScenario,
            String consentScenario,
            String connectorScenario,
            String providerScenario,
            String payloadJson,
            String contractVersion) {}
}
