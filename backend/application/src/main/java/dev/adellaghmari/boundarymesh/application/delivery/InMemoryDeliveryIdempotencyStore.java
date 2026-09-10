package dev.adellaghmari.boundarymesh.application.delivery;

import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryResult;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryDeliveryIdempotencyStore implements DeliveryIdempotencyStore {

    private final ConcurrentHashMap<String, DeliveryResult> results = new ConcurrentHashMap<>();

    @Override
    public Optional<DeliveryResult> previous(String deliveryInstructionId) {
        return Optional.ofNullable(results.get(deliveryInstructionId));
    }

    @Override
    public void remember(String deliveryInstructionId, DeliveryResult result) {
        results.putIfAbsent(deliveryInstructionId, result);
    }

    @Override
    public void clear() {
        results.clear();
    }
}
