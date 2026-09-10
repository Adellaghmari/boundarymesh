package dev.adellaghmari.boundarymesh.worker.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.adellaghmari.boundarymesh.domain.connector.CircuitState;
import dev.adellaghmari.boundarymesh.domain.connector.ConnectorSafetySnapshot;
import dev.adellaghmari.boundarymesh.worker.connector.ResilientConnectorClient;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/internal/connectors", produces = MediaType.APPLICATION_JSON_VALUE)
public class ConnectorSafetyController {

    private static final Set<String> CONFIGURED = Set.of("COVECRM", "LUMENWAY", "RIVERTIDE");
    private final ResilientConnectorClient connectors;

    public ConnectorSafetyController(ResilientConnectorClient connectors) {
        this.connectors = connectors;
    }

    @GetMapping("/{destination}/safety")
    public SafetyView safety(@PathVariable String destination) {
        String dest = destination.toUpperCase(Locale.ROOT);
        CircuitState state = connectors.circuitState(dest);
        boolean configured = CONFIGURED.contains(dest);
        return SafetyView.from(
                new ConnectorSafetySnapshot(dest, configured, state, configured && state == CircuitState.CLOSED));
    }

    @PostMapping("/{destination}/fail/{count}")
    public SafetyView fail(@PathVariable String destination, @PathVariable int count) {
        connectors.recordFailures(destination.toUpperCase(Locale.ROOT), Math.min(count, 8));
        return safety(destination);
    }

    @PostMapping("/{destination}/reset")
    public SafetyView reset(@PathVariable String destination) {
        connectors.reset(destination.toUpperCase(Locale.ROOT));
        return safety(destination);
    }

    public record SafetyView(
            String destination,
            boolean configured,
            @JsonProperty("circuit_state") String circuitState,
            boolean eligible) {

        static SafetyView from(ConnectorSafetySnapshot snapshot) {
            return new SafetyView(
                    snapshot.destination(),
                    snapshot.configured(),
                    snapshot.circuitState().name(),
                    snapshot.eligible());
        }
    }
}
