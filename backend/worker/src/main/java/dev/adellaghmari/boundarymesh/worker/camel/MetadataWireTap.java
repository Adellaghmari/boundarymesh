package dev.adellaghmari.boundarymesh.worker.camel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Safe metadata observation only. Never logs payload values or secrets. */
@Component
public class MetadataWireTap implements Processor {

    private static final Logger log = LoggerFactory.getLogger(MetadataWireTap.class);
    private final CopyOnWriteArrayList<String> taps = new CopyOnWriteArrayList<>();

    @Override
    public void process(Exchange exchange) {
        String route = exchange.getFromRouteId();
        String destination = exchange.getMessage().getHeader("destination", String.class);
        String deliveryId = exchange.getMessage().getHeader("deliveryId", String.class);
        String note = "route=" + route + " destination=" + destination + " deliveryId=" + deliveryId;
        taps.add(note);
        log.info("Camel wire-tap {}", note);
    }

    public List<String> taps() {
        return new ArrayList<>(taps);
    }
}
