package dev.adellaghmari.boundarymesh.domain;

import dev.adellaghmari.boundarymesh.domain.exception.InvariantViolationException;
import java.time.Instant;
import java.util.Objects;

/**
 * One attempt to move data or an action across a system boundary.
 */
public record BoundaryTransaction(
        TransactionId id,
        IntegrationId integrationId,
        SourceSystem sourceSystem,
        DestinationSystem destinationSystem,
        TransactionState state,
        Instant receivedAt) {

    public BoundaryTransaction {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(integrationId, "integrationId");
        Objects.requireNonNull(sourceSystem, "sourceSystem");
        Objects.requireNonNull(destinationSystem, "destinationSystem");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(receivedAt, "receivedAt");
    }

    public static BoundaryTransaction received(
            TransactionId id,
            IntegrationId integrationId,
            SourceSystem sourceSystem,
            DestinationSystem destinationSystem,
            Instant receivedAt) {
        return new BoundaryTransaction(
                id, integrationId, sourceSystem, destinationSystem, TransactionState.RECEIVED, receivedAt);
    }

    public BoundaryTransaction withState(TransactionState next) {
        Objects.requireNonNull(next, "next");
        if (!isAllowedTransition(state, next)) {
            throw new InvariantViolationException(
                    "TRANSACTION_STATE_INVALID", "cannot transition transaction from " + state + " to " + next);
        }
        return new BoundaryTransaction(id, integrationId, sourceSystem, destinationSystem, next, receivedAt);
    }

    public BoundaryTransaction apply(BoundaryOutcome outcome) {
        Objects.requireNonNull(outcome, "outcome");
        TransactionState evaluated = state == TransactionState.RECEIVED
                ? withState(TransactionState.EVALUATED).state()
                : state;
        BoundaryTransaction evaluatedTxn =
                new BoundaryTransaction(id, integrationId, sourceSystem, destinationSystem, evaluated, receivedAt);
        return switch (outcome) {
            case ALLOW, ALLOW_WITH_TRANSFORMATION -> evaluatedTxn.withState(TransactionState.ALLOWED);
            case QUARANTINE -> evaluatedTxn.withState(TransactionState.QUARANTINED);
            case REJECT -> evaluatedTxn.withState(TransactionState.REJECTED);
        };
    }

    static boolean isAllowedTransition(TransactionState from, TransactionState to) {
        return switch (from) {
            case RECEIVED -> to == TransactionState.EVALUATED;
            case EVALUATED ->
                to == TransactionState.ALLOWED || to == TransactionState.QUARANTINED || to == TransactionState.REJECTED;
            case ALLOWED, QUARANTINED, REJECTED -> false;
        };
    }
}
