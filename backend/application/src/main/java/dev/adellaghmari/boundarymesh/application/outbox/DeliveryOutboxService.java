package dev.adellaghmari.boundarymesh.application.outbox;

import dev.adellaghmari.boundarymesh.application.crossing.CrossingComposer.CrossingCommand;
import dev.adellaghmari.boundarymesh.application.crossing.CrossingComposer.CrossingResult;
import dev.adellaghmari.boundarymesh.application.delivery.DeliveryInstructionFactory;
import dev.adellaghmari.boundarymesh.domain.BoundaryOutcome;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryInstruction;
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;

/**
 * Application boundary for durable delivery authorization. Only {@code ALLOW} and {@code
 * ALLOW_WITH_TRANSFORMATION} persist an outbox record. {@code REJECT} and {@code QUARANTINE} write
 * nothing. Publishing happens later.
 */
public final class DeliveryOutboxService {

    private final DeliveryInstructionFactory instructions;
    private final OutboxRepository outbox;
    private final Clock clock;

    public DeliveryOutboxService(DeliveryInstructionFactory instructions, OutboxRepository outbox, Clock clock) {
        this.instructions = Objects.requireNonNull(instructions, "instructions");
        this.outbox = Objects.requireNonNull(outbox, "outbox");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public Optional<OutboxRecord> persistApprovedInstruction(CrossingResult result, CrossingCommand command) {
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(command, "command");
        BoundaryOutcome outcome = result.decision().outcome();
        if (outcome != BoundaryOutcome.ALLOW && outcome != BoundaryOutcome.ALLOW_WITH_TRANSFORMATION) {
            return Optional.empty();
        }
        Optional<DeliveryInstruction> instruction = instructions.from(result, command);
        if (instruction.isEmpty()) {
            return Optional.empty();
        }
        OutboxRecord record = OutboxRecord.pending(instruction.get(), clock.instant());
        outbox.savePending(record);
        return outbox.find(record.instructionId());
    }
}
