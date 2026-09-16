package dev.adellaghmari.boundarymesh.worker;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.client.WireMock;
import dev.adellaghmari.boundarymesh.application.delivery.DeliveryIdempotencyStore;
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
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryResultKind;
import dev.adellaghmari.boundarymesh.domain.delivery.ProviderFailureCategory;
import dev.adellaghmari.boundarymesh.worker.camel.DeliveryRouteBuilder;
import dev.adellaghmari.boundarymesh.worker.connector.ConnectorEndpointProvider;
import dev.adellaghmari.boundarymesh.worker.connector.ResilientConnectorClient;
import dev.adellaghmari.boundarymesh.worker.secret.LocalSecretValueProvider;
import dev.adellaghmari.boundarymesh.worker.stub.SyntheticProviderServer;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CamelDeliveryPipelineTest {

    @Autowired
    private DeliveryPort deliveryPort;

    @Autowired
    private SyntheticProviderServer providers;

    @Autowired
    private ResilientConnectorClient connectors;

    @Autowired
    private DeliveryIdempotencyStore idempotency;

    @Autowired
    private DeliveryRouteBuilder routes;

    @Autowired
    private MeterRegistry meters;

    @Autowired
    private ConnectorEndpointProvider endpoints;

    @BeforeEach
    void reset() {
        assertThat(endpoints.baseUrl("COVECRM")).isEqualTo(providers.baseUrl());
        connectors.reset("COVECRM");
        connectors.reset("LUMENWAY");
        connectors.reset("RIVERTIDE");
        idempotency.clear();
        routes.resetIdempotency();
        providers.resetScenarios();
        providers.server().resetRequests();
    }

    @Test
    void transformedCovePayloadIsDeliveredAndSecretStaysOnWorker() {
        var result = deliveryPort.deliver(request("COVECRM", ProviderScenario.SUCCESS, approved()));
        assertThat(result.kind()).isEqualTo(DeliveryResultKind.DELIVERED);
        assertThat(result.attemptCount()).isEqualTo(1);
        assertThat(result.routeId()).isEqualTo("sink.covecrm.activity");
        assertThat(result.safeExplanation()).doesNotContain(LocalSecretValueProvider.COVECRM_V2);
        providers
                .server()
                .verify(
                        1,
                        WireMock.postRequestedFor(WireMock.urlPathEqualTo("/covecrm/activities"))
                                .withRequestBody(WireMock.containing("[REDACTED]"))
                                .withRequestBody(WireMock.notContaining("ada.lovelace@example.test"))
                                .withHeader("X-Boundary-Credential-Version", WireMock.equalTo("2")));
    }

    @Test
    void lumenwayTranslatorChangesShape() {
        var result = deliveryPort.deliver(request("LUMENWAY", ProviderScenario.SUCCESS, approved()));
        assertThat(result.kind()).isEqualTo(DeliveryResultKind.DELIVERED);
        assertThat(result.routeId()).isEqualTo("sink.lumenway.fulfillment");
        providers
                .server()
                .verify(WireMock.postRequestedFor(WireMock.urlPathEqualTo("/lumenway/fulfillments"))
                        .withRequestBody(WireMock.containing("fulfillmentId"))
                        .withRequestBody(WireMock.containing("ord-1001"))
                        .withRequestBody(WireMock.notContaining("event_type")));
    }

    @Test
    void unknownDestinationDoesNotCallProvider() {
        var result = deliveryPort.deliver(request("NONE", ProviderScenario.SUCCESS, approved()));
        assertThat(result.kind()).isEqualTo(DeliveryResultKind.QUARANTINED);
        assertThat(result.providerCalled()).isFalse();
        assertThat(providers.server().findAll(WireMock.postRequestedFor(WireMock.urlMatching(".*"))))
                .isEmpty();
    }

    @Test
    void deliveredSuccessIncrementsMicrometer() {
        deliveryPort.deliver(request("COVECRM", ProviderScenario.SUCCESS, approved()));
        assertThat(meters.counter("boundarymesh.delivery.success").count()).isGreaterThanOrEqualTo(1.0);
        assertThat(meters.counter("boundarymesh.delivery.attempts").count()).isGreaterThanOrEqualTo(1.0);
    }

    @Test
    void duplicateInstructionIsSuppressed() {
        DeliveryRequest first = request("COVECRM", ProviderScenario.SUCCESS, approved());
        var initial = deliveryPort.deliver(first);
        assertThat(initial.kind())
                .as("first delivery %s %s", initial.kind(), initial.safeExplanation())
                .isEqualTo(DeliveryResultKind.DELIVERED);
        int afterFirst = providers
                .server()
                .findAll(WireMock.postRequestedFor(WireMock.urlPathEqualTo("/covecrm/activities")))
                .size();
        assertThat(afterFirst).isGreaterThanOrEqualTo(1);
        var second = deliveryPort.deliver(first);
        assertThat(second.kind()).isEqualTo(DeliveryResultKind.DUPLICATE_SUPPRESSED);
        assertThat(second.providerCalled()).isFalse();
        assertThat(providers
                        .server()
                        .findAll(WireMock.postRequestedFor(WireMock.urlPathEqualTo("/covecrm/activities")))
                        .size())
                .isEqualTo(afterFirst);
    }

    @Test
    void status400IsNotRetried() {
        var result = deliveryPort.deliver(request("COVECRM", ProviderScenario.STATUS_400, approved()));
        assertThat(result.kind()).isEqualTo(DeliveryResultKind.PERMANENT_FAILURE);
        assertThat(result.attemptCount()).isEqualTo(1);
        assertThat(result.providerCategory()).isEqualTo(ProviderFailureCategory.PERMANENT_REJECTION);
    }

    @Test
    void status401And403AreNotRetried() {
        assertThat(deliveryPort
                        .deliver(request("COVECRM", ProviderScenario.STATUS_401, approved()))
                        .attemptCount())
                .isEqualTo(1);
        assertThat(deliveryPort
                        .deliver(request("RIVERTIDE", ProviderScenario.STATUS_403, approved()))
                        .attemptCount())
                .isEqualTo(1);
    }

    @Test
    void status503RetriesThreeTimesThenQuarantines() {
        var result = deliveryPort.deliver(request("COVECRM", ProviderScenario.STATUS_503, approved()));
        assertThat(result.kind()).isEqualTo(DeliveryResultKind.QUARANTINED);
        assertThat(result.attemptCount()).isEqualTo(3);
        providers.server().verify(3, WireMock.postRequestedFor(WireMock.urlPathEqualTo("/covecrm/activities/503")));
    }

    @Test
    void status429RetriesThenSucceedsWhenStubAdvances() {
        var result = deliveryPort.deliver(request("COVECRM", ProviderScenario.STATUS_429_THEN_SUCCESS, approved()));
        assertThat(result.kind()).isEqualTo(DeliveryResultKind.DELIVERED);
        assertThat(result.attemptCount()).isEqualTo(2);
    }

    @Test
    void retiredCredentialDoesNotResolveSilently() {
        DeliveryRequest request = request("COVECRM", ProviderScenario.SUCCESS, approved());
        var retired = new DeliveryRequest(
                new DeliveryInstruction(
                        request.instruction().eventId(),
                        request.instruction().transactionId(),
                        request.instruction().integrationId(),
                        request.instruction().sourceSystem(),
                        request.instruction().destinationSystem(),
                        request.instruction().contractReference(),
                        request.instruction().egressPolicyReference(),
                        request.instruction().boundaryOutcome(),
                        request.instruction().payloadJson(),
                        request.instruction().correlationId(),
                        request.instruction().createdAt(),
                        new CredentialReference(
                                new CredentialId("covecrm-api"), new CredentialVersion("1"), CredentialStatus.RETIRED),
                        request.instruction().consentId()),
                ProviderScenario.SUCCESS);
        var result = deliveryPort.deliver(retired);
        assertThat(result.kind()).isEqualTo(DeliveryResultKind.QUARANTINED);
        assertThat(result.providerCalled()).isFalse();
    }

    private static DeliveryRequest request(String destination, ProviderScenario scenario, String payload) {
        CredentialReference credential =
                switch (destination) {
                    case "LUMENWAY" -> ref("lumenway-api", "1");
                    case "RIVERTIDE" -> ref("rivertide-api", "1");
                    default -> ref("covecrm-api", "2");
                };
        return new DeliveryRequest(
                new DeliveryInstruction(
                        new DeliveryInstructionId("dlv-" + UUID.randomUUID()),
                        TransactionId.of(UUID.randomUUID().toString()),
                        "quayforge-orders",
                        "QUAYFORGE",
                        destination,
                        "quayforge-orders:v1",
                        destination.toLowerCase(),
                        BoundaryOutcome.ALLOW_WITH_TRANSFORMATION,
                        payload,
                        "corr-1",
                        Instant.parse("2026-09-10T12:00:00Z"),
                        credential,
                        "consent-" + destination.toLowerCase()),
                scenario);
    }

    private static CredentialReference ref(String id, String version) {
        return new CredentialReference(new CredentialId(id), new CredentialVersion(version), CredentialStatus.ACTIVE);
    }

    private static String approved() {
        return "{\"event_id\":\"evt-1\",\"event_type\":\"order.updated\",\"order_id\":\"ord-1001\","
                + "\"customer_id\":\"cust-9\",\"email\":\"[REDACTED]\",\"shipping_address\":\"1 Synthetic Wharf\"}";
    }
}
