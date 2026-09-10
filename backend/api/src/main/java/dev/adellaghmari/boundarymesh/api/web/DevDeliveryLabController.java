package dev.adellaghmari.boundarymesh.api.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.adellaghmari.boundarymesh.api.security.JwtToPrincipalMapper;
import dev.adellaghmari.boundarymesh.application.crossing.CrossingDeliveryService;
import dev.adellaghmari.boundarymesh.application.crossing.CrossingScenarios;
import dev.adellaghmari.boundarymesh.application.delivery.ProviderScenario;
import dev.adellaghmari.boundarymesh.domain.identity.BoundaryPrincipal;
import jakarta.validation.Valid;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/dev/delivery-lab", produces = MediaType.APPLICATION_JSON_VALUE)
@ConditionalOnProperty(name = "boundarymesh.dev-evaluate-enabled", havingValue = "true")
public class DevDeliveryLabController {

    private static final Logger log = LoggerFactory.getLogger(DevDeliveryLabController.class);

    private final CrossingDeliveryService deliveries;
    private final JwtToPrincipalMapper mapper;

    public DevDeliveryLabController(CrossingDeliveryService deliveries, JwtToPrincipalMapper mapper) {
        this.deliveries = deliveries;
        this.mapper = mapper;
    }

    @PostMapping(path = "/{scenario}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public DeliveryLabResponse run(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String scenario,
            @Valid @RequestBody(required = false) DeliveryLabRequest request) {
        BoundaryPrincipal principal = mapper.from(jwt);
        var command = CrossingScenarios.delivery(scenario).withPrincipal(principal);
        ProviderScenario provider = providerFor(scenario, request);
        var result = "duplicate".equalsIgnoreCase(scenario)
                ? deliveries.executeThenReplay(command, provider)
                : deliveries.execute(command, provider);
        MDC.put("transaction_id", result.crossing().decision().transactionId().value());
        MDC.put("boundary_outcome", result.crossing().decision().outcome().name());
        MDC.put("delivery_result", result.delivery().kind().name());
        log.info("Synthetic delivery lab evaluated");
        return DeliveryLabResponse.from(result, principal, command.payloadJson());
    }

    private static ProviderScenario providerFor(String scenario, DeliveryLabRequest request) {
        if (request != null
                && request.providerScenario() != null
                && !request.providerScenario().isBlank()) {
            return ProviderScenario.valueOf(request.providerScenario().trim().toUpperCase(Locale.ROOT));
        }
        return switch (scenario) {
            case "rate-limit" -> ProviderScenario.STATUS_429;
            case "rate-limit-then-success" -> ProviderScenario.STATUS_429_THEN_SUCCESS;
            case "server-error" -> ProviderScenario.STATUS_503;
            case "duplicate" -> ProviderScenario.SUCCESS;
            default -> ProviderScenario.SUCCESS;
        };
    }

    public record DeliveryLabRequest(@JsonProperty("provider_scenario") String providerScenario) {}
}
