package dev.adellaghmari.boundarymesh.api.contract;

import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import dev.adellaghmari.boundarymesh.application.contract.PayloadSchemaValidator;
import dev.adellaghmari.boundarymesh.domain.contract.ValidationViolation;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class NetworkntPayloadSchemaValidator implements PayloadSchemaValidator {

    private final SchemaRegistry registry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);
    private final JsonMapper mapper = JsonMapper.shared();

    @Override
    public List<ValidationViolation> validate(String schemaJson, String payloadJson) {
        Schema schema = registry.getSchema(schemaJson);
        List<Error> errors = new ArrayList<>(schema.validate(payloadJson, InputFormat.JSON));
        JsonNode payload = mapper.readTree(payloadJson);
        List<ValidationViolation> violations = new ArrayList<>();
        for (Error error : errors) {
            String pointer = error.getInstanceLocation() == null
                    ? ""
                    : error.getInstanceLocation().toString();
            if (!pointer.isEmpty() && !pointer.startsWith("/")) {
                pointer = "/" + pointer;
            }
            JsonNode node = pointer.isEmpty() ? payload : payload.at(pointer);
            violations.add(new ValidationViolation(
                    pointer.isEmpty() ? "/" : pointer,
                    error.getKeyword() == null ? "unknown" : error.getKeyword(),
                    expectedCategory(error),
                    actualCategory(node)));
        }
        return violations;
    }

    private static String expectedCategory(Error error) {
        if (error.getSchemaNode() != null && error.getSchemaNode().has("type")) {
            return error.getSchemaNode().get("type").asText();
        }
        return error.getKeyword() == null ? "" : error.getKeyword();
    }

    private static String actualCategory(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "missing";
        }
        if (node.isTextual()) {
            return "string";
        }
        if (node.isNumber()) {
            return "number";
        }
        if (node.isBoolean()) {
            return "boolean";
        }
        if (node.isArray()) {
            return "array";
        }
        if (node.isObject()) {
            return "object";
        }
        return "unknown";
    }
}
