package dev.adellaghmari.boundarymesh.api.web;

import dev.adellaghmari.boundarymesh.api.config.BoundaryMeshProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/microsoft", produces = MediaType.APPLICATION_JSON_VALUE)
public class MicrosoftConnectorController {

    private final BoundaryMeshProperties properties;

    public MicrosoftConnectorController(BoundaryMeshProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/connector")
    public Map<String, Object> connector() {
        Map<String, Object> body = new LinkedHashMap<>();
        boolean live = properties.entra().liveGraph();
        String mode = live ? "LIVE_VERIFIED" : "CONTRACT_HARNESS";
        if (!properties.entra().configured() && !live) {
            mode = "CONTRACT_HARNESS";
        }
        body.put("mode", mode);
        body.put("live", live);
        body.put("permission", "Calendars.Read");
        body.put("permission_kind", "delegated");
        body.put(
                "banner",
                live
                        ? "LIVE MICROSOFT GRAPH DATA"
                        : "CONTRACT HARNESS. Microsoft Graph Calendar is exercised through BoundaryMesh contracts without a live tenant.");
        body.put("subscriptions", "PLANNED");
        body.put("entra_configured", properties.entra().configured());
        return body;
    }
}
