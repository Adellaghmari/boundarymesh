package dev.adellaghmari.boundarymesh.api.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.adellaghmari.boundarymesh.api.config.BoundaryMeshProperties;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
public class HealthController {

    private final BoundaryMeshProperties properties;

    public HealthController(BoundaryMeshProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("UP", properties.serviceName(), "SYNTHETIC DEMO / LOCAL");
    }

    @GetMapping("/ready")
    public ReadyResponse ready() {
        return new ReadyResponse("READY", "Phase 1 local foundation has no downstream dependencies.");
    }

    public record HealthResponse(String status, String service, String truth) {}

    public record ReadyResponse(String status, @JsonProperty("detail") String detail) {}
}
