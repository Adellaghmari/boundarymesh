package dev.adellaghmari.boundarymesh.domain.connector;

import java.util.Objects;

/**
 * Whether a connector is eligible to receive a new crossing. This is not a delivery success result.
 */
public record ConnectorSafetySnapshot(
        String destination, boolean configured, CircuitState circuitState, boolean eligible) {

    public ConnectorSafetySnapshot {
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(circuitState, "circuitState");
    }
}
