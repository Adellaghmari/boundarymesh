package dev.adellaghmari.boundarymesh.domain.graph;

import java.util.Objects;

public record SystemNode(String id, String label, NodeKind kind, boolean synthetic) {

    public SystemNode {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(kind, "kind");
    }
}
