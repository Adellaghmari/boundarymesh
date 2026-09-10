package dev.adellaghmari.boundarymesh.application.quarantine;

import dev.adellaghmari.boundarymesh.domain.ReasonCode;
import dev.adellaghmari.boundarymesh.domain.TransactionId;
import dev.adellaghmari.boundarymesh.domain.exception.InvalidInputException;
import dev.adellaghmari.boundarymesh.domain.quarantine.QuarantineId;
import dev.adellaghmari.boundarymesh.domain.quarantine.QuarantineItem;
import dev.adellaghmari.boundarymesh.domain.quarantine.QuarantineReason;
import dev.adellaghmari.boundarymesh.domain.quarantine.RecoveryDecision;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class QuarantineOrchestrator {

    private final QuarantineRepository repository;
    private final Clock clock;

    public QuarantineOrchestrator(QuarantineRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public QuarantineItem open(
            TransactionId transactionId,
            String integrationId,
            String sourceSystem,
            String destinationSystem,
            ReasonCode reasonCode,
            String contractReference,
            String policyReference,
            String credentialReference,
            String connectorState) {
        Instant now = clock.instant();
        QuarantineReason reason = QuarantineReason.from(reasonCode);
        QuarantineItem item = new QuarantineItem(
                QuarantineId.random(),
                transactionId,
                integrationId,
                sourceSystem,
                destinationSystem,
                reason,
                reasonCode,
                dev.adellaghmari.boundarymesh.domain.quarantine.QuarantineStatus.OPEN,
                0,
                QuarantineItem.DEFAULT_MAX_RECOVERY,
                QuarantineItem.eligibleReason(reason),
                contractReference,
                policyReference,
                credentialReference,
                connectorState,
                now,
                now,
                List.of());
        repository.save(item);
        return item;
    }

    public Optional<QuarantineItem> find(QuarantineId id) {
        return repository.find(id);
    }

    public List<QuarantineItem> list() {
        return repository.list();
    }

    public QuarantineItem beginReprocess(QuarantineId id) {
        QuarantineItem item = repository
                .find(id)
                .orElseThrow(() -> new InvalidInputException("QUARANTINE_NOT_FOUND", "quarantine item was not found"));
        if (item.recoveryDecision() != RecoveryDecision.ELIGIBLE) {
            throw new InvalidInputException(
                    "QUARANTINE_REPROCESS_DENIED", "quarantine item is not eligible to reprocess");
        }
        QuarantineItem next = item.beginReprocess(clock.instant());
        repository.save(next);
        return next;
    }

    public QuarantineItem resolve(QuarantineId id) {
        QuarantineItem item = repository
                .find(id)
                .orElseThrow(() -> new InvalidInputException("QUARANTINE_NOT_FOUND", "quarantine item was not found"));
        QuarantineItem next = item.markResolved(clock.instant());
        repository.save(next);
        return next;
    }

    public QuarantineItem remain(QuarantineId id, String note) {
        QuarantineItem item = repository
                .find(id)
                .orElseThrow(() -> new InvalidInputException("QUARANTINE_NOT_FOUND", "quarantine item was not found"));
        QuarantineItem next = item.markStillHeld(clock.instant(), note);
        repository.save(next);
        return next;
    }
}
