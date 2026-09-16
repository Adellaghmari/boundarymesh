package dev.adellaghmari.boundarymesh.application.delivery;

import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryResult;
import java.util.Objects;
import java.util.Optional;

/** Read-only view of the worker's durably recorded terminal disposition. */
public final class DeliveryStateQueryService {

    private final DeliveryIdempotencyStore store;

    public DeliveryStateQueryService(DeliveryIdempotencyStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    public Optional<DeliveryResult> find(String instructionId) {
        if (instructionId == null
                || !instructionId.matches(
                        "dlv-[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
            return Optional.empty();
        }
        return store.previous(instructionId);
    }
}
