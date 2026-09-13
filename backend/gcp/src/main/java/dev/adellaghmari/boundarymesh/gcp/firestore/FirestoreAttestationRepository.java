package dev.adellaghmari.boundarymesh.gcp.firestore;

import dev.adellaghmari.boundarymesh.application.attestation.AttestationRepository;
import dev.adellaghmari.boundarymesh.domain.attestation.AttestationId;
import dev.adellaghmari.boundarymesh.domain.attestation.ExchangeAttestation;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Persists public attestation evidence only. Never stores private signing material. */
public final class FirestoreAttestationRepository implements AttestationRepository {

    private final FirestoreFacade firestore;

    public FirestoreAttestationRepository(FirestoreFacade firestore) {
        this.firestore = Objects.requireNonNull(firestore, "firestore");
    }

    @Override
    public void save(ExchangeAttestation attestation) {
        firestore.set(
                FirestoreCollections.attestation(attestation.id().value()), FirestoreCodecs.attestation(attestation));
    }

    @Override
    public Optional<ExchangeAttestation> find(AttestationId id) {
        return firestore.get(FirestoreCollections.attestation(id.value())).map(FirestoreCodecs::attestationFrom);
    }

    @Override
    public List<ExchangeAttestation> list(int limit) {
        List<ExchangeAttestation> items = new ArrayList<>();
        for (var row : firestore.list(FirestoreCollections.ATTESTATIONS, "saved_at", false, 0)) {
            items.add(FirestoreCodecs.attestationFrom(row));
        }
        if (items.size() <= limit) {
            return List.copyOf(items);
        }
        return List.copyOf(items.subList(items.size() - limit, items.size()));
    }
}
