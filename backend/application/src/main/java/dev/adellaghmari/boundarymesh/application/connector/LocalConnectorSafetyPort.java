package dev.adellaghmari.boundarymesh.application.connector;

import dev.adellaghmari.boundarymesh.domain.connector.CircuitState;
import dev.adellaghmari.boundarymesh.domain.connector.ConnectorSafetySnapshot;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class LocalConnectorSafetyPort implements ConnectorSafetyPort {

    private static final Set<String> CONFIGURED = Set.of("COVECRM", "LUMENWAY", "RIVERTIDE", "M365_CALENDAR");
    private final ConcurrentHashMap<String, CircuitState> states = new ConcurrentHashMap<>();

    public void setState(String destination, CircuitState state) {
        states.put(normalize(destination), state);
    }

    @Override
    public ConnectorSafetySnapshot snapshot(String destination) {
        String dest = normalize(destination);
        boolean configured = CONFIGURED.contains(dest);
        CircuitState state = states.getOrDefault(dest, configured ? CircuitState.CLOSED : CircuitState.UNKNOWN);
        boolean eligible = configured && state == CircuitState.CLOSED;
        return new ConnectorSafetySnapshot(dest, configured, state, eligible);
    }

    private static String normalize(String destination) {
        return destination == null ? "" : destination.trim().toUpperCase(Locale.ROOT);
    }
}
