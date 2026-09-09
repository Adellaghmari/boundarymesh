package dev.adellaghmari.boundarymesh.application.contract;

import dev.adellaghmari.boundarymesh.domain.ControlKind;
import dev.adellaghmari.boundarymesh.domain.ControlResult;
import dev.adellaghmari.boundarymesh.domain.ReasonCode;
import dev.adellaghmari.boundarymesh.domain.contract.ContractEvaluation;
import dev.adellaghmari.boundarymesh.domain.contract.ContractReference;
import dev.adellaghmari.boundarymesh.domain.contract.ContractStatus;
import dev.adellaghmari.boundarymesh.domain.contract.ValidationViolation;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ContractEvaluator {

    public static final String QUAYFORGE_ORDERS = "quayforge-orders";

    private final ContractRegistry registry;
    private final PayloadSchemaValidator validator;

    public ContractEvaluator(ContractRegistry registry, PayloadSchemaValidator validator) {
        this.registry = Objects.requireNonNull(registry);
        this.validator = Objects.requireNonNull(validator);
    }

    public ContractEvaluation evaluate(String integrationId, String declaredVersion, String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank() || !looksLikeJson(payloadJson)) {
            return evaluation(
                    null,
                    ControlResult.fail(
                            ControlKind.CONTRACT, ReasonCode.CONTRACT_MALFORMED_PAYLOAD, "Payload is not usable JSON."),
                    List.of(),
                    0,
                    "MALFORMED_PAYLOAD");
        }
        String version = declaredVersion == null || declaredVersion.isBlank() ? "v1" : declaredVersion.trim();
        Optional<RegisteredContract> found = registry.find(integrationId, version);
        if (found.isEmpty()) {
            return evaluation(
                    null,
                    ControlResult.fail(
                            ControlKind.CONTRACT,
                            ReasonCode.CONTRACT_VERSION_UNSUPPORTED,
                            "Declared contract version is not registered."),
                    List.of(),
                    0,
                    "UNSUPPORTED_CONTRACT_VERSION");
        }
        RegisteredContract registered = found.get();
        ContractReference reference = registered.reference();
        if (reference.status() == ContractStatus.UNSUPPORTED) {
            return evaluation(
                    reference,
                    ControlResult.fail(
                            ControlKind.CONTRACT,
                            ReasonCode.CONTRACT_VERSION_UNSUPPORTED,
                            "Declared contract version is registered as unsupported."),
                    List.of(),
                    0,
                    "UNSUPPORTED_CONTRACT_VERSION");
        }
        List<ValidationViolation> raw = validator.validate(registered.schemaJson(), payloadJson);
        List<ValidationViolation> truncated = raw.size() > ContractEvaluation.MAX_VIOLATIONS
                ? List.copyOf(raw.subList(0, ContractEvaluation.MAX_VIOLATIONS))
                : List.copyOf(raw);
        int overflow = Math.max(0, raw.size() - truncated.size());
        if (truncated.isEmpty()) {
            return evaluation(
                    reference,
                    new ControlResult(
                            ControlKind.CONTRACT,
                            dev.adellaghmari.boundarymesh.domain.ControlStatus.PASS,
                            List.of(),
                            "Supported contract " + reference.version() + " accepted the payload.",
                            reference.id().value() + ":" + reference.version()),
                    List.of(),
                    0,
                    "VALID");
        }
        if (matchesOtherSupportedVersion(integrationId, version, payloadJson)) {
            return evaluation(
                    reference,
                    ControlResult.fail(
                            ControlKind.CONTRACT,
                            ReasonCode.CONTRACT_DRIFT_DETECTED,
                            "Payload fails the declared version but matches another supported contract."),
                    truncated,
                    overflow,
                    "CONTRACT_DRIFT");
        }
        return evaluation(
                reference,
                ControlResult.fail(
                        ControlKind.CONTRACT,
                        ReasonCode.CONTRACT_SCHEMA_INVALID,
                        "Payload failed the supported contract schema."),
                truncated,
                overflow,
                "SCHEMA_VIOLATION");
    }

    public ContractEvaluation unavailable(String reason) {
        return evaluation(
                null,
                ControlResult.unavailable(ControlKind.CONTRACT, ReasonCode.CONTRACT_UNAVAILABLE, reason),
                List.of(),
                0,
                "CONTRACT_UNAVAILABLE");
    }

    private boolean matchesOtherSupportedVersion(String integrationId, String declaredVersion, String payloadJson) {
        return registry.list(integrationId).stream()
                .filter(item -> item.reference().status() == ContractStatus.SUPPORTED)
                .filter(item -> !item.reference().version().value().equals(declaredVersion))
                .anyMatch(item ->
                        validator.validate(item.schemaJson(), payloadJson).isEmpty());
    }

    private static boolean looksLikeJson(String payloadJson) {
        String trimmed = payloadJson.trim();
        return trimmed.startsWith("{") && trimmed.endsWith("}");
    }

    private static ContractEvaluation evaluation(
            ContractReference reference,
            ControlResult control,
            List<ValidationViolation> violations,
            int truncated,
            String classification) {
        return new ContractEvaluation(reference, control, new ArrayList<>(violations), truncated, classification);
    }
}
