package dev.adellaghmari.boundarymesh.domain.egress;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EgressPolicy(
        EgressPolicyId id,
        EgressPolicyVersion version,
        String destination,
        boolean implicitDeny,
        List<FieldRule> rules) {

    public EgressPolicy {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(destination, "destination");
        rules = List.copyOf(rules);
        if (destination.isBlank()) {
            throw new IllegalArgumentException("destination is required");
        }
        Map<String, FieldAction> seen = new LinkedHashMap<>();
        for (FieldRule rule : rules) {
            FieldAction previous = seen.put(rule.path().pointer(), rule.action());
            if (previous != null && previous != rule.action()) {
                throw new IllegalArgumentException("conflicting rules for " + rule.path());
            }
        }
    }
}
