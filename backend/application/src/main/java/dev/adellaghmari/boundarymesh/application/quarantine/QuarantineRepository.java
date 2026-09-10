package dev.adellaghmari.boundarymesh.application.quarantine;

import dev.adellaghmari.boundarymesh.domain.quarantine.QuarantineId;
import dev.adellaghmari.boundarymesh.domain.quarantine.QuarantineItem;
import java.util.List;
import java.util.Optional;

public interface QuarantineRepository {

    void save(QuarantineItem item);

    Optional<QuarantineItem> find(QuarantineId id);

    List<QuarantineItem> list();
}
