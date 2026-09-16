package dev.adellaghmari.boundarymesh.worker.camel;

import dev.adellaghmari.boundarymesh.application.credential.SecretMaterial;
import dev.adellaghmari.boundarymesh.application.credential.SecretValueProvider;
import dev.adellaghmari.boundarymesh.application.delivery.DeliveryRequest;
import dev.adellaghmari.boundarymesh.application.delivery.ProviderScenario;
import dev.adellaghmari.boundarymesh.domain.ReasonCode;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialReference;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryResult;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryResultKind;
import dev.adellaghmari.boundarymesh.domain.delivery.ProviderFailureCategory;
import dev.adellaghmari.boundarymesh.worker.connector.ConnectorEndpointProvider;
import dev.adellaghmari.boundarymesh.worker.connector.FailureClassifier;
import dev.adellaghmari.boundarymesh.worker.connector.ResilientConnectorClient;
import java.time.Duration;
import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;

@Component("camelConnectorBridge")
public class CamelConnectorBridge {

    private final ResilientConnectorClient connectors;
    private final SecretValueProvider secrets;
    private final ConnectorEndpointProvider endpoints;

    public CamelConnectorBridge(
            ResilientConnectorClient connectors, SecretValueProvider secrets, ConnectorEndpointProvider endpoints) {
        this.connectors = connectors;
        this.secrets = secrets;
        this.endpoints = endpoints;
    }

    public void deliverCove(Exchange exchange) {
        deliver(exchange, "COVECRM", "/covecrm/activities");
    }

    public void deliverLumenway(Exchange exchange) {
        deliver(exchange, "LUMENWAY", "/lumenway/fulfillments");
    }

    public void deliverRivertide(Exchange exchange) {
        deliver(exchange, "RIVERTIDE", "/rivertide/settlements");
    }

    private void deliver(Exchange exchange, String destination, String basePath) {
        DeliveryRequest request = exchange.getProperty("deliveryRequest", DeliveryRequest.class);
        if (request == null) {
            request = exchange.getMessage().getBody(DeliveryRequest.class);
        }
        String body = exchange.getProperty("translator") != null
                ? exchange.getMessage().getBody(String.class)
                : request == null ? "" : request.instruction().payloadJson();
        CredentialReference reference =
                request == null ? null : request.instruction().credentialReference();
        String providerBaseUrl = endpoints.baseUrl(destination);
        if (providerBaseUrl.isBlank()) {
            exchange.getMessage().setBody(failed(destination, exchange, ReasonCode.CONNECTOR_UNAVAILABLE, 0, false));
            return;
        }
        if (reference == null) {
            exchange.getMessage().setBody(failed(destination, exchange, ReasonCode.CREDENTIAL_MISSING, 0, false));
            return;
        }
        try (SecretMaterial secret = secrets.resolve(reference).orElse(null)) {
            if (secret == null) {
                exchange.getMessage()
                        .setBody(failed(destination, exchange, ReasonCode.CREDENTIAL_PROVIDER_UNAVAILABLE, 0, false));
                return;
            }
            ProviderScenario scenario = request == null ? ProviderScenario.SUCCESS : request.providerScenario();
            String suffix = scenario == ProviderScenario.STATUS_429_THEN_SUCCESS
                    ? "/429-then-success"
                    : ResilientConnectorClient.pathFor(scenario);
            long start = System.nanoTime();
            ResilientConnectorClient.ConnectorCallResult call = connectors.post(
                    destination,
                    providerBaseUrl + basePath + suffix,
                    body,
                    reference.version().value(),
                    secret);
            Duration duration = Duration.ofNanos(System.nanoTime() - start);
            exchange.getMessage().setBody(toResult(destination, exchange, request, call, duration, reference));
        }
    }

    private DeliveryResult toResult(
            String destination,
            Exchange exchange,
            DeliveryRequest request,
            ResilientConnectorClient.ConnectorCallResult call,
            Duration duration,
            CredentialReference reference) {
        DeliveryResultKind kind;
        if (call.category() == ProviderFailureCategory.SUCCESS) {
            kind = DeliveryResultKind.DELIVERED;
        } else if (call.category() == ProviderFailureCategory.PERMANENT_REJECTION) {
            kind = DeliveryResultKind.PERMANENT_FAILURE;
        } else {
            kind = DeliveryResultKind.QUARANTINED;
        }
        return new DeliveryResult(
                kind,
                call.attempts(),
                call.category(),
                FailureClassifier.reason(call.category()),
                call.safeMessage(),
                duration,
                destination.toLowerCase(),
                exchange.getProperty("routeId", String.class),
                call.circuitState() == null
                        ? connectors.circuitState(destination).name()
                        : call.circuitState().name(),
                call.status() == 0 ? null : call.status(),
                reference,
                request == null ? null : request.instruction().consentId(),
                call.category() != ProviderFailureCategory.CIRCUIT_OPEN);
    }

    private DeliveryResult failed(
            String destination, Exchange exchange, ReasonCode reason, int attempts, boolean called) {
        return new DeliveryResult(
                DeliveryResultKind.QUARANTINED,
                attempts,
                ProviderFailureCategory.CREDENTIAL_OR_CONSENT,
                reason,
                reason.description(),
                Duration.ZERO,
                destination.toLowerCase(),
                exchange.getProperty("routeId", String.class),
                connectors.circuitState(destination).name(),
                null,
                null,
                null,
                called);
    }
}
