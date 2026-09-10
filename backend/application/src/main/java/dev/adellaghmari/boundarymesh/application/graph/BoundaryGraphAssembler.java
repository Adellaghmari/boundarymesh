package dev.adellaghmari.boundarymesh.application.graph;

import dev.adellaghmari.boundarymesh.application.quarantine.QuarantineRepository;
import dev.adellaghmari.boundarymesh.domain.graph.BoundaryEdge;
import dev.adellaghmari.boundarymesh.domain.graph.BoundaryEdgeStatus;
import dev.adellaghmari.boundarymesh.domain.graph.BoundaryGraph;
import dev.adellaghmari.boundarymesh.domain.graph.IntegrationReference;
import dev.adellaghmari.boundarymesh.domain.graph.NodeKind;
import dev.adellaghmari.boundarymesh.domain.graph.SystemNode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class BoundaryGraphAssembler {

    private final ObservedCrossingStore observations;
    private final QuarantineRepository quarantine;

    public BoundaryGraphAssembler(ObservedCrossingStore observations, QuarantineRepository quarantine) {
        this.observations = Objects.requireNonNull(observations);
        this.quarantine = Objects.requireNonNull(quarantine);
    }

    public BoundaryGraph assemble() {
        List<SystemNode> nodes = List.of(
                new SystemNode("QUAYFORGE", "QuayForge Orders", NodeKind.SYSTEM, true),
                new SystemNode("HELIX365", "Helix 365", NodeKind.SYSTEM, true),
                new SystemNode("BOUNDARYMESH", "BoundaryMesh", NodeKind.CONTROL_PLANE, true),
                new SystemNode("COVECRM", "CoveCRM", NodeKind.SYSTEM, true),
                new SystemNode("LUMENWAY", "Lumenway Fulfillment", NodeKind.SYSTEM, true),
                new SystemNode("RIVERTIDE", "Rivertide Settlements", NodeKind.SYSTEM, true));
        List<BoundaryEdge> edges = List.of(
                edge(
                        "quayforge-orders",
                        "QUAYFORGE",
                        "COVECRM",
                        "JWT RS256",
                        "quayforge-orders:v1",
                        "covecrm",
                        "covecrm-api:2",
                        "GRANTED",
                        "sink.covecrm.activity"),
                edge(
                        "quayforge-fulfillment",
                        "QUAYFORGE",
                        "LUMENWAY",
                        "JWT RS256",
                        "quayforge-orders:v1",
                        "lumenway",
                        "lumenway-api:1",
                        "GRANTED",
                        "sink.lumenway.fulfillment"),
                edge(
                        "quayforge-settlements",
                        "QUAYFORGE",
                        "RIVERTIDE",
                        "HMAC-SHA256",
                        "quayforge-orders:v1",
                        "rivertide",
                        "rivertide-api:1",
                        "GRANTED",
                        "sink.rivertide.instruction"),
                edge(
                        "helix365-calendar",
                        "HELIX365",
                        "COVECRM",
                        "Entra (PLANNED)",
                        "helix365-calendar:v1",
                        "covecrm",
                        "none",
                        "UNCONFIGURED",
                        null));
        return new BoundaryGraph(
                "LOCAL / SYNTHETIC SESSION DATA. Graph is derived from configured integrations plus observed crossings. Not Firestore.",
                nodes,
                edges);
    }

    private BoundaryEdge edge(
            String integrationId,
            String source,
            String destination,
            String identity,
            String contract,
            String policy,
            String credential,
            String consent,
            String route) {
        boolean helix = "helix365-calendar".equals(integrationId);
        var observed = helix
                ? Optional.<ObservedCrossingStore.Observation>empty()
                : observations.findByDestination(destination);
        int quarantineCount = helix
                ? 0
                : (int) quarantine.list().stream()
                        .filter(item -> destination.equalsIgnoreCase(item.destinationSystem()))
                        .filter(item -> item.status().name().equals("OPEN")
                                || item.status().name().equals("REPROCESSING"))
                        .count();
        BoundaryEdgeStatus status;
        String outcome = null;
        String delivery = null;
        String attestationId = null;
        boolean seen = observed.isPresent();
        if (helix) {
            status = BoundaryEdgeStatus.NO_RECENT_ACTIVITY;
        } else if (observed.isPresent() && "REJECT".equals(observed.get().outcome())) {
            status = BoundaryEdgeStatus.BLOCKED;
            outcome = observed.get().outcome();
            delivery = observed.get().deliveryResult();
            attestationId = observed.get().attestationId();
        } else if (observed.isPresent() && (observed.get().quarantined() || quarantineCount > 0)) {
            status = BoundaryEdgeStatus.QUARANTINED;
            outcome = observed.get().outcome();
            delivery = observed.get().deliveryResult();
            attestationId = observed.get().attestationId();
        } else if (observed.isPresent()) {
            status = BoundaryEdgeStatus.ACTIVE;
            outcome = observed.get().outcome();
            delivery = observed.get().deliveryResult();
            attestationId = observed.get().attestationId();
        } else {
            status = BoundaryEdgeStatus.CONFIGURED;
        }
        String auditChain = observed.map(ObservedCrossingStore.Observation::auditChainId)
                .filter(id -> id != null && !id.isBlank())
                .orElse(integrationId);
        String attestationRef =
                observed.map(ObservedCrossingStore.Observation::attestationId).orElse(null);
        return new BoundaryEdge(
                integrationId,
                new IntegrationReference(integrationId, source, destination),
                identity,
                contract,
                policy,
                credential,
                consent,
                route,
                status,
                true,
                seen,
                outcome,
                delivery,
                quarantineCount,
                auditChain,
                attestationRef == null ? attestationId : attestationRef);
    }
}
