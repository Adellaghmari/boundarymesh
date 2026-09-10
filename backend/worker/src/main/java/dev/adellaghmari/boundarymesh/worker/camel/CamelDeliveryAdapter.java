package dev.adellaghmari.boundarymesh.worker.camel;

import dev.adellaghmari.boundarymesh.application.delivery.DeliveryIdempotencyStore;
import dev.adellaghmari.boundarymesh.application.delivery.DeliveryPort;
import dev.adellaghmari.boundarymesh.application.delivery.DeliveryRequest;
import dev.adellaghmari.boundarymesh.domain.ReasonCode;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryResult;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryResultKind;
import dev.adellaghmari.boundarymesh.domain.delivery.ProviderFailureCategory;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import org.apache.camel.ProducerTemplate;
import org.springframework.stereotype.Component;

@Component
public class CamelDeliveryAdapter implements DeliveryPort {

    private final ProducerTemplate producer;
    private final DeliveryIdempotencyStore idempotency;
    private final Counter attempts;
    private final Counter success;
    private final Counter failure;

    public CamelDeliveryAdapter(ProducerTemplate producer, DeliveryIdempotencyStore idempotency, MeterRegistry meters) {
        this.producer = producer;
        this.idempotency = idempotency;
        this.attempts = Counter.builder("boundarymesh.delivery.attempts")
                .description("Delivery attempts including first try")
                .register(meters);
        this.success = Counter.builder("boundarymesh.delivery.success").register(meters);
        this.failure = Counter.builder("boundarymesh.delivery.failure").register(meters);
    }

    @Override
    public DeliveryResult deliver(DeliveryRequest request) {
        String id = request.instruction().eventId().value();
        var previous = idempotency.previous(id);
        if (previous.isPresent()) {
            return new DeliveryResult(
                    DeliveryResultKind.DUPLICATE_SUPPRESSED,
                    0,
                    ProviderFailureCategory.DUPLICATE,
                    ReasonCode.DELIVERY_DUPLICATE_SUPPRESSED,
                    "Idempotent consumer recognized a duplicate delivery instruction.",
                    Duration.ZERO,
                    previous.get().connectorId(),
                    previous.get().routeId(),
                    previous.get().circuitState(),
                    previous.get().httpStatus(),
                    request.instruction().credentialReference(),
                    request.instruction().consentId(),
                    false);
        }
        attempts.increment();
        DeliveryResult result;
        try {
            result = producer.requestBody(DeliveryRouteBuilder.DIRECT_DELIVERY, request, DeliveryResult.class);
        } catch (RuntimeException ex) {
            result = new DeliveryResult(
                    DeliveryResultKind.QUARANTINED,
                    0,
                    ProviderFailureCategory.UNKNOWN_DESTINATION,
                    ReasonCode.CONNECTOR_UNKNOWN,
                    "Camel route terminated without a delivery result.",
                    Duration.ZERO,
                    request.instruction().destinationSystem().toLowerCase(),
                    "error.dlc",
                    null,
                    null,
                    request.instruction().credentialReference(),
                    request.instruction().consentId(),
                    false);
        }
        if (result == null) {
            result = new DeliveryResult(
                    DeliveryResultKind.QUARANTINED,
                    0,
                    ProviderFailureCategory.UNKNOWN_DESTINATION,
                    ReasonCode.CONNECTOR_UNKNOWN,
                    "Camel route returned no delivery result.",
                    Duration.ZERO,
                    request.instruction().destinationSystem().toLowerCase(),
                    "delivery.inbound",
                    null,
                    null,
                    request.instruction().credentialReference(),
                    request.instruction().consentId(),
                    false);
        }
        idempotency.remember(id, result);
        if (result.kind() == DeliveryResultKind.DELIVERED) {
            success.increment();
        } else if (result.kind() != DeliveryResultKind.DUPLICATE_SUPPRESSED) {
            failure.increment();
        }
        return result;
    }
}
