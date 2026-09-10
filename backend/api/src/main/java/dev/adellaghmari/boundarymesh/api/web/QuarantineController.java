package dev.adellaghmari.boundarymesh.api.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.adellaghmari.boundarymesh.api.security.JwtToPrincipalMapper;
import dev.adellaghmari.boundarymesh.application.crossing.CrossingDeliveryService;
import dev.adellaghmari.boundarymesh.application.quarantine.QuarantineOrchestrator;
import dev.adellaghmari.boundarymesh.domain.exception.InvalidInputException;
import dev.adellaghmari.boundarymesh.domain.identity.BoundaryPermission;
import dev.adellaghmari.boundarymesh.domain.identity.BoundaryPrincipal;
import dev.adellaghmari.boundarymesh.domain.quarantine.QuarantineId;
import dev.adellaghmari.boundarymesh.domain.quarantine.QuarantineItem;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/quarantine", produces = MediaType.APPLICATION_JSON_VALUE)
public class QuarantineController {

    private final QuarantineOrchestrator quarantine;
    private final CrossingDeliveryService deliveries;
    private final JwtToPrincipalMapper mapper;
    private final Counter openCounter;
    private final Counter reprocessCounter;

    public QuarantineController(
            QuarantineOrchestrator quarantine,
            CrossingDeliveryService deliveries,
            JwtToPrincipalMapper mapper,
            MeterRegistry meters) {
        this.quarantine = quarantine;
        this.deliveries = deliveries;
        this.mapper = mapper;
        this.openCounter = Counter.builder("boundarymesh.quarantine.open").register(meters);
        this.reprocessCounter = Counter.builder("boundarymesh.reprocess.total").register(meters);
    }

    @GetMapping
    public QuarantineListView list() {
        List<QuarantineItem> items = quarantine.list();
        return new QuarantineListView(
                "LOCAL / SYNTHETIC SESSION DATA. Not Firestore. Does not survive restart.",
                items.size(),
                items.stream().map(QuarantineItemView::from).toList());
    }

    @GetMapping("/{id}")
    public QuarantineItemView one(@PathVariable String id) {
        return QuarantineItemView.from(quarantine
                .find(new QuarantineId(id))
                .orElseThrow(() -> new InvalidInputException("QUARANTINE_NOT_FOUND", "quarantine item was not found")));
    }

    @PostMapping("/{id}/reprocess")
    public DeliveryLabResponse reprocess(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        BoundaryPrincipal principal = mapper.from(jwt);
        if (!principal.hasScope(BoundaryPermission.BOUNDARY_ADMIN)) {
            throw new InvalidInputException("REPROCESS_FORBIDDEN", "reprocess requires boundary.admin");
        }
        QuarantineItem item = quarantine
                .find(new QuarantineId(id))
                .orElseThrow(() -> new InvalidInputException("QUARANTINE_NOT_FOUND", "quarantine item was not found"));
        reprocessCounter.increment();
        var result = deliveries.reprocess(item, true);
        if (result.quarantine() != null && result.quarantine().status().name().equals("OPEN")) {
            openCounter.increment();
        }
        return DeliveryLabResponse.from(
                result, principal, result.crossing().egress().transformedJson());
    }

    public record QuarantineListView(String banner, int count, List<QuarantineItemView> items) {}

    public record QuarantineItemView(
            String id,
            @JsonProperty("transaction_id") String transactionId,
            String destination,
            String reason,
            String status,
            int attempts,
            @JsonProperty("max_attempts") int maxAttempts,
            boolean eligible,
            @JsonProperty("credential_reference") String credentialReference,
            @JsonProperty("connector_state") String connectorState,
            @JsonProperty("created_at") String createdAt) {

        static QuarantineItemView from(QuarantineItem item) {
            return new QuarantineItemView(
                    item.id().value(),
                    item.transactionId().value(),
                    item.destinationSystem(),
                    item.reason().name(),
                    item.status().name(),
                    item.recoveryAttempts(),
                    item.maxRecoveryAttempts(),
                    item.recoveryDecision()
                            == dev.adellaghmari.boundarymesh.domain.quarantine.RecoveryDecision.ELIGIBLE,
                    item.credentialReference(),
                    item.connectorState(),
                    item.createdAt().toString());
        }
    }
}
