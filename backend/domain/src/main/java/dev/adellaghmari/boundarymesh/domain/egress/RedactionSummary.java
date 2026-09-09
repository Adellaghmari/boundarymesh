package dev.adellaghmari.boundarymesh.domain.egress;

import java.util.List;

public record RedactionSummary(
        List<String> allowedPaths,
        List<String> redactedPaths,
        List<String> deniedPaths,
        List<String> implicitDenyPaths,
        int transformationCount) {

    public RedactionSummary {
        allowedPaths = List.copyOf(allowedPaths == null ? List.of() : allowedPaths);
        redactedPaths = List.copyOf(redactedPaths == null ? List.of() : redactedPaths);
        deniedPaths = List.copyOf(deniedPaths == null ? List.of() : deniedPaths);
        implicitDenyPaths = List.copyOf(implicitDenyPaths == null ? List.of() : implicitDenyPaths);
    }
}
