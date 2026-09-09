package dev.adellaghmari.boundarymesh.application.contract;

import java.util.List;
import java.util.Optional;

public interface ContractRegistry {

    Optional<RegisteredContract> find(String integrationId, String version);

    List<RegisteredContract> list(String integrationId);
}
