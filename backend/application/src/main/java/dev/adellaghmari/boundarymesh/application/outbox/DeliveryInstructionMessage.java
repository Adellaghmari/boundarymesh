package dev.adellaghmari.boundarymesh.application.outbox;

import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryInstruction;
import java.util.Objects;

/** Versioned transport envelope. Adapters serialize this; Google types stay out of application. */
public record DeliveryInstructionMessage(String schemaVersion, DeliveryInstruction instruction) {

    public static final String SCHEMA_VERSION = "delivery-instruction/v1";

    public DeliveryInstructionMessage {
        Objects.requireNonNull(schemaVersion, "schemaVersion");
        Objects.requireNonNull(instruction, "instruction");
    }

    public static DeliveryInstructionMessage of(DeliveryInstruction instruction) {
        return new DeliveryInstructionMessage(SCHEMA_VERSION, instruction);
    }

    public String instructionId() {
        return instruction.eventId().value();
    }

    public String integrationId() {
        return instruction.integrationId();
    }

    public String destinationSystem() {
        return instruction.destinationSystem();
    }
}
