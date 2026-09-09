package dev.adellaghmari.boundarymesh.api.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.adellaghmari.boundarymesh.domain.BoundaryDecision;
import dev.adellaghmari.boundarymesh.domain.ControlKind;
import dev.adellaghmari.boundarymesh.domain.ControlResult;
import dev.adellaghmari.boundarymesh.domain.ControlStatus;
import dev.adellaghmari.boundarymesh.domain.ControlVector;
import dev.adellaghmari.boundarymesh.domain.IngressKind;
import dev.adellaghmari.boundarymesh.domain.identity.BoundaryPrincipal;
import java.util.List;
import java.util.Map;

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
        AuthorizationView authorization) {

    public static DecisionResponse from(
            BoundaryDecision decision,
            ControlVector vector,
            IngressKind ingressKind,
            BoundaryPrincipal principal,
            boolean duplicateDelivery) {
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
                AuthorizationView.from(vector.authorization(), principal));
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
}
