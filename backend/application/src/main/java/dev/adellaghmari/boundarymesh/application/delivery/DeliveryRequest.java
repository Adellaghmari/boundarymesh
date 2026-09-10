package dev.adellaghmari.boundarymesh.application.delivery;

import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryInstruction;
import java.util.Objects;

public record DeliveryRequest(DeliveryInstruction instruction, ProviderScenario providerScenario) {

    public DeliveryRequest {
        Objects.requireNonNull(instruction, "instruction");
        providerScenario = providerScenario == null ? ProviderScenario.SUCCESS : providerScenario;
    }
}
