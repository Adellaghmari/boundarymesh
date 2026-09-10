package dev.adellaghmari.boundarymesh.domain.graph;

import java.util.Objects;

public record IntegrationReference(String integrationId, String source, String destination) {

    public IntegrationReference {
        Objects.requireNonNull(integrationId, "integrationId");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(destination, "destination");
    }
}
