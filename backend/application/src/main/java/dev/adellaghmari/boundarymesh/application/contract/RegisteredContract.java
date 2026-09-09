package dev.adellaghmari.boundarymesh.application.contract;

import dev.adellaghmari.boundarymesh.domain.contract.ContractReference;
import java.util.Objects;

public record RegisteredContract(String integrationId, ContractReference reference, String schemaJson) {

    public RegisteredContract {
        Objects.requireNonNull(integrationId, "integrationId");
        Objects.requireNonNull(reference, "reference");
        Objects.requireNonNull(schemaJson, "schemaJson");
    }
}
