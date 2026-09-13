package dev.adellaghmari.boundarymesh.gcp.firestore;

import dev.adellaghmari.boundarymesh.application.quarantine.QuarantineRepository;
import dev.adellaghmari.boundarymesh.domain.quarantine.QuarantineId;
import dev.adellaghmari.boundarymesh.domain.quarantine.QuarantineItem;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class FirestoreQuarantineRepository implements QuarantineRepository {

    private final FirestoreFacade firestore;

    public FirestoreQuarantineRepository(FirestoreFacade firestore) {
        this.firestore = Objects.requireNonNull(firestore, "firestore");
    }

    @Override
    public void save(QuarantineItem item) {
        firestore.set(FirestoreCollections.quarantine(item.id().value()), FirestoreCodecs.quarantine(item));
    }

    @Override
    public Optional<QuarantineItem> find(QuarantineId id) {
        return firestore.get(FirestoreCollections.quarantine(id.value())).map(FirestoreCodecs::quarantineItem);
    }

    @Override
    public List<QuarantineItem> list() {
        return firestore.list(FirestoreCollections.QUARANTINE, "created_at", true, 0).stream()
                .map(FirestoreCodecs::quarantineItem)
                .toList();
    }
}
