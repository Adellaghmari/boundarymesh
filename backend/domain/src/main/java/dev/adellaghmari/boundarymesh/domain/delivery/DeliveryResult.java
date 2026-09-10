package dev.adellaghmari.boundarymesh.domain.delivery;

import dev.adellaghmari.boundarymesh.domain.ReasonCode;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialReference;
import java.time.Duration;
import java.util.Objects;

/** Safe connector delivery evidence. Never includes secrets or raw provider bodies. */
public record DeliveryResult(
        DeliveryResultKind kind,
        int attemptCount,
        ProviderFailureCategory providerCategory,
        ReasonCode reason,
        String safeExplanation,
        Duration duration,
        String connectorId,
        String routeId,
        String circuitState,
        Integer httpStatus,
        CredentialReference credentialReference,
        String consentId,
        boolean providerCalled) {

    public DeliveryResult {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(providerCategory, "providerCategory");
        Objects.requireNonNull(safeExplanation, "safeExplanation");
        Objects.requireNonNull(duration, "duration");
        if (attemptCount < 0) {
            throw new IllegalArgumentException("attemptCount");
        }
    }

    public static DeliveryResult notAttempted(String explanation) {
        return new DeliveryResult(
                DeliveryResultKind.NOT_ATTEMPTED,
                0,
                ProviderFailureCategory.NONE,
                null,
                explanation,
                Duration.ZERO,
                null,
                null,
                null,
                null,
                null,
                null,
                false);
    }
}
