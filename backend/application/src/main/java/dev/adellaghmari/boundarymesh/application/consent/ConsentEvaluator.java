package dev.adellaghmari.boundarymesh.application.consent;

import dev.adellaghmari.boundarymesh.domain.ReasonCode;
import dev.adellaghmari.boundarymesh.domain.consent.ConnectorOperation;
import dev.adellaghmari.boundarymesh.domain.consent.ConsentGrant;
import dev.adellaghmari.boundarymesh.domain.consent.ConsentStatus;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class ConsentEvaluator {

    private final ConsentCatalog catalog;

    public ConsentEvaluator(ConsentCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    public ConsentEvaluation evaluate(String destination, ConnectorOperation operation, String scenario) {
        String profile = scenario == null ? "valid" : scenario.trim().toLowerCase(Locale.ROOT);
        if ("missing".equals(profile)) {
            return new ConsentEvaluation(
                    ConsentStatus.MISSING,
                    ReasonCode.CONSENT_MISSING,
                    "No consent grant for this connector operation.",
                    null);
        }
        if ("revoked".equals(profile)) {
            return new ConsentEvaluation(
                    ConsentStatus.REVOKED,
                    ReasonCode.CONSENT_REVOKED,
                    "Consent grant is revoked; delivery is prohibited.",
                    catalog.find(destination, operation).orElse(null));
        }
        Optional<ConsentGrant> grant = catalog.find(destination, operation);
        if (grant.isEmpty() || grant.get().status() == ConsentStatus.MISSING) {
            return new ConsentEvaluation(
                    ConsentStatus.MISSING,
                    ReasonCode.CONSENT_MISSING,
                    "No consent grant for this connector operation.",
                    null);
        }
        if (grant.get().status() == ConsentStatus.REVOKED) {
            return new ConsentEvaluation(
                    ConsentStatus.REVOKED,
                    ReasonCode.CONSENT_REVOKED,
                    "Consent grant is revoked; delivery is prohibited.",
                    grant.get());
        }
        return new ConsentEvaluation(
                ConsentStatus.GRANTED,
                null,
                "Connector identity is permitted for this destination operation.",
                grant.get());
    }
}
