package dev.adellaghmari.boundarymesh.domain.contract;

import java.util.Objects;

public record ContractReference(ContractId id, ContractVersion version, ContractType type, ContractStatus status) {

    public ContractReference {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(status, "status");
    }
}
