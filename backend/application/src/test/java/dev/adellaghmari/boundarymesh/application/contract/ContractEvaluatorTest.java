package dev.adellaghmari.boundarymesh.application.contract;

import static org.assertj.core.api.Assertions.assertThat;

import dev.adellaghmari.boundarymesh.domain.ControlStatus;
import dev.adellaghmari.boundarymesh.domain.ReasonCode;
import dev.adellaghmari.boundarymesh.domain.contract.ContractEvaluation;
import dev.adellaghmari.boundarymesh.domain.contract.ContractId;
import dev.adellaghmari.boundarymesh.domain.contract.ContractReference;
import dev.adellaghmari.boundarymesh.domain.contract.ContractStatus;
import dev.adellaghmari.boundarymesh.domain.contract.ContractType;
import dev.adellaghmari.boundarymesh.domain.contract.ContractVersion;
import dev.adellaghmari.boundarymesh.domain.contract.ValidationViolation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ContractEvaluatorTest {

    private ContractEvaluator evaluator;

    @BeforeEach
    void setUp() {
        Map<String, RegisteredContract> store = new HashMap<>();
        store.put("quayforge-orders:v1", registered("v1", ContractStatus.SUPPORTED, "v1-schema"));
        store.put("quayforge-orders:v2", registered("v2", ContractStatus.SUPPORTED, "v2-schema"));
        store.put("quayforge-orders:v3", registered("v3", ContractStatus.UNSUPPORTED, "v3-schema"));
        evaluator = new ContractEvaluator(new InMemoryRegistry(store), (schema, payload) -> switch (schema) {
            case "v1-schema" ->
                payload.contains("\"order_id\"") && !payload.contains("\"order_status\"")
                        ? List.of()
                        : List.of(new ValidationViolation("/order_id", "required", "string", "missing"));
            case "v2-schema" ->
                payload.contains("\"order_status\"")
                        ? List.of()
                        : List.of(new ValidationViolation("/order_status", "required", "string", "missing"));
            default -> List.of();
        });
    }

    @Test
    void validSupportedPayloadPasses() {
        ContractEvaluation evaluation =
                evaluator.evaluate("quayforge-orders", "v1", "{\"event_id\":\"e\",\"order_id\":\"o\"}");
        assertThat(evaluation.classification()).isEqualTo("VALID");
        assertThat(evaluation.control().status()).isEqualTo(ControlStatus.PASS);
        assertThat(evaluation.violations()).isEmpty();
    }

    @Test
    void schemaViolationRejectsSupportedVersion() {
        ContractEvaluation evaluation = evaluator.evaluate("quayforge-orders", "v1", "{\"event_id\":\"e\"}");
        assertThat(evaluation.classification()).isEqualTo("SCHEMA_VIOLATION");
        assertThat(evaluation.control().reasonCodes()).contains(ReasonCode.CONTRACT_SCHEMA_INVALID);
        assertThat(evaluation.violations().getFirst().jsonPointer()).isEqualTo("/order_id");
    }

    @Test
    void unsupportedVersionIsDistinctFromSchemaViolation() {
        ContractEvaluation evaluation =
                evaluator.evaluate("quayforge-orders", "v3", "{\"event_id\":\"e\",\"order_id\":\"o\"}");
        assertThat(evaluation.classification()).isEqualTo("UNSUPPORTED_CONTRACT_VERSION");
        assertThat(evaluation.control().reasonCodes()).contains(ReasonCode.CONTRACT_VERSION_UNSUPPORTED);
    }

    @Test
    void driftWhenPayloadMatchesAnotherSupportedVersion() {
        ContractEvaluation evaluation = evaluator.evaluate(
                "quayforge-orders", "v1", "{\"event_id\":\"e\",\"order_id\":\"o\",\"order_status\":\"created\"}");
        assertThat(evaluation.classification()).isEqualTo("CONTRACT_DRIFT");
        assertThat(evaluation.control().reasonCodes()).contains(ReasonCode.CONTRACT_DRIFT_DETECTED);
    }

    @Test
    void malformedPayloadIsNotCalledDrift() {
        ContractEvaluation evaluation = evaluator.evaluate("quayforge-orders", "v1", "not-json");
        assertThat(evaluation.classification()).isEqualTo("MALFORMED_PAYLOAD");
        assertThat(evaluation.control().reasonCodes()).contains(ReasonCode.CONTRACT_MALFORMED_PAYLOAD);
    }

    @Test
    void unavailableIsExplicit() {
        ContractEvaluation evaluation = evaluator.unavailable("Synthetic contract registry unavailable.");
        assertThat(evaluation.classification()).isEqualTo("CONTRACT_UNAVAILABLE");
        assertThat(evaluation.control().status()).isEqualTo(ControlStatus.UNAVAILABLE);
    }

    @Test
    void truncatesViolationsAndDoesNotEmbedValues() {
        PayloadSchemaValidator noisy = (schema, payload) -> {
            java.util.ArrayList<ValidationViolation> items = new java.util.ArrayList<>();
            for (int i = 0; i < 25; i++) {
                items.add(new ValidationViolation("/extra_" + i, "additionalProperties", "none", "present"));
            }
            return items;
        };
        ContractEvaluator local = new ContractEvaluator(
                new InMemoryRegistry(
                        Map.of("quayforge-orders:v1", registered("v1", ContractStatus.SUPPORTED, "v1-schema"))),
                noisy);
        ContractEvaluation evaluation = local.evaluate("quayforge-orders", "v1", "{\"event_id\":\"e\"}");
        assertThat(evaluation.violations()).hasSize(20);
        assertThat(evaluation.truncatedCount()).isEqualTo(5);
        assertThat(evaluation.violations().toString()).doesNotContain("@");
        assertThat(evaluation.violations().toString()).doesNotContain("secret");
    }

    private static RegisteredContract registered(String version, ContractStatus status, String schema) {
        return new RegisteredContract(
                "quayforge-orders",
                new ContractReference(
                        new ContractId("quayforge-orders"),
                        new ContractVersion(version),
                        ContractType.JSON_SCHEMA,
                        status),
                schema);
    }

    private record InMemoryRegistry(Map<String, RegisteredContract> store) implements ContractRegistry {
        @Override
        public Optional<RegisteredContract> find(String integrationId, String version) {
            return Optional.ofNullable(store.get(integrationId + ":" + version));
        }

        @Override
        public List<RegisteredContract> list(String integrationId) {
            return store.values().stream()
                    .filter(item -> item.integrationId().equals(integrationId))
                    .toList();
        }
    }
}
