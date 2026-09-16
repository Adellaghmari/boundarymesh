package dev.adellaghmari.boundarymesh.webedge;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** UID allowlist is server-side operational configuration; empty always denies. */
final class OperatorPolicy {
    private final Set<String> allowedUids;

    OperatorPolicy(String configuredUids) {
        allowedUids = Arrays.stream(configuredUids.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    boolean allows(AuthGateway.Principal principal) {
        return principal != null && principal.emailVerified() && allowedUids.contains(principal.uid());
    }
}
