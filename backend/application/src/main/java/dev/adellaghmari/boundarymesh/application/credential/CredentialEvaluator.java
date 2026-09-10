package dev.adellaghmari.boundarymesh.application.credential;

import dev.adellaghmari.boundarymesh.domain.ControlKind;
import dev.adellaghmari.boundarymesh.domain.ControlResult;
import dev.adellaghmari.boundarymesh.domain.ReasonCode;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialRecord;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialStatus;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class CredentialEvaluator {

    private final CredentialCatalog catalog;

    public CredentialEvaluator(CredentialCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    public ControlResult evaluate(String destination, String scenario) {
        String profile = scenario == null ? "active" : scenario.trim().toLowerCase(Locale.ROOT);
        if ("unavailable".equals(profile) || "provider-unavailable".equals(profile)) {
            return ControlResult.unavailable(
                    ControlKind.CREDENTIAL_RESOLVABILITY,
                    ReasonCode.CREDENTIAL_PROVIDER_UNAVAILABLE,
                    "Secret provider is temporarily unavailable.");
        }
        if ("missing".equals(profile)) {
            return ControlResult.fail(
                    ControlKind.CREDENTIAL_RESOLVABILITY,
                    ReasonCode.CREDENTIAL_MISSING,
                    "No credential reference is configured for " + destination + ".");
        }
        Optional<CredentialRecord> record = catalog.activeForDestination(destination);
        if ("revoked".equals(profile) || "retired".equals(profile)) {
            return ControlResult.fail(
                    ControlKind.CREDENTIAL_RESOLVABILITY,
                    ReasonCode.CREDENTIAL_REVOKED,
                    "Credential version is retired or revoked; no silent fallback.");
        }
        if (record.isEmpty()) {
            return ControlResult.fail(
                    ControlKind.CREDENTIAL_RESOLVABILITY,
                    ReasonCode.CREDENTIAL_MISSING,
                    "No active credential reference for destination " + destination + ".");
        }
        CredentialRecord found = record.get();
        if (found.reference().status() != CredentialStatus.ACTIVE) {
            return ControlResult.fail(
                    ControlKind.CREDENTIAL_RESOLVABILITY,
                    ReasonCode.CREDENTIAL_REVOKED,
                    "Active lookup returned a non-active version.");
        }
        if ("rotated".equals(profile) || "active".equals(profile) || profile.isBlank()) {
            return new ControlResult(
                    ControlKind.CREDENTIAL_RESOLVABILITY,
                    dev.adellaghmari.boundarymesh.domain.ControlStatus.PASS,
                    java.util.List.of(),
                    "Credential " + found.reference().display() + " is active and resolvable by reference.",
                    found.reference().display());
        }
        return ControlResult.fail(
                ControlKind.CREDENTIAL_RESOLVABILITY,
                ReasonCode.CREDENTIAL_UNAVAILABLE,
                "Credential profile is not recognized.");
    }

    public Optional<CredentialRecord> reference(String destination) {
        return catalog.activeForDestination(destination);
    }
}
