package dev.adellaghmari.boundarymesh.application.delivery;

import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryResult;
import java.util.Optional;

public interface DeliveryIdempotencyStore {

    Optional<DeliveryResult> previous(String deliveryInstructionId);

    void remember(String deliveryInstructionId, DeliveryResult result);

    void clear();
}
