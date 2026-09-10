package dev.adellaghmari.boundarymesh.api.delivery;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.adellaghmari.boundarymesh.application.connector.ConnectorSafetyPort;
import dev.adellaghmari.boundarymesh.domain.connector.CircuitState;
import dev.adellaghmari.boundarymesh.domain.connector.ConnectorSafetySnapshot;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class HttpConnectorSafetyPort implements ConnectorSafetyPort {

    private final RestClient client;

    public HttpConnectorSafetyPort(RestClient.Builder builder, String workerBaseUrl) {
        this.client = builder.baseUrl(workerBaseUrl).build();
    }

    @Override
    public ConnectorSafetySnapshot snapshot(String destination) {
        try {
            SafetyView view = client.get()
                    .uri("/internal/connectors/{destination}/safety", destination)
                    .retrieve()
                    .body(SafetyView.class);
            if (view == null) {
                return new ConnectorSafetySnapshot(destination, false, CircuitState.UNKNOWN, false);
            }
            return new ConnectorSafetySnapshot(
                    view.destination(), view.configured(), CircuitState.valueOf(view.circuitState()), view.eligible());
        } catch (RestClientException ex) {
            return new ConnectorSafetySnapshot(destination, true, CircuitState.UNKNOWN, false);
        }
    }

    public record SafetyView(
            String destination,
            boolean configured,
            @JsonProperty("circuit_state") String circuitState,
            boolean eligible) {}
}
