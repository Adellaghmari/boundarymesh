package dev.adellaghmari.boundarymesh.application.attestation;

import dev.adellaghmari.boundarymesh.domain.attestation.AttestationId;
import dev.adellaghmari.boundarymesh.domain.attestation.ExchangeAttestation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory adapter. Does not store private keys. Firestore remains PLANNED. */
public final class InMemoryAttestationRepository implements AttestationRepository {

    private final Map<AttestationId, ExchangeAttestation> byId = new ConcurrentHashMap<>();
    private final List<AttestationId> order = new ArrayList<>();

    @Override
    public synchronized void save(ExchangeAttestation attestation) {
        if (!byId.containsKey(attestation.id())) {
            order.add(attestation.id());
        }
        byId.put(attestation.id(), attestation);
    }

    @Override
    public Optional<ExchangeAttestation> find(AttestationId id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public synchronized List<ExchangeAttestation> list(int limit) {
        int from = Math.max(0, order.size() - limit);
        return order.subList(from, order.size()).stream()
                .map(byId::get)
                .filter(item -> item != null)
                .toList();
    }

    public synchronized void clear() {
        byId.clear();
        order.clear();
    }
}
