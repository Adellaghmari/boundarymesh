package dev.adellaghmari.boundarymesh.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

class ContractArtifactTests {

    @Test
    void jsonSchemasAreValid2020AndDistinguishVersions() throws Exception {
        String v1 = read("contracts/json-schema/quayforge-order.v1.schema.json");
        String v2 = read("contracts/json-schema/quayforge-order.v2.schema.json");
        SchemaRegistry registry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);
        Schema schemaV1 = registry.getSchema(v1);
        Schema schemaV2 = registry.getSchema(v2);
        String validV1 = "{\"event_id\":\"e1\",\"event_type\":\"order.updated\",\"order_id\":\"ord-1001\"}";
        String validV2 =
                "{\"event_id\":\"e1\",\"event_type\":\"order.updated\",\"order_id\":\"ord-1001\",\"order_status\":\"created\"}";
        assertThat(schemaV1.validate(validV1, InputFormat.JSON)).isEmpty();
        assertThat(schemaV1.validate(validV2, InputFormat.JSON)).isNotEmpty();
        assertThat(schemaV2.validate(validV2, InputFormat.JSON)).isEmpty();
    }

    @Test
    void openApiArtifactIsOpenApi31PartnerContract() throws Exception {
        JsonNode doc = new YAMLMapper().readTree(bytes("contracts/openapi/quayforge-orders-webhook.openapi.yaml"));
        assertThat(doc.get("openapi").stringValue()).startsWith("3.1");
        assertThat(doc.at("/paths/~1webhooks~1orders/post").isMissingNode()).isFalse();
        assertThat(doc.toString()).doesNotContain("secret");
        assertThat(doc.toString()).doesNotContain("Bearer eyJ");
    }

    @Test
    void asyncApiArtifactDefinesDeliveryInstructionWithoutSecrets() throws Exception {
        JsonNode doc = new YAMLMapper().readTree(bytes("contracts/asyncapi/delivery-instructions.asyncapi.yaml"));
        assertThat(doc.get("asyncapi").stringValue()).startsWith("3.");
        assertThat(doc.at("/channels/deliveryInstructions").isMissingNode()).isFalse();
        assertThat(doc.at("/components/schemas/DeliveryInstruction/required").toString())
                .contains("boundary_outcome");
        assertThat(doc.toString()).doesNotContain("client_secret");
        assertThat(doc.toString()).doesNotContain("password");
    }

    @Test
    void egressPoliciesArePresentAndVersioned() throws Exception {
        JsonMapper mapper = JsonMapper.shared();
        for (String path : new String[] {
            "contracts/policies/rivertide.egress.v1.json",
            "contracts/policies/covecrm.egress.v1.json",
            "contracts/policies/lumenway.egress.v1.json"
        }) {
            JsonNode node = mapper.readTree(bytes(path));
            assertThat(node.get("id").stringValue()).isNotBlank();
            assertThat(node.get("version").stringValue()).isEqualTo("v1");
            assertThat(node.get("destination").stringValue()).isNotBlank();
            assertThat(node.get("rules").isArray()).isTrue();
        }
    }

    private static String read(String classpath) throws Exception {
        return new String(bytes(classpath), StandardCharsets.UTF_8);
    }

    private static byte[] bytes(String classpath) throws Exception {
        return new ClassPathResource(classpath).getInputStream().readAllBytes();
    }
}
