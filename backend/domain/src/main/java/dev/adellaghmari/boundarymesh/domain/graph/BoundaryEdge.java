package dev.adellaghmari.boundarymesh.domain.graph;

import java.util.Objects;

public record BoundaryEdge(
        String id,
        IntegrationReference integration,
        String identityMechanism,
        String contractReference,
        String egressPolicy,
        String credentialReference,
        String consentState,
        String connectorRoute,
        BoundaryEdgeStatus status,
        boolean configured,
        boolean observed,
        String recentOutcome,
        String recentDelivery,
        int quarantineCount,
        String auditChainId,
        String attestationId) {

    public BoundaryEdge {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(integration, "integration");
        Objects.requireNonNull(status, "status");
    }

    public String source() {
        return integration.source();
    }

    public String destination() {
        return integration.destination();
    }
}
