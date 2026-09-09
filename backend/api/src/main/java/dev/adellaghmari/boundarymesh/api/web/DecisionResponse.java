package dev.adellaghmari.boundarymesh.api.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.adellaghmari.boundarymesh.application.crossing.CrossingComposer;
import dev.adellaghmari.boundarymesh.domain.BoundaryDecision;
import dev.adellaghmari.boundarymesh.domain.ControlKind;
import dev.adellaghmari.boundarymesh.domain.ControlResult;
import dev.adellaghmari.boundarymesh.domain.ControlStatus;
import dev.adellaghmari.boundarymesh.domain.ControlVector;
import dev.adellaghmari.boundarymesh.domain.EnforcementTrace;
import dev.adellaghmari.boundarymesh.domain.IngressKind;
import dev.adellaghmari.boundarymesh.domain.contract.ContractEvaluation;
import dev.adellaghmari.boundarymesh.domain.egress.EgressEvaluation;
import dev.adellaghmari.boundarymesh.domain.identity.BoundaryPrincipal;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DecisionResponse(
        String banner,
        @JsonProperty("transaction_id") String transactionId,
        String outcome,
        @JsonProperty("ingress_kind") String ingressKind,
        @JsonProperty("reason_codes") List<ReasonCodeView> reasonCodes,
        @JsonProperty("applied_controls") Map<ControlKind, ControlStatus> appliedControls,
        @JsonProperty("controls") List<ControlView> controls,
        @JsonProperty("decided_at") String decidedAt,
        @JsonProperty("policy_version") String policyVersion,
        @JsonProperty("duplicate_delivery") boolean duplicateDelivery,
        IdentityView identity,
        AuthorizationView authorization,
        ContractView contract,
        EgressView egress,
        @JsonProperty("enforcement_trace") List<TraceStepView> enforcementTrace,
        String classification,
        @JsonProperty("source_payload") String sourcePayload,
        @JsonProperty("transformed_payload") String transformedPayload) {

    public static DecisionResponse from(
            BoundaryDecision decision,
            ControlVector vector,
            IngressKind ingressKind,
            BoundaryPrincipal principal,
            boolean duplicateDelivery) {
        return assemble(decision, vector, ingressKind, principal, duplicateDelivery, null, null, null, null, null);
    }

    public static DecisionResponse fromCrossing(
            CrossingComposer.CrossingResult result,
            IngressKind ingressKind,
            BoundaryPrincipal principal,
            boolean includeDiagnostics) {
        ContractEvaluation contract = result.contract();
        EgressEvaluation egress = result.egress();
        return assemble(
                result.decision(),
                result.vector(),
                ingressKind,
                principal,
                false,
                ContractView.from(contract),
                EgressView.from(egress, includeDiagnostics),
                TraceStepView.from(result.trace()),
                includeDiagnostics ? contract.classification() : null,
                includeDiagnostics ? egress.transformedJson() : null);
    }

    public static DecisionResponse fromCrossing(
            CrossingComposer.CrossingResult result,
            IngressKind ingressKind,
            BoundaryPrincipal principal,
            boolean includeDiagnostics,
            String sourcePayload) {
        DecisionResponse base = fromCrossing(result, ingressKind, principal, includeDiagnostics);
        return new DecisionResponse(
                base.banner(),
                base.transactionId(),
                base.outcome(),
                base.ingressKind(),
                base.reasonCodes(),
                base.appliedControls(),
                base.controls(),
                base.decidedAt(),
                base.policyVersion(),
                base.duplicateDelivery(),
                base.identity(),
                base.authorization(),
                base.contract(),
                base.egress(),
                base.enforcementTrace(),
                base.classification(),
                includeDiagnostics ? sourcePayload : null,
                base.transformedPayload());
    }

    private static DecisionResponse assemble(
            BoundaryDecision decision,
            ControlVector vector,
            IngressKind ingressKind,
            BoundaryPrincipal principal,
            boolean duplicateDelivery,
            ContractView contract,
            EgressView egress,
            List<TraceStepView> trace,
            String classification,
            String transformedPayload) {
        List<ReasonCodeView> reasons = decision.reasonCodes().stream()
                .map(code -> new ReasonCodeView(code.code(), code.description()))
                .toList();
        List<ControlView> controls = vector.asList().stream()
                .map(result -> new ControlView(
                        result.kind().name(),
                        result.status().name(),
                        result.reasonCodes().stream().map(ReasonCodeView::from).toList(),
                        result.safeExplanation()))
                .toList();
        return new DecisionResponse(
                "SYNTHETIC DEMO - local BoundaryMesh security enforcement. Not Entra or Graph.",
                decision.transactionId().value(),
                decision.outcome().name(),
                ingressKind.name(),
                reasons,
                decision.appliedControls(),
                controls,
                decision.decidedAt().toString(),
                decision.policyVersion().value(),
                duplicateDelivery,
                IdentityView.from(vector.identity(), principal),
                AuthorizationView.from(vector.authorization(), principal),
                contract,
                egress,
                trace,
                classification,
                null,
                transformedPayload);
    }

    public record ReasonCodeView(String code, String description) {
        static ReasonCodeView from(dev.adellaghmari.boundarymesh.domain.ReasonCode code) {
            return new ReasonCodeView(code.code(), code.description());
        }
    }

    public record ControlView(String kind, String status, List<ReasonCodeView> reasonCodes, String safeExplanation) {}

    public record IdentityView(
            String status, String principal, String issuer, String method, String keyId, String explanation) {
        static IdentityView from(ControlResult result, BoundaryPrincipal principal) {
            return new IdentityView(
                    result.status().name(),
                    principal == null ? null : principal.id().value(),
                    principal == null ? null : principal.issuer().value(),
                    principal == null ? null : principal.method().name(),
                    principal == null ? null : principal.keyId(),
                    result.safeExplanation());
        }
    }

    public record AuthorizationView(String status, List<String> grantedScopes, String explanation) {
        static AuthorizationView from(ControlResult result, BoundaryPrincipal principal) {
            return new AuthorizationView(
                    result.status().name(),
                    principal == null ? List.of() : List.copyOf(principal.scopes()),
                    result.safeExplanation());
        }
    }

    public record ContractView(
            String id,
            String version,
            String type,
            String status,
            String classification,
            @JsonProperty("truncated_count") int truncatedCount,
            List<ViolationView> violations) {
        static ContractView from(ContractEvaluation evaluation) {
            if (evaluation == null) {
                return null;
            }
            String id = evaluation.reference() == null
                    ? null
                    : evaluation.reference().id().value();
            String version = evaluation.reference() == null
                    ? null
                    : evaluation.reference().version().value();
            String type = evaluation.reference() == null
                    ? null
                    : evaluation.reference().type().name();
            String status = evaluation.reference() == null
                    ? null
                    : evaluation.reference().status().name();
            return new ContractView(
                    id,
                    version,
                    type,
                    status,
                    evaluation.classification(),
                    evaluation.truncatedCount(),
                    evaluation.violations().stream()
                            .map(item -> new ViolationView(
                                    item.jsonPointer(), item.keyword(), item.expectedCategory(), item.actualCategory()))
                            .toList());
        }
    }

    public record ViolationView(String path, String keyword, String expected, String actual) {}

    public record EgressView(
            @JsonProperty("policy_id") String policyId,
            @JsonProperty("policy_version") String policyVersion,
            String destination,
            List<FieldActionView> fields,
            @JsonProperty("redacted_paths") List<String> redactedPaths,
            @JsonProperty("denied_paths") List<String> deniedPaths,
            @JsonProperty("implicit_deny_paths") List<String> implicitDenyPaths,
            @JsonProperty("transformation_count") int transformationCount) {
        static EgressView from(EgressEvaluation evaluation, boolean includeFields) {
            if (evaluation == null) {
                return null;
            }
            return new EgressView(
                    evaluation.policyId().value(),
                    evaluation.policyVersion().value(),
                    evaluation.destination(),
                    includeFields
                            ? evaluation.fieldDecisions().stream()
                                    .map(item -> new FieldActionView(
                                            item.path().pointer(), item.action().name(), item.ruleId(), item.note()))
                                    .toList()
                            : List.of(),
                    evaluation.summary().redactedPaths(),
                    evaluation.summary().deniedPaths(),
                    evaluation.summary().implicitDenyPaths(),
                    evaluation.summary().transformationCount());
        }
    }

    public record FieldActionView(
            String path, String action, @JsonProperty("rule_id") String ruleId, String explanation) {}

    public record TraceStepView(String kind, String status, String explanation, String reference) {
        static List<TraceStepView> from(EnforcementTrace trace) {
            if (trace == null) {
                return List.of();
            }
            return trace.steps().stream()
                    .map(step -> new TraceStepView(
                            step.kind().name(), step.status().name(), step.explanation(), step.reference()))
                    .toList();
        }
    }
}
