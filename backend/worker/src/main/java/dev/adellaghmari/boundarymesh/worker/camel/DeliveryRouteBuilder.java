package dev.adellaghmari.boundarymesh.worker.camel;

import dev.adellaghmari.boundarymesh.application.delivery.DeliveryRequest;
import dev.adellaghmari.boundarymesh.domain.ReasonCode;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryResult;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryResultKind;
import dev.adellaghmari.boundarymesh.domain.delivery.ProviderFailureCategory;
import java.time.Duration;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.support.processor.idempotent.MemoryIdempotentRepository;
import org.springframework.stereotype.Component;

@Component
public class DeliveryRouteBuilder extends RouteBuilder {

    public static final String DIRECT_DELIVERY = "direct:delivery";
    public static final String DIRECT_DLC = "direct:error.dlc";

    private final LumenwayTranslator lumenwayTranslator;
    private final MetadataWireTap wireTap;
    private final MemoryIdempotentRepository idempotentRepository = new MemoryIdempotentRepository();

    public DeliveryRouteBuilder(LumenwayTranslator lumenwayTranslator, MetadataWireTap wireTap) {
        this.lumenwayTranslator = lumenwayTranslator;
        this.wireTap = wireTap;
    }

    @Override
    public void configure() {
        errorHandler(deadLetterChannel(DIRECT_DLC).useOriginalMessage().maximumRedeliveries(0));

        from(DIRECT_DELIVERY)
                .routeId("delivery.inbound")
                .setProperty("deliveryRequest", body())
                .setHeader("destination", simple("${body.instruction.destinationSystem}"))
                .setHeader("deliveryId", simple("${body.instruction.eventId.value}"))
                .idempotentConsumer(header("deliveryId"), idempotentRepository)
                .skipDuplicate(false)
                .choice()
                .when(exchangeProperty(Exchange.DUPLICATE_MESSAGE))
                .process(exchange -> exchange.getMessage().setBody(duplicateResult(exchange)))
                .otherwise()
                .to("direct:route")
                .end();

        from("direct:route")
                .routeId("delivery.router")
                .wireTap("direct:tap-metadata")
                .choice()
                .when(header("destination").isEqualTo("COVECRM"))
                .to("direct:sink.covecrm")
                .when(header("destination").isEqualTo("LUMENWAY"))
                .to("direct:sink.lumenway")
                .when(header("destination").isEqualTo("RIVERTIDE"))
                .to("direct:sink.rivertide")
                .otherwise()
                .setProperty("unknownDestination", constant(true))
                .to(DIRECT_DLC);

        from("direct:sink.covecrm")
                .routeId("sink.covecrm.activity")
                .setProperty("routeId", constant("sink.covecrm.activity"))
                .setProperty("connectorId", constant("covecrm"))
                .to("bean:camelConnectorBridge?method=deliverCove");

        from("direct:sink.lumenway")
                .routeId("sink.lumenway.fulfillment")
                .setBody(simple("${exchangeProperty.deliveryRequest.instruction.payloadJson}"))
                .process(lumenwayTranslator)
                .setProperty("routeId", constant("sink.lumenway.fulfillment"))
                .setProperty("connectorId", constant("lumenway"))
                .to("bean:camelConnectorBridge?method=deliverLumenway");

        from("direct:sink.rivertide")
                .routeId("sink.rivertide.instruction")
                .setProperty("routeId", constant("sink.rivertide.instruction"))
                .setProperty("connectorId", constant("rivertide"))
                .to("bean:camelConnectorBridge?method=deliverRivertide");

        from("direct:tap-metadata").routeId("wiretap.metadata").process(wireTap);

        from(DIRECT_DLC).routeId("error.dlc").process(exchange -> exchange.getMessage()
                .setBody(unknownResult(exchange)));
    }

    public void resetIdempotency() {
        idempotentRepository.clear();
    }

    private static DeliveryResult duplicateResult(Exchange exchange) {
        DeliveryRequest request = exchange.getProperty("deliveryRequest", DeliveryRequest.class);
        return new DeliveryResult(
                DeliveryResultKind.DUPLICATE_SUPPRESSED,
                0,
                ProviderFailureCategory.DUPLICATE,
                ReasonCode.DELIVERY_DUPLICATE_SUPPRESSED,
                "Camel idempotent consumer recognized a duplicate delivery instruction.",
                Duration.ZERO,
                request == null
                        ? null
                        : request.instruction().destinationSystem().toLowerCase(),
                "delivery.inbound",
                null,
                null,
                request == null ? null : request.instruction().credentialReference(),
                request == null ? null : request.instruction().consentId(),
                false);
    }

    private static DeliveryResult unknownResult(Exchange exchange) {
        DeliveryRequest request = exchange.getProperty("deliveryRequest", DeliveryRequest.class);
        return new DeliveryResult(
                DeliveryResultKind.QUARANTINED,
                0,
                ProviderFailureCategory.UNKNOWN_DESTINATION,
                ReasonCode.CONNECTOR_UNKNOWN,
                "Destination is not a configured Camel connector route.",
                Duration.ZERO,
                request == null
                        ? "unknown"
                        : request.instruction().destinationSystem().toLowerCase(),
                "error.dlc",
                null,
                null,
                request == null ? null : request.instruction().credentialReference(),
                request == null ? null : request.instruction().consentId(),
                false);
    }
}
