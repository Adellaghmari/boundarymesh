package dev.adellaghmari.boundarymesh.application.quarantine;

import dev.adellaghmari.boundarymesh.domain.quarantine.QuarantineId;
import dev.adellaghmari.boundarymesh.domain.quarantine.QuarantineItem;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryQuarantineRepository implements QuarantineRepository {

    private final ConcurrentHashMap<String, QuarantineItem> items = new ConcurrentHashMap<>();

    @Override
    public void save(QuarantineItem item) {
        items.put(item.id().value(), item);
    }

    @Override
    public Optional<QuarantineItem> find(QuarantineId id) {
        return Optional.ofNullable(items.get(id.value()));
    }

    @Override
    public List<QuarantineItem> list() {
        return items.values().stream()
                .sorted(Comparator.comparing((QuarantineItem item) -> item.createdAt())
                        .reversed())
                .toList();
    }
}
