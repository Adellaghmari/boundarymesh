package dev.adellaghmari.boundarymesh.domain.graph;

import java.util.List;
import java.util.Objects;

public record BoundaryGraph(String banner, List<SystemNode> nodes, List<BoundaryEdge> edges) {

    public BoundaryGraph {
        Objects.requireNonNull(banner, "banner");
        Objects.requireNonNull(nodes, "nodes");
        Objects.requireNonNull(edges, "edges");
        nodes = List.copyOf(nodes);
        edges = List.copyOf(edges);
    }
}
