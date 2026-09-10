package dev.adellaghmari.boundarymesh.domain.delivery;

import dev.adellaghmari.boundarymesh.domain.BoundaryOutcome;
import dev.adellaghmari.boundarymesh.domain.TransactionId;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialReference;
import dev.adellaghmari.boundarymesh.domain.exception.InvariantViolationException;
import java.time.Instant;
import java.util.Objects;

/**
 * Normalized outbound instruction produced only after ALLOW or ALLOW_WITH_TRANSFORMATION. Never
 * contains JWT, HMAC, or connector secret material.
 */
public record DeliveryInstruction(
        DeliveryInstructionId eventId,
        TransactionId transactionId,
        String integrationId,
        String sourceSystem,
        String destinationSystem,
        String contractReference,
        String egressPolicyReference,
        BoundaryOutcome boundaryOutcome,
        String payloadJson,
        String correlationId,
        Instant createdAt,
        CredentialReference credentialReference,
        String consentId) {

    public DeliveryInstruction {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(transactionId, "transactionId");
        Objects.requireNonNull(integrationId, "integrationId");
        Objects.requireNonNull(sourceSystem, "sourceSystem");
        Objects.requireNonNull(destinationSystem, "destinationSystem");
        Objects.requireNonNull(boundaryOutcome, "boundaryOutcome");
        Objects.requireNonNull(payloadJson, "payloadJson");
        Objects.requireNonNull(correlationId, "correlationId");
        Objects.requireNonNull(createdAt, "createdAt");
        if (boundaryOutcome != BoundaryOutcome.ALLOW && boundaryOutcome != BoundaryOutcome.ALLOW_WITH_TRANSFORMATION) {
            throw new InvariantViolationException(
                    "DELIVERY_OUTCOME_INVALID", "only ALLOW or ALLOW_WITH_TRANSFORMATION may produce a delivery");
        }
    }
}
