package dev.adellaghmari.boundarymesh.worker;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.client.WireMock;
import dev.adellaghmari.boundarymesh.application.delivery.DeliveryIdempotencyStore;
import dev.adellaghmari.boundarymesh.application.delivery.DeliveryPort;
import dev.adellaghmari.boundarymesh.application.delivery.DeliveryRequest;
import dev.adellaghmari.boundarymesh.application.delivery.ProviderScenario;
import dev.adellaghmari.boundarymesh.domain.BoundaryOutcome;
import dev.adellaghmari.boundarymesh.domain.TransactionId;
import dev.adellaghmari.boundarymesh.domain.connector.CircuitState;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialId;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialReference;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialStatus;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialVersion;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryInstruction;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryInstructionId;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryResultKind;
import dev.adellaghmari.boundarymesh.domain.delivery.ProviderFailureCategory;
import dev.adellaghmari.boundarymesh.worker.connector.ResilientConnectorClient;
import dev.adellaghmari.boundarymesh.worker.connector.RetryAfterParser;
import dev.adellaghmari.boundarymesh.worker.stub.SyntheticProviderServer;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ResilienceAndCircuitTest {

    @Autowired
    private DeliveryPort deliveryPort;

    @Autowired
    private ResilientConnectorClient connectors;

    @Autowired
    private SyntheticProviderServer providers;

    @Autowired
    private DeliveryIdempotencyStore idempotency;

    @BeforeEach
    void reset() {
        connectors.reset("COVECRM");
        connectors.reset("LUMENWAY");
        connectors.reset("RIVERTIDE");
        idempotency.clear();
        providers.resetScenarios();
        providers.server().resetRequests();
    }

    @Test
    void successIsSingleAttempt() {
        var result = deliveryPort.deliver(cove(ProviderScenario.SUCCESS));
        assertThat(result.kind()).isEqualTo(DeliveryResultKind.DELIVERED);
        assertThat(result.attemptCount()).isEqualTo(1);
    }

    @Test
    void retryAfterIsCapped() {
        assertThat(RetryAfterParser.parse("3600", Duration.ofMillis(50))).isEqualTo(Duration.ofMillis(50));
        assertThat(RetryAfterParser.parse("-5", Duration.ofSeconds(2))).isEqualTo(Duration.ZERO);
        assertThat(RetryAfterParser.parse("not-a-value", Duration.ofSeconds(2))).isEqualTo(Duration.ZERO);
    }

    @Test
    void openCircuitFailsFastThenHalfOpenRecovers() throws Exception {
        connectors.recordFailures("COVECRM", 4);
        assertThat(connectors.circuitState("COVECRM")).isEqualTo(CircuitState.OPEN);
        providers.server().resetRequests();
        var blocked = deliveryPort.deliver(cove(ProviderScenario.SUCCESS));
        assertThat(blocked.providerCategory()).isEqualTo(ProviderFailureCategory.CIRCUIT_OPEN);
        assertThat(blocked.providerCalled()).isFalse();
        assertThat(providers.server().findAll(WireMock.postRequestedFor(WireMock.urlMatching("/covecrm/.*"))))
                .isEmpty();
        Thread.sleep(120);
        var recovered = deliveryPort.deliver(cove(ProviderScenario.SUCCESS));
        assertThat(recovered.kind()).isEqualTo(DeliveryResultKind.DELIVERED);
        assertThat(connectors.circuitState("COVECRM")).isEqualTo(CircuitState.CLOSED);
    }

    private DeliveryRequest cove(ProviderScenario scenario) {
        return new DeliveryRequest(
                new DeliveryInstruction(
                        new DeliveryInstructionId("dlv-" + UUID.randomUUID()),
                        TransactionId.of(UUID.randomUUID().toString()),
                        "quayforge-orders",
                        "QUAYFORGE",
                        "COVECRM",
                        "quayforge-orders:v1",
                        "covecrm",
                        BoundaryOutcome.ALLOW_WITH_TRANSFORMATION,
                        "{\"order_id\":\"ord-1001\",\"email\":\"[REDACTED]\"}",
                        "corr",
                        Instant.parse("2026-09-10T12:00:00Z"),
                        new CredentialReference(
                                new CredentialId("covecrm-api"), new CredentialVersion("2"), CredentialStatus.ACTIVE),
                        "covecrm-activity"),
                scenario);
    }
}
