package dev.adellaghmari.boundarymesh.application.connector;

import dev.adellaghmari.boundarymesh.domain.ControlKind;
import dev.adellaghmari.boundarymesh.domain.ControlResult;
import dev.adellaghmari.boundarymesh.domain.ReasonCode;
import dev.adellaghmari.boundarymesh.domain.connector.CircuitState;
import dev.adellaghmari.boundarymesh.domain.connector.ConnectorSafetySnapshot;
import java.util.Locale;
import java.util.Objects;

public final class ConnectorSafetyEvaluator {

    private final ConnectorSafetyPort port;

    public ConnectorSafetyEvaluator(ConnectorSafetyPort port) {
        this.port = Objects.requireNonNull(port, "port");
    }

    public ControlResult evaluate(String destination, String scenario) {
        String profile = scenario == null ? "closed" : scenario.trim().toLowerCase(Locale.ROOT);
        if ("open".equals(profile) || "circuit-open".equals(profile)) {
            return ControlResult.unavailable(
                    ControlKind.CONNECTOR_SAFETY,
                    ReasonCode.CONNECTOR_CIRCUIT_OPEN,
                    "Connector circuit is OPEN; no outbound attempt will be made.");
        }
        if ("unknown".equals(profile)) {
            return ControlResult.fail(
                    ControlKind.CONNECTOR_SAFETY,
                    ReasonCode.CONNECTOR_UNKNOWN,
                    "Destination is not a configured connector.");
        }
        ConnectorSafetySnapshot snapshot = port.snapshot(destination);
        if (!snapshot.configured()) {
            return ControlResult.fail(
                    ControlKind.CONNECTOR_SAFETY,
                    ReasonCode.CONNECTOR_UNKNOWN,
                    "Destination is not a configured connector.");
        }
        if (snapshot.circuitState() == CircuitState.OPEN) {
            return ControlResult.unavailable(
                    ControlKind.CONNECTOR_SAFETY,
                    ReasonCode.CONNECTOR_CIRCUIT_OPEN,
                    "Connector circuit is OPEN; no outbound attempt will be made.");
        }
        if (snapshot.circuitState() == CircuitState.UNKNOWN) {
            return ControlResult.unavailable(
                    ControlKind.CONNECTOR_SAFETY,
                    ReasonCode.CONNECTOR_UNAVAILABLE,
                    "Connector safety snapshot is unavailable.");
        }
        return new ControlResult(
                ControlKind.CONNECTOR_SAFETY,
                dev.adellaghmari.boundarymesh.domain.ControlStatus.PASS,
                java.util.List.of(),
                "Connector is configured and circuit is " + snapshot.circuitState() + ".",
                snapshot.circuitState().name());
    }
}
