package dev.adellaghmari.boundarymesh.worker.connector;

/** Resolves non-secret connector endpoint configuration for the delivery runtime. */
@FunctionalInterface
public interface ConnectorEndpointProvider {

    String baseUrl(String destination);
}
