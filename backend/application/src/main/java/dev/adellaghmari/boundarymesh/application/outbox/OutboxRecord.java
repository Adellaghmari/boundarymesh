package dev.adellaghmari.boundarymesh.application.outbox;

import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryInstruction;
import java.time.Instant;
import java.util.Objects;

/**
 * Durable approved delivery instruction plus publish state. This record is the transactional unit:
 * application authorization state and outbox metadata commit together. Pub/Sub is not part of that
 * transaction.
 */
public record OutboxRecord(
        String instructionId,
        String integrationId,
        String destinationSystem,
        String schemaVersion,
        DeliveryInstruction instruction,
        OutboxStatus status,
        Instant createdAt,
        int publishAttempts,
        Instant lastAttemptAt,
        Instant publishedAt,
        PublishFailureKind lastFailureKind) {

    public static final String SCHEMA_VERSION = "delivery-outbox/v1";

    public OutboxRecord {
        Objects.requireNonNull(instructionId, "instructionId");
        Objects.requireNonNull(integrationId, "integrationId");
        Objects.requireNonNull(destinationSystem, "destinationSystem");
        Objects.requireNonNull(schemaVersion, "schemaVersion");
        Objects.requireNonNull(instruction, "instruction");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
        if (publishAttempts < 0) {
            throw new IllegalArgumentException("publishAttempts");
        }
        if (!instructionId.equals(instruction.eventId().value())) {
            throw new IllegalArgumentException("instruction id mismatch");
        }
    }

    public static OutboxRecord pending(DeliveryInstruction instruction, Instant createdAt) {
        return new OutboxRecord(
                instruction.eventId().value(),
                instruction.integrationId(),
                instruction.destinationSystem(),
                SCHEMA_VERSION,
                instruction,
                OutboxStatus.PENDING,
                createdAt,
                0,
                null,
                null,
                null);
    }

    public OutboxRecord published(Instant publishedAt) {
        return new OutboxRecord(
                instructionId,
                integrationId,
                destinationSystem,
                schemaVersion,
                instruction,
                OutboxStatus.PUBLISHED,
                createdAt,
                publishAttempts,
                publishedAt,
                publishedAt,
                null);
    }

    public OutboxRecord failedAttempt(Instant attemptedAt, PublishFailureKind kind) {
        return new OutboxRecord(
                instructionId,
                integrationId,
                destinationSystem,
                schemaVersion,
                instruction,
                OutboxStatus.PENDING,
                createdAt,
                publishAttempts + 1,
                attemptedAt,
                publishedAt,
                kind);
    }
}
