package dev.adellaghmari.boundarymesh.worker.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.adellaghmari.boundarymesh.application.delivery.DeliveryPort;
import dev.adellaghmari.boundarymesh.application.delivery.DeliveryRequest;
import dev.adellaghmari.boundarymesh.application.delivery.ProviderScenario;
import dev.adellaghmari.boundarymesh.domain.BoundaryOutcome;
import dev.adellaghmari.boundarymesh.domain.TransactionId;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialId;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialReference;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialStatus;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialVersion;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryInstruction;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryInstructionId;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryResult;
import java.time.Instant;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/internal", produces = MediaType.APPLICATION_JSON_VALUE)
public class InternalDeliveryController {

    private final DeliveryPort deliveryPort;

    public InternalDeliveryController(DeliveryPort deliveryPort) {
        this.deliveryPort = deliveryPort;
    }

    @PostMapping(path = "/deliveries", consumes = MediaType.APPLICATION_JSON_VALUE)
    public DeliveryResultView deliver(@RequestBody DeliveryRequestView request) {
        DeliveryResult result = deliveryPort.deliver(request.toRequest());
        return DeliveryResultView.from(result);
    }

    public record DeliveryRequestView(
            InstructionView instruction, @JsonProperty("provider_scenario") String providerScenario) {

        DeliveryRequest toRequest() {
            ProviderScenario scenario = providerScenario == null || providerScenario.isBlank()
                    ? ProviderScenario.SUCCESS
                    : ProviderScenario.valueOf(providerScenario);
            return new DeliveryRequest(instruction.toInstruction(), scenario);
        }
    }

    public record InstructionView(
            @JsonProperty("event_id") String eventId,
            @JsonProperty("transaction_id") String transactionId,
            @JsonProperty("integration_id") String integrationId,
            @JsonProperty("source_system") String sourceSystem,
            @JsonProperty("destination_system") String destinationSystem,
            @JsonProperty("contract_reference") String contractReference,
            @JsonProperty("egress_policy_reference") String egressPolicyReference,
            @JsonProperty("boundary_outcome") String boundaryOutcome,
            @JsonProperty("payload_json") String payloadJson,
            @JsonProperty("correlation_id") String correlationId,
            @JsonProperty("created_at") String createdAt,
            @JsonProperty("credential_id") String credentialId,
            @JsonProperty("credential_version") String credentialVersion,
            @JsonProperty("credential_status") String credentialStatus,
            @JsonProperty("consent_id") String consentId) {

        DeliveryInstruction toInstruction() {
            CredentialReference credential = credentialId == null
                    ? null
                    : new CredentialReference(
                            new CredentialId(credentialId),
                            new CredentialVersion(credentialVersion == null ? "1" : credentialVersion),
                            credentialStatus == null
                                    ? CredentialStatus.ACTIVE
                                    : CredentialStatus.valueOf(credentialStatus));
            return new DeliveryInstruction(
                    new DeliveryInstructionId(eventId),
                    TransactionId.of(transactionId),
                    integrationId,
                    sourceSystem,
                    destinationSystem,
                    contractReference,
                    egressPolicyReference,
                    BoundaryOutcome.valueOf(boundaryOutcome),
                    payloadJson,
                    correlationId,
                    Instant.parse(createdAt),
                    credential,
                    consentId);
        }
    }

    public record DeliveryResultView(
            String kind,
            @JsonProperty("attempt_count") int attemptCount,
            @JsonProperty("provider_category") String providerCategory,
            String reason,
            String explanation,
            long durationMs,
            @JsonProperty("connector_id") String connectorId,
            @JsonProperty("route_id") String routeId,
            @JsonProperty("circuit_state") String circuitState,
            @JsonProperty("http_status") Integer httpStatus,
            @JsonProperty("credential_reference") String credentialReference,
            @JsonProperty("consent_id") String consentId,
            @JsonProperty("provider_called") boolean providerCalled) {

        static DeliveryResultView from(DeliveryResult result) {
            return new DeliveryResultView(
                    result.kind().name(),
                    result.attemptCount(),
                    result.providerCategory().name(),
                    result.reason() == null ? null : result.reason().code(),
                    result.safeExplanation(),
                    result.duration().toMillis(),
                    result.connectorId(),
                    result.routeId(),
                    result.circuitState(),
                    result.httpStatus(),
                    result.credentialReference() == null
                            ? null
                            : result.credentialReference().display(),
                    result.consentId(),
                    result.providerCalled());
        }
    }
}
