package dev.adellaghmari.boundarymesh.application.egress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.adellaghmari.boundarymesh.domain.ControlStatus;
import dev.adellaghmari.boundarymesh.domain.ReasonCode;
import dev.adellaghmari.boundarymesh.domain.egress.DenyMode;
import dev.adellaghmari.boundarymesh.domain.egress.EgressEvaluation;
import dev.adellaghmari.boundarymesh.domain.egress.EgressPolicy;
import dev.adellaghmari.boundarymesh.domain.egress.EgressPolicyId;
import dev.adellaghmari.boundarymesh.domain.egress.EgressPolicyVersion;
import dev.adellaghmari.boundarymesh.domain.egress.FieldAction;
import dev.adellaghmari.boundarymesh.domain.egress.FieldPath;
import dev.adellaghmari.boundarymesh.domain.egress.FieldRule;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EgressEvaluatorTest {

    private static final String SOURCE =
            """
            {"customer_id":"cust-9","email":"ada.lovelace@example.test","shipping_address":"1 Synthetic Wharf",\
            "internal_risk_score":99,"legacy_region":"EU-SYNTHETIC","customer":{"email":"ada.lovelace@example.test"}}
            """;

    @Test
    void allowsOnlyPermittedFields() {
        EgressEvaluator evaluator = evaluator(policy("COVECRM", FieldAction.ALLOW, DenyMode.REJECT, false));
        EgressEvaluation evaluation =
                evaluator.evaluate("COVECRM", "{\"customer_id\":\"cust-9\",\"email\":\"ada.lovelace@example.test\"}");
        assertThat(evaluation.control().status()).isEqualTo(ControlStatus.PASS);
        assertThat(evaluation.control().reasonCodes()).isEmpty();
        assertThat(evaluation.transformedJson()).contains("cust-9");
        assertThat(evaluation.transformedJson()).contains("ada.lovelace@example.test");
    }

    @Test
    void redactsWithoutCopyingValuesIntoEvidence() {
        EgressEvaluator evaluator = evaluator(policy("RIVERTIDE", FieldAction.REDACT, DenyMode.STRIP, false));
        String original = SOURCE;
        EgressEvaluation evaluation = evaluator.evaluate("RIVERTIDE", original);
        assertThat(evaluation.control().reasonCodes()).contains(ReasonCode.EGRESS_REDACTION_REQUIRED);
        assertThat(evaluation.transformedJson()).contains("[REDACTED]");
        assertThat(evaluation.transformedJson()).doesNotContain("ada.lovelace@example.test");
        assertThat(evaluation.fieldDecisions().toString()).doesNotContain("ada.lovelace@example.test");
        assertThat(original).contains("ada.lovelace@example.test");
    }

    @Test
    void criticalDenyRejectsWithoutOutboundBody() {
        EgressEvaluator evaluator = evaluator(denyPolicy());
        EgressEvaluation evaluation = evaluator.evaluate("RIVERTIDE", SOURCE);
        assertThat(evaluation.control().status()).isEqualTo(ControlStatus.FAIL);
        assertThat(evaluation.control().reasonCodes()).contains(ReasonCode.EGRESS_FIELD_DENIED);
        assertThat(evaluation.transformedJson()).isNull();
        assertThat(evaluation.fieldDecisions().toString()).doesNotContain("99");
    }

    @Test
    void implicitDenyOmitsUnlistedFields() {
        EgressEvaluator evaluator = evaluator(policy("LUMENWAY", FieldAction.ALLOW, DenyMode.STRIP, true));
        EgressEvaluation evaluation =
                evaluator.evaluate("LUMENWAY", "{\"customer_id\":\"cust-9\",\"legacy_region\":\"EU-SYNTHETIC\"}");
        assertThat(evaluation.summary().implicitDenyPaths()).contains("/legacy_region");
        assertThat(evaluation.transformedJson()).contains("cust-9");
        assertThat(evaluation.transformedJson()).doesNotContain("EU-SYNTHETIC");
    }

    @Test
    void nestedPathIsDeterministic() {
        EgressPolicy policy = new EgressPolicy(
                new EgressPolicyId("nested-policy"),
                new EgressPolicyVersion("v1"),
                "RIVERTIDE",
                true,
                List.of(
                        new FieldRule("EGRESS-N-001", FieldPath.parse("/customer"), FieldAction.ALLOW, null),
                        new FieldRule("EGRESS-N-002", FieldPath.parse("/customer/email"), FieldAction.REDACT, null)));
        EgressEvaluation evaluation = new EgressEvaluator(dest -> Optional.of(policy))
                .evaluate("RIVERTIDE", "{\"customer\":{\"email\":\"ada.lovelace@example.test\"}}");
        assertThat(evaluation.transformedJson()).contains("[REDACTED]");
        assertThat(evaluation.transformedJson()).doesNotContain("ada.lovelace@example.test");
        assertThat(evaluation.summary().redactedPaths()).contains("/customer/email");
    }

    @Test
    void unavailableDestinationQuarantinesViaControlStatus() {
        EgressEvaluation evaluation = new EgressEvaluator(dest -> Optional.empty()).evaluate("NOWHERE", "{}");
        assertThat(evaluation.control().status()).isEqualTo(ControlStatus.UNAVAILABLE);
        assertThat(evaluation.control().reasonCodes()).contains(ReasonCode.EGRESS_POLICY_UNAVAILABLE);
    }

    @Test
    void conflictingRulesFailClosedAtConstruction() {
        assertThatThrownBy(() -> new EgressPolicy(
                        new EgressPolicyId("bad-policy"),
                        new EgressPolicyVersion("v1"),
                        "RIVERTIDE",
                        true,
                        List.of(
                                new FieldRule("A", FieldPath.parse("/email"), FieldAction.ALLOW, null),
                                new FieldRule("B", FieldPath.parse("/email"), FieldAction.DENY, DenyMode.REJECT))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("conflicting");
    }

    @Test
    void policyVersionIsReturned() {
        EgressEvaluation evaluation = evaluator(policy("RIVERTIDE", FieldAction.ALLOW, DenyMode.STRIP, false))
                .evaluate("RIVERTIDE", "{}");
        assertThat(evaluation.policyId().value()).isEqualTo("rivertide-test");
        assertThat(evaluation.policyVersion().value()).isEqualTo("v1");
    }

    private static EgressEvaluator evaluator(EgressPolicy policy) {
        return new EgressEvaluator(
                dest -> Optional.ofNullable(Map.of(policy.destination(), policy).get(dest)));
    }

    private static EgressPolicy policy(
            String destination, FieldAction emailAction, DenyMode denyMode, boolean implicit) {
        return new EgressPolicy(
                new EgressPolicyId(destination.equals("RIVERTIDE") ? "rivertide-test" : "dest-test"),
                new EgressPolicyVersion("v1"),
                destination,
                implicit,
                List.of(
                        new FieldRule("EGRESS-001", FieldPath.parse("/customer_id"), FieldAction.ALLOW, null),
                        new FieldRule("EGRESS-002", FieldPath.parse("/email"), emailAction, denyMode)));
    }

    private static EgressPolicy denyPolicy() {
        return new EgressPolicy(
                new EgressPolicyId("rivertide-test"),
                new EgressPolicyVersion("v1"),
                "RIVERTIDE",
                true,
                List.of(
                        new FieldRule("EGRESS-001", FieldPath.parse("/customer_id"), FieldAction.ALLOW, null),
                        new FieldRule(
                                "EGRESS-013",
                                FieldPath.parse("/internal_risk_score"),
                                FieldAction.DENY,
                                DenyMode.REJECT)));
    }
}
