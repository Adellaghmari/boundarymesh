package dev.adellaghmari.boundarymesh.domain.quarantine;

import java.time.Instant;
import java.util.Objects;

public record RecoveryAttempt(int attemptNumber, Instant attemptedAt, QuarantineStatus resultingStatus, String note) {

    public RecoveryAttempt {
        if (attemptNumber < 1) {
            throw new IllegalArgumentException("attemptNumber");
        }
        Objects.requireNonNull(attemptedAt, "attemptedAt");
        Objects.requireNonNull(resultingStatus, "resultingStatus");
        note = note == null ? "" : note;
    }
}
