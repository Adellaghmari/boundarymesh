package dev.adellaghmari.boundarymesh.application;

import static org.assertj.core.api.Assertions.assertThat;

import dev.adellaghmari.boundarymesh.domain.BoundaryDecision;
import dev.adellaghmari.boundarymesh.domain.BoundaryOutcome;
import dev.adellaghmari.boundarymesh.domain.ControlKind;
import dev.adellaghmari.boundarymesh.domain.ControlResult;
import dev.adellaghmari.boundarymesh.domain.ControlVector;
import dev.adellaghmari.boundarymesh.domain.ReasonCode;
import dev.adellaghmari.boundarymesh.domain.TransactionId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class BoundaryEnforcementEngineTest {

    private static final Instant FIXED = Instant.parse("2026-09-09T15:00:00Z");
    private static final TransactionId TX = TransactionId.of("11111111-2222-3333-4444-555555555555");

    private BoundaryEnforcementEngine engine;

    @BeforeEach
    void setUp() {
        engine = new BoundaryEnforcementEngine(Clock.fixed(FIXED, ZoneOffset.UTC));
    }

    @Test
    void allowWhenRequiredControlsPass() {
        BoundaryDecision decision = engine.decide(TX, ControlVector.allPass());
        assertThat(decision.outcome()).isEqualTo(BoundaryOutcome.ALLOW);
        assertThat(decision.reasonCodes()).isEmpty();
        assertThat(decision.decidedAt()).isEqualTo(FIXED);
        assertThat(decision.transactionId()).isEqualTo(TX);
    }

    @Test
    void rejectOnHardSecurityFailure() {
        ControlVector vector = ControlVector.allPass()
                .withIdentity(ControlResult.fail(
                        ControlKind.IDENTITY, ReasonCode.IDENTITY_INVALID, "Synthetic invalid identity."));
        BoundaryDecision decision = engine.decide(TX, vector);
        assertThat(decision.outcome()).isEqualTo(BoundaryOutcome.REJECT);
        assertThat(decision.reasonCodes()).containsExactly(ReasonCode.IDENTITY_INVALID);
    }

    @Test
    void quarantineOnRecoverableUnavailable() {
        ControlVector vector = ControlVector.allPass()
                .withCredentialResolvability(ControlResult.unavailable(
                        ControlKind.CREDENTIAL_RESOLVABILITY,
                        ReasonCode.CREDENTIAL_UNAVAILABLE,
                        "Synthetic credential reference missing."));
        BoundaryDecision decision = engine.decide(TX, vector);
        assertThat(decision.outcome()).isEqualTo(BoundaryOutcome.QUARANTINE);
        assertThat(decision.reasonCodes()).containsExactly(ReasonCode.CREDENTIAL_UNAVAILABLE);
    }

    @Test
    void allowWithTransformationWhenEgressRequiresIt() {
        ControlVector vector = ControlVector.allPass()
                .withEgress(ControlResult.pass(
                        ControlKind.EGRESS,
                        ReasonCode.EGRESS_TRANSFORMATION_REQUIRED,
                        "Two fields require redaction."));
        BoundaryDecision decision = engine.decide(TX, vector);
        assertThat(decision.outcome()).isEqualTo(BoundaryOutcome.ALLOW_WITH_TRANSFORMATION);
        assertThat(decision.reasonCodes()).containsExactly(ReasonCode.EGRESS_TRANSFORMATION_REQUIRED);
    }

    @Test
    void rejectPrecedenceOverQuarantine() {
        ControlVector vector = ControlVector.allPass()
                .withReplay(ControlResult.fail(ControlKind.REPLAY, ReasonCode.REPLAY_DETECTED, "Synthetic replay."))
                .withConnectorSafety(ControlResult.unavailable(
                        ControlKind.CONNECTOR_SAFETY, ReasonCode.CONNECTOR_UNAVAILABLE, "Circuit open."));
        BoundaryDecision decision = engine.decide(TX, vector);
        assertThat(decision.outcome()).isEqualTo(BoundaryOutcome.REJECT);
        assertThat(decision.reasonCodes()).containsExactly(ReasonCode.REPLAY_DETECTED);
        assertThat(decision.reasonCodes()).doesNotContain(ReasonCode.CONNECTOR_UNAVAILABLE);
    }

    @Test
    void rejectPrecedenceOverTransformation() {
        ControlVector vector = ControlVector.allPass()
                .withAuthorization(ControlResult.fail(
                        ControlKind.AUTHORIZATION, ReasonCode.AUTHORIZATION_MISSING, "Missing app role."))
                .withEgress(ControlResult.pass(
                        ControlKind.EGRESS, ReasonCode.EGRESS_TRANSFORMATION_REQUIRED, "Would redact."));
        BoundaryDecision decision = engine.decide(TX, vector);
        assertThat(decision.outcome()).isEqualTo(BoundaryOutcome.REJECT);
        assertThat(decision.reasonCodes()).containsExactly(ReasonCode.AUTHORIZATION_MISSING);
    }

    @Test
    void quarantinePrecedenceOverTransformation() {
        ControlVector vector = ControlVector.allPass()
                .withContract(ControlResult.fail(
                        ControlKind.CONTRACT, ReasonCode.CONTRACT_UNSUPPORTED, "Schema version held."))
                .withEgress(ControlResult.pass(
                        ControlKind.EGRESS, ReasonCode.EGRESS_TRANSFORMATION_REQUIRED, "Would redact."));
        BoundaryDecision decision = engine.decide(TX, vector);
        assertThat(decision.outcome()).isEqualTo(BoundaryOutcome.QUARANTINE);
        assertThat(decision.reasonCodes()).containsExactly(ReasonCode.CONTRACT_UNSUPPORTED);
    }

    @Test
    void securityUnavailableFailsClosedToReject() {
        ControlVector vector = ControlVector.allPass()
                .withIdentity(ControlResult.unavailable(
                        ControlKind.IDENTITY, ReasonCode.IDENTITY_UNAVAILABLE, "JWKS unavailable (synthetic)."));
        BoundaryDecision decision = engine.decide(TX, vector);
        assertThat(decision.outcome()).isEqualTo(BoundaryOutcome.REJECT);
        assertThat(decision.reasonCodes()).containsExactly(ReasonCode.IDENTITY_UNAVAILABLE);
    }

    @Test
    void contractDriftQuarantinesEvenWhenEgressWouldTransform() {
        ControlVector vector = ControlVector.allPass()
                .withContract(ControlResult.fail(
                        ControlKind.CONTRACT, ReasonCode.CONTRACT_DRIFT_DETECTED, "Declared v1 matches v2."))
                .withEgress(
                        ControlResult.pass(ControlKind.EGRESS, ReasonCode.EGRESS_REDACTION_REQUIRED, "Would redact."));
        assertThat(engine.decide(TX, vector).outcome()).isEqualTo(BoundaryOutcome.QUARANTINE);
    }

    @Test
    void authenticityFailureDominatesContractQuarantine() {
        ControlVector vector = ControlVector.allPass()
                .withAuthenticity(ControlResult.fail(
                        ControlKind.AUTHENTICITY, ReasonCode.AUTHENTICITY_INVALID_SIGNATURE, "Bad HMAC."))
                .withContract(ControlResult.fail(
                        ControlKind.CONTRACT, ReasonCode.CONTRACT_DRIFT_DETECTED, "Would quarantine."));
        BoundaryDecision decision = engine.decide(TX, vector);
        assertThat(decision.outcome()).isEqualTo(BoundaryOutcome.REJECT);
        assertThat(decision.reasonCodes()).containsExactly(ReasonCode.AUTHENTICITY_INVALID_SIGNATURE);
    }

    @Test
    void schemaInvalidRejectsWhileUnsupportedQuarantines() {
        assertThat(engine.decide(
                                TX,
                                ControlVector.allPass()
                                        .withContract(ControlResult.fail(
                                                ControlKind.CONTRACT,
                                                ReasonCode.CONTRACT_SCHEMA_INVALID,
                                                "Missing required field.")))
                        .outcome())
                .isEqualTo(BoundaryOutcome.REJECT);
        assertThat(engine.decide(
                                TX,
                                ControlVector.allPass()
                                        .withContract(ControlResult.fail(
                                                ControlKind.CONTRACT,
                                                ReasonCode.CONTRACT_VERSION_UNSUPPORTED,
                                                "v3 is unsupported.")))
                        .outcome())
                .isEqualTo(BoundaryOutcome.QUARANTINE);
    }

    @Test
    void egressUnavailableQuarantinesAndRedactionTransforms() {
        assertThat(engine.decide(
                                TX,
                                ControlVector.allPass()
                                        .withEgress(ControlResult.unavailable(
                                                ControlKind.EGRESS,
                                                ReasonCode.EGRESS_POLICY_UNAVAILABLE,
                                                "No policy.")))
                        .outcome())
                .isEqualTo(BoundaryOutcome.QUARANTINE);
        assertThat(engine.decide(
                                TX,
                                ControlVector.allPass()
                                        .withEgress(ControlResult.pass(
                                                ControlKind.EGRESS,
                                                ReasonCode.EGRESS_REDACTION_REQUIRED,
                                                "Email redacted.")))
                        .outcome())
                .isEqualTo(BoundaryOutcome.ALLOW_WITH_TRANSFORMATION);
    }

    @Test
    void usesInjectedClock() {
        Instant later = Instant.parse("2026-12-01T00:00:00Z");
        BoundaryEnforcementEngine laterEngine = new BoundaryEnforcementEngine(Clock.fixed(later, ZoneOffset.UTC));
        assertThat(laterEngine.decide(TX, ControlVector.allPass()).decidedAt()).isEqualTo(later);
        assertThat(engine.decide(TX, ControlVector.allPass()).decidedAt()).isEqualTo(FIXED);
    }

    @Test
    void preservesMultipleRejectReasonCodes() {
        ControlVector vector = ControlVector.allPass()
                .withIdentity(ControlResult.fail(ControlKind.IDENTITY, ReasonCode.IDENTITY_INVALID, "Bad identity."))
                .withAuthenticity(ControlResult.fail(
                        ControlKind.AUTHENTICITY, ReasonCode.AUTHENTICITY_INVALID, "Bad signature."));
        BoundaryDecision decision = engine.decide(TX, vector);
        assertThat(decision.reasonCodes())
                .containsExactly(ReasonCode.IDENTITY_INVALID, ReasonCode.AUTHENTICITY_INVALID);
    }

    @ParameterizedTest
    @MethodSource("mixedVectors")
    void precedenceAcrossCombinations(ControlVector vector, BoundaryOutcome expected, ReasonCode firstReason) {
        BoundaryDecision decision = engine.decide(TX, vector);
        assertThat(decision.outcome()).isEqualTo(expected);
        if (expected == BoundaryOutcome.ALLOW) {
            assertThat(decision.reasonCodes()).isEmpty();
        } else {
            assertThat(decision.reasonCodes().getFirst()).isEqualTo(firstReason);
        }
    }

    static Stream<Arguments> mixedVectors() {
        ControlVector denyEgress = ControlVector.allPass()
                .withEgress(ControlResult.fail(ControlKind.EGRESS, ReasonCode.EGRESS_FIELD_DENIED, "Forbidden field."));
        ControlVector invalidContract = ControlVector.allPass()
                .withContract(
                        ControlResult.fail(ControlKind.CONTRACT, ReasonCode.CONTRACT_INVALID, "Malformed schema."));
        ControlVector connectorDown = ControlVector.allPass()
                .withConnectorSafety(ControlResult.unavailable(
                        ControlKind.CONNECTOR_SAFETY, ReasonCode.CONNECTOR_UNAVAILABLE, "Open circuit."));
        ControlVector freshnessNa =
                ControlVector.allPass().withFreshness(ControlResult.notApplicable(ControlKind.FRESHNESS));
        return Stream.of(
                Arguments.of(ControlVector.allPass(), BoundaryOutcome.ALLOW, null),
                Arguments.of(denyEgress, BoundaryOutcome.REJECT, ReasonCode.EGRESS_FIELD_DENIED),
                Arguments.of(invalidContract, BoundaryOutcome.REJECT, ReasonCode.CONTRACT_INVALID),
                Arguments.of(connectorDown, BoundaryOutcome.QUARANTINE, ReasonCode.CONNECTOR_UNAVAILABLE),
                Arguments.of(freshnessNa, BoundaryOutcome.ALLOW, null));
    }
}
