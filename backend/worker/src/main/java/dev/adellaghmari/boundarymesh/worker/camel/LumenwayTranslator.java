package dev.adellaghmari.boundarymesh.worker.camel;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Destination-specific Message Translator. Separate from the Data Boundary Firewall: this maps an
 * already-approved payload into Lumenway's fulfillment shape.
 */
@Component
public class LumenwayTranslator implements Processor {

    private final JsonMapper mapper = JsonMapper.shared();

    @Override
    public void process(Exchange exchange) {
        String approved = exchange.getMessage().getBody(String.class);
        JsonNode source = mapper.readTree(approved);
        ObjectNode target = mapper.createObjectNode();
        target.put("fulfillmentId", text(source, "order_id", "ful-unknown"));
        target.put("orderRef", text(source, "order_id", ""));
        target.put("shipTo", text(source, "shipping_address", "SYNTHETIC"));
        target.put("sourceEvent", text(source, "event_id", ""));
        target.put("customerRef", text(source, "customer_id", ""));
        exchange.getMessage().setBody(target.toString());
        exchange.setProperty("translator", "lumenway-fulfillment");
    }

    private static String text(JsonNode node, String field, String fallback) {
        JsonNode value = node.get(field);
        return value == null || !value.isString() ? fallback : value.stringValue();
    }
}
