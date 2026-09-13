package dev.adellaghmari.boundarymesh.application.outbox;

/** Cloud-neutral consumer result. Adapters map this to Pub/Sub ACK or NACK. */
public enum ConsumeDisposition {
    ACK,
    NACK
}
