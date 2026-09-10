package dev.adellaghmari.boundarymesh.api.web;

import dev.adellaghmari.boundarymesh.application.crossing.CrossingDeliveryService.CrossingDeliveryResult;
import dev.adellaghmari.boundarymesh.application.microsoft.MicrosoftCalendarCrossingService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/dev/microsoft-lab", produces = MediaType.APPLICATION_JSON_VALUE)
public class DevMicrosoftLabController {

    private final MicrosoftCalendarCrossingService crossings;

    public DevMicrosoftLabController(MicrosoftCalendarCrossingService crossings) {
        this.crossings = crossings;
    }

    @PostMapping("/harness-crossing")
    public Map<String, Object> harness() {
        var result = crossings.pull(MicrosoftCalendarCrossingService.harnessPrincipal(), false);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("mode", "CONTRACT_HARNESS");
        body.put("live", false);
        body.put("provider_status", result.providerStatus());
        body.put("connector_ok", result.connectorOk());
        body.put("count", result.crossings().size());
        if (!result.crossings().isEmpty()) {
            CrossingDeliveryResult first = result.crossings().getFirst();
            body.put("boundary_outcome", first.crossing().decision().outcome().name());
            body.put("contract_id", "m365-calendar");
            body.put("contract_version", "v1");
            body.put(
                    "egress_policy_id",
                    first.crossing().egress().policyId() == null
                            ? null
                            : first.crossing().egress().policyId().value());
            body.put(
                    "transformation_count",
                    first.crossing().egress().summary() == null
                            ? 0
                            : first.crossing().egress().summary().transformationCount());
            body.put(
                    "attestation_id",
                    first.attestation() == null
                            ? null
                            : first.attestation().id().value());
            body.put(
                    "audit_chain_id",
                    first.attestation() == null
                            ? null
                            : first.attestation().auditChainId().value());
        }
        body.put("banner", "CONTRACT HARNESS. Subjects and attendees are not shown.");
        return body;
    }
}
