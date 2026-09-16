package dev.adellaghmari.boundarymesh.api.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.adellaghmari.boundarymesh.application.delivery.DeliveryStateQueryService;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryResult;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Product-safe read of the worker-owned delivery disposition; no Firestore or provider details. */
@RestController
@RequestMapping(path = "/api/v1/deliveries", produces = MediaType.APPLICATION_JSON_VALUE)
public class DeliveryStateController {

    private final DeliveryStateQueryService query;

    public DeliveryStateController(DeliveryStateQueryService query) {
        this.query = query;
    }

    @GetMapping("/{instructionId}")
    public ResponseEntity<DeliveryStateView> one(@PathVariable String instructionId) {
        return query.find(instructionId)
                .map(result -> ResponseEntity.ok(DeliveryStateView.from(instructionId, result)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public record DeliveryStateView(
            @JsonProperty("instruction_id") String instructionId,
            String state,
            @JsonProperty("provider_category") String providerCategory,
            @JsonProperty("provider_called") boolean providerCalled,
            @JsonProperty("attempt_count") int attemptCount,
            boolean terminal) {

        static DeliveryStateView from(String id, DeliveryResult result) {
            return new DeliveryStateView(
                    id,
                    result.kind().name(),
                    result.providerCategory().name(),
                    result.providerCalled(),
                    result.attemptCount(),
                    true);
        }
    }
}
