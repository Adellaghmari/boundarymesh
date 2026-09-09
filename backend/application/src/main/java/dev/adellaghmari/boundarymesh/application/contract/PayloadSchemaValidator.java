package dev.adellaghmari.boundarymesh.application.contract;

import dev.adellaghmari.boundarymesh.domain.contract.ValidationViolation;
import java.util.List;

public interface PayloadSchemaValidator {

    List<ValidationViolation> validate(String schemaJson, String payloadJson);
}
