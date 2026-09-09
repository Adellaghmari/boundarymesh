package dev.adellaghmari.boundarymesh.application.egress;

import dev.adellaghmari.boundarymesh.domain.egress.EgressPolicy;
import java.util.Optional;

public interface EgressPolicyRepository {

    Optional<EgressPolicy> findByDestination(String destination);
}
