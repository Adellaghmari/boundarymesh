package dev.adellaghmari.boundarymesh.domain.quarantine;

import dev.adellaghmari.boundarymesh.domain.ReasonCode;
import dev.adellaghmari.boundarymesh.domain.TransactionId;
import dev.adellaghmari.boundarymesh.domain.exception.InvalidInputException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Recoverable hold. Does not store secrets. Payload is omitted from the public record; local
 * reprocess uses a bounded internal store.
 */
public record QuarantineItem(
        QuarantineId id,
        TransactionId transactionId,
        String integrationId,
        String sourceSystem,
        String destinationSystem,
        QuarantineReason reason,
        ReasonCode reasonCode,
        QuarantineStatus status,
        int recoveryAttempts,
        int maxRecoveryAttempts,
        boolean eligible,
        String contractReference,
        String policyReference,
        String credentialReference,
        String connectorState,
        Instant createdAt,
        Instant updatedAt,
        List<RecoveryAttempt> attempts) {

    public static final int DEFAULT_MAX_RECOVERY = 3;

    public QuarantineItem {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(transactionId, "transactionId");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(reasonCode, "reasonCode");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        attempts = attempts == null ? List.of() : List.copyOf(attempts);
        if (maxRecoveryAttempts < 1) {
            throw new InvalidInputException("QUARANTINE_CAP_INVALID", "max recovery attempts must be at least 1");
        }
    }

    public RecoveryDecision recoveryDecision() {
        if (status == QuarantineStatus.TERMINAL || status == QuarantineStatus.RESOLVED) {
            return RecoveryDecision.EXHAUSTED;
        }
        if (status == QuarantineStatus.REPROCESSING) {
            return RecoveryDecision.INELIGIBLE;
        }
        if (!eligible) {
            return RecoveryDecision.INELIGIBLE;
        }
        if (recoveryAttempts >= maxRecoveryAttempts) {
            return RecoveryDecision.EXHAUSTED;
        }
        return RecoveryDecision.ELIGIBLE;
    }

    public QuarantineItem beginReprocess(Instant now) {
        if (recoveryDecision() != RecoveryDecision.ELIGIBLE) {
            throw new InvalidInputException("QUARANTINE_REPROCESS_DENIED", "quarantine item is not eligible");
        }
        if (status != QuarantineStatus.OPEN) {
            throw new InvalidInputException("QUARANTINE_TRANSITION_INVALID", "only OPEN items may begin reprocess");
        }
        return withStatus(QuarantineStatus.REPROCESSING, now, "Reprocess authorized.");
    }

    public QuarantineItem markResolved(Instant now) {
        if (status != QuarantineStatus.REPROCESSING && status != QuarantineStatus.OPEN) {
            throw new InvalidInputException("QUARANTINE_TRANSITION_INVALID", "cannot resolve from " + status);
        }
        List<RecoveryAttempt> next = append(now, QuarantineStatus.RESOLVED, "Current conditions allowed delivery.");
        return new QuarantineItem(
                id,
                transactionId,
                integrationId,
                sourceSystem,
                destinationSystem,
                reason,
                reasonCode,
                QuarantineStatus.RESOLVED,
                recoveryAttempts + 1,
                maxRecoveryAttempts,
                false,
                contractReference,
                policyReference,
                credentialReference,
                connectorState,
                createdAt,
                now,
                next);
    }

    public QuarantineItem markStillHeld(Instant now, String note) {
        if (status != QuarantineStatus.REPROCESSING) {
            throw new InvalidInputException(
                    "QUARANTINE_TRANSITION_INVALID", "only REPROCESSING items may fail recovery");
        }
        int used = recoveryAttempts + 1;
        QuarantineStatus nextStatus = used >= maxRecoveryAttempts ? QuarantineStatus.TERMINAL : QuarantineStatus.OPEN;
        List<RecoveryAttempt> next = append(now, nextStatus, note);
        return new QuarantineItem(
                id,
                transactionId,
                integrationId,
                sourceSystem,
                destinationSystem,
                reason,
                reasonCode,
                nextStatus,
                used,
                maxRecoveryAttempts,
                nextStatus == QuarantineStatus.OPEN && eligible,
                contractReference,
                policyReference,
                credentialReference,
                connectorState,
                createdAt,
                now,
                next);
    }

    public QuarantineItem withConnectorState(String nextState, Instant now) {
        return new QuarantineItem(
                id,
                transactionId,
                integrationId,
                sourceSystem,
                destinationSystem,
                reason,
                reasonCode,
                status,
                recoveryAttempts,
                maxRecoveryAttempts,
                eligible,
                contractReference,
                policyReference,
                credentialReference,
                nextState,
                createdAt,
                now,
                attempts);
    }

    private QuarantineItem withStatus(QuarantineStatus next, Instant now, String note) {
        List<RecoveryAttempt> history = status == QuarantineStatus.REPROCESSING ? append(now, next, note) : attempts;
        return new QuarantineItem(
                id,
                transactionId,
                integrationId,
                sourceSystem,
                destinationSystem,
                reason,
                reasonCode,
                next,
                recoveryAttempts,
                maxRecoveryAttempts,
                eligible,
                contractReference,
                policyReference,
                credentialReference,
                connectorState,
                createdAt,
                now,
                history);
    }

    private List<RecoveryAttempt> append(Instant now, QuarantineStatus resulting, String note) {
        List<RecoveryAttempt> next = new ArrayList<>(attempts);
        next.add(new RecoveryAttempt(recoveryAttempts + 1, now, resulting, note));
        return List.copyOf(next);
    }

    public static boolean eligibleReason(QuarantineReason reason) {
        return reason != null;
    }
}
