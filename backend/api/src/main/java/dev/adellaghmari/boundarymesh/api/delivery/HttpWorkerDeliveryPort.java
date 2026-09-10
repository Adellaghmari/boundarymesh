package dev.adellaghmari.boundarymesh.api.delivery;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.adellaghmari.boundarymesh.application.delivery.DeliveryPort;
import dev.adellaghmari.boundarymesh.application.delivery.DeliveryRequest;
import dev.adellaghmari.boundarymesh.domain.ReasonCode;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialId;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialReference;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialStatus;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialVersion;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryResult;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryResultKind;
import dev.adellaghmari.boundarymesh.domain.delivery.ProviderFailureCategory;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class HttpWorkerDeliveryPort implements DeliveryPort {

    private final RestClient client;

    public HttpWorkerDeliveryPort(RestClient.Builder builder, String workerBaseUrl) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofSeconds(8));
        this.client = builder.baseUrl(workerBaseUrl).requestFactory(factory).build();
    }

    @Override
    public DeliveryResult deliver(DeliveryRequest request) {
        try {
            WorkerResult body = client.post()
                    .uri("/internal/deliveries")
                    .body(toView(request))
                    .retrieve()
                    .body(WorkerResult.class);
            if (body == null) {
                return unavailable("Worker returned an empty delivery result.");
            }
            return body.toResult();
        } catch (RestClientException ex) {
            return unavailable("Delivery worker is unavailable.");
        }
    }

    private static Map<String, Object> toView(DeliveryRequest request) {
        var instruction = request.instruction();
        var credential = instruction.credentialReference();
        return Map.ofEntries(
                Map.entry("provider_scenario", request.providerScenario().name()),
                Map.entry(
                        "instruction",
                        Map.ofEntries(
                                Map.entry("event_id", instruction.eventId().value()),
                                Map.entry(
                                        "transaction_id",
                                        instruction.transactionId().value()),
                                Map.entry("integration_id", instruction.integrationId()),
                                Map.entry("source_system", instruction.sourceSystem()),
                                Map.entry("destination_system", instruction.destinationSystem()),
                                Map.entry(
                                        "contract_reference",
                                        instruction.contractReference() == null ? "" : instruction.contractReference()),
                                Map.entry(
                                        "egress_policy_reference",
                                        instruction.egressPolicyReference() == null
                                                ? ""
                                                : instruction.egressPolicyReference()),
                                Map.entry(
                                        "boundary_outcome",
                                        instruction.boundaryOutcome().name()),
                                Map.entry("payload_json", instruction.payloadJson()),
                                Map.entry("correlation_id", instruction.correlationId()),
                                Map.entry("created_at", instruction.createdAt().toString()),
                                Map.entry(
                                        "credential_id",
                                        credential == null
                                                ? ""
                                                : credential.id().value()),
                                Map.entry(
                                        "credential_version",
                                        credential == null
                                                ? "1"
                                                : credential.version().value()),
                                Map.entry(
                                        "credential_status",
                                        credential == null
                                                ? "ACTIVE"
                                                : credential.status().name()),
                                Map.entry(
                                        "consent_id",
                                        instruction.consentId() == null ? "" : instruction.consentId()))));
    }

    private static DeliveryResult unavailable(String explanation) {
        return new DeliveryResult(
                DeliveryResultKind.QUARANTINED,
                0,
                ProviderFailureCategory.TEMPORARY_FAILURE,
                ReasonCode.CONNECTOR_UNAVAILABLE,
                explanation,
                Duration.ZERO,
                null,
                null,
                "UNKNOWN",
                null,
                null,
                null,
                false);
    }

    public record WorkerResult(
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

        DeliveryResult toResult() {
            CredentialReference credential = null;
            if (credentialReference != null && credentialReference.contains(":v")) {
                String[] parts = credentialReference.split(":v");
                credential = new CredentialReference(
                        new CredentialId(parts[0]), new CredentialVersion(parts[1]), CredentialStatus.ACTIVE);
            }
            return new DeliveryResult(
                    DeliveryResultKind.valueOf(kind),
                    attemptCount,
                    ProviderFailureCategory.valueOf(providerCategory),
                    reason == null ? null : ReasonCode.of(reason, explanation == null ? reason : explanation),
                    explanation == null ? "" : explanation,
                    Duration.ofMillis(durationMs),
                    connectorId,
                    routeId,
                    circuitState,
                    httpStatus,
                    credential,
                    consentId,
                    providerCalled);
        }
    }
}
