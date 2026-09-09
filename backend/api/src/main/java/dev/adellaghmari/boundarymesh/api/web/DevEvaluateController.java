package dev.adellaghmari.boundarymesh.api.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.adellaghmari.boundarymesh.application.BoundaryEnforcementEngine;
import dev.adellaghmari.boundarymesh.domain.BoundaryDecision;
import dev.adellaghmari.boundarymesh.domain.BoundaryTransaction;
import dev.adellaghmari.boundarymesh.domain.ControlKind;
import dev.adellaghmari.boundarymesh.domain.ControlResult;
import dev.adellaghmari.boundarymesh.domain.ControlStatus;
import dev.adellaghmari.boundarymesh.domain.ControlVector;
import dev.adellaghmari.boundarymesh.domain.DestinationSystem;
import dev.adellaghmari.boundarymesh.domain.IntegrationId;
import dev.adellaghmari.boundarymesh.domain.ReasonCode;
import dev.adellaghmari.boundarymesh.domain.SourceSystem;
import dev.adellaghmari.boundarymesh.domain.TransactionId;
import dev.adellaghmari.boundarymesh.domain.exception.InvalidInputException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * LOCAL/TEST synthetic evaluation only. Not a production ingress. Not Entra/HMAC/Graph.
 */
@RestController
@RequestMapping(path = "/api/v1/dev", produces = MediaType.APPLICATION_JSON_VALUE)
@ConditionalOnProperty(name = "boundarymesh.dev-evaluate-enabled", havingValue = "true")
public class DevEvaluateController {

    private static final Logger log = LoggerFactory.getLogger(DevEvaluateController.class);

    private final BoundaryEnforcementEngine engine;

    public DevEvaluateController(BoundaryEnforcementEngine engine) {
        this.engine = engine;
    }

    @PostMapping(path = "/evaluate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public EvaluateResponse evaluate(@Valid @RequestBody EvaluateRequest request) {
        if (!request.synthetic()) {
            throw new InvalidInputException(
                    "SYNTHETIC_REQUIRED", "Phase 1 evaluate only accepts synthetic ControlVector payloads.");
        }
        TransactionId transactionId = TransactionId.of(request.transactionId());
        MDC.put("transaction_id", transactionId.value());
        ControlVector vector = request.toVector();
        BoundaryDecision decision = engine.decide(transactionId, vector);
        MDC.put("boundary_outcome", decision.outcome().name());
        if (!decision.reasonCodes().isEmpty()) {
            MDC.put("reason_code", decision.reasonCodes().getFirst().code());
        }
        BoundaryTransaction.received(
                        transactionId,
                        IntegrationId.of(request.integrationId()),
                        request.sourceSystem(),
                        request.destinationSystem(),
                        decision.decidedAt())
                .apply(decision.outcome());
        log.info("Synthetic boundary evaluation completed");
        return EvaluateResponse.from(decision);
    }

    public record EvaluateRequest(
            boolean synthetic,
            @NotBlank @JsonProperty("transaction_id") String transactionId,
            @NotBlank @JsonProperty("integration_id") String integrationId,
            @NotNull @JsonProperty("source_system") SourceSystem sourceSystem,
            @NotNull @JsonProperty("destination_system") DestinationSystem destinationSystem,
            @NotNull Map<String, ControlInput> controls) {

        ControlVector toVector() {
            return ControlVector.of(
                    require(ControlKind.IDENTITY),
                    require(ControlKind.AUTHORIZATION),
                    require(ControlKind.AUTHENTICITY),
                    require(ControlKind.FRESHNESS),
                    require(ControlKind.REPLAY),
                    require(ControlKind.CONTRACT),
                    require(ControlKind.EGRESS),
                    require(ControlKind.CREDENTIAL_RESOLVABILITY),
                    require(ControlKind.CONNECTOR_SAFETY));
        }

        private ControlResult require(ControlKind kind) {
            if (controls == null) {
                throw new InvalidInputException("CONTROLS_REQUIRED", "controls object is required");
            }
            ControlInput input = controls.get(kind.name().toLowerCase(Locale.ROOT));
            if (input == null) {
                input = controls.get(kind.name());
            }
            if (input == null) {
                throw new InvalidInputException("CONTROL_MISSING", "missing control " + kind);
            }
            return input.toResult(kind);
        }
    }

    public record ControlInput(
            @NotNull ControlStatus status,
            @JsonProperty("reason_codes") List<String> reasonCodes,
            @JsonProperty("safe_explanation") String safeExplanation,
            @JsonProperty("metadata_ref") String metadataRef) {

        ControlResult toResult(ControlKind kind) {
            List<ReasonCode> codes = reasonCodes == null
                    ? List.of()
                    : reasonCodes.stream()
                            .map(DevEvaluateController::knownOrCustom)
                            .toList();
            String explanation = safeExplanation == null ? "Synthetic control result." : safeExplanation;
            return new ControlResult(kind, status, codes, explanation, metadataRef);
        }
    }

    static ReasonCode knownOrCustom(String code) {
        return switch (code) {
            case "IDENTITY_INVALID" -> ReasonCode.IDENTITY_INVALID;
            case "IDENTITY_UNAVAILABLE" -> ReasonCode.IDENTITY_UNAVAILABLE;
            case "AUTHORIZATION_MISSING" -> ReasonCode.AUTHORIZATION_MISSING;
            case "AUTHORIZATION_UNAVAILABLE" -> ReasonCode.AUTHORIZATION_UNAVAILABLE;
            case "AUTHENTICITY_INVALID" -> ReasonCode.AUTHENTICITY_INVALID;
            case "AUTHENTICITY_UNAVAILABLE" -> ReasonCode.AUTHENTICITY_UNAVAILABLE;
            case "FRESHNESS_EXPIRED" -> ReasonCode.FRESHNESS_EXPIRED;
            case "FRESHNESS_UNAVAILABLE" -> ReasonCode.FRESHNESS_UNAVAILABLE;
            case "REPLAY_DETECTED" -> ReasonCode.REPLAY_DETECTED;
            case "REPLAY_UNAVAILABLE" -> ReasonCode.REPLAY_UNAVAILABLE;
            case "CONTRACT_INVALID" -> ReasonCode.CONTRACT_INVALID;
            case "CONTRACT_UNSUPPORTED" -> ReasonCode.CONTRACT_UNSUPPORTED;
            case "CONTRACT_UNAVAILABLE" -> ReasonCode.CONTRACT_UNAVAILABLE;
            case "EGRESS_TRANSFORMATION_REQUIRED" -> ReasonCode.EGRESS_TRANSFORMATION_REQUIRED;
            case "EGRESS_FIELD_DENIED" -> ReasonCode.EGRESS_FIELD_DENIED;
            case "CREDENTIAL_UNAVAILABLE" -> ReasonCode.CREDENTIAL_UNAVAILABLE;
            case "CONNECTOR_UNAVAILABLE" -> ReasonCode.CONNECTOR_UNAVAILABLE;
            default -> ReasonCode.of(code, "Synthetic reason code.");
        };
    }

    public record EvaluateResponse(
            String banner,
            @JsonProperty("transaction_id") String transactionId,
            String outcome,
            @JsonProperty("reason_codes") List<ReasonCodeView> reasonCodes,
            @JsonProperty("applied_controls") Map<ControlKind, ControlStatus> appliedControls,
            @JsonProperty("decided_at") String decidedAt,
            @JsonProperty("policy_version") String policyVersion) {

        static EvaluateResponse from(BoundaryDecision decision) {
            List<ReasonCodeView> reasons = decision.reasonCodes().stream()
                    .map(code -> new ReasonCodeView(code.code(), code.description()))
                    .toList();
            return new EvaluateResponse(
                    "SYNTHETIC DEMO - local Boundary Enforcement Engine only. Not production IAM.",
                    decision.transactionId().value(),
                    decision.outcome().name(),
                    reasons,
                    decision.appliedControls(),
                    decision.decidedAt().toString(),
                    decision.policyVersion().value());
        }
    }

    public record ReasonCodeView(String code, String description) {}
}
