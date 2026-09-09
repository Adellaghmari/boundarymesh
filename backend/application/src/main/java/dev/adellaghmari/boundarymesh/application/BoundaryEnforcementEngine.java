package dev.adellaghmari.boundarymesh.application;

import dev.adellaghmari.boundarymesh.domain.BoundaryDecision;
import dev.adellaghmari.boundarymesh.domain.BoundaryOutcome;
import dev.adellaghmari.boundarymesh.domain.ControlKind;
import dev.adellaghmari.boundarymesh.domain.ControlResult;
import dev.adellaghmari.boundarymesh.domain.ControlStatus;
import dev.adellaghmari.boundarymesh.domain.ControlVector;
import dev.adellaghmari.boundarymesh.domain.PolicyVersion;
import dev.adellaghmari.boundarymesh.domain.ReasonCode;
import dev.adellaghmari.boundarymesh.domain.TransactionId;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Phase 1 foundation: typed input, deterministic precedence, typed output. Not the full later
 * security policy. Testable without Spring.
 *
 * <p>Precedence exists because a security rejection must dominate a recoverable hold, and a hold
 * must dominate a transformation that would otherwise allow the crossing:
 *
 * <pre>
 * REJECT &gt; QUARANTINE &gt; ALLOW_WITH_TRANSFORMATION &gt; ALLOW
 * </pre>
 *
 * An invalid identity cannot be overridden by a required redaction. A required security control
 * that is {@code UNAVAILABLE} fails closed to {@code REJECT}, never {@code ALLOW}.
 */
public final class BoundaryEnforcementEngine {

    private final Clock clock;
    private final PolicyVersion policyVersion;

    public BoundaryEnforcementEngine(Clock clock) {
        this(clock, PolicyVersion.PHASE_1);
    }

    public BoundaryEnforcementEngine(Clock clock, PolicyVersion policyVersion) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.policyVersion = Objects.requireNonNull(policyVersion, "policyVersion");
    }

    public BoundaryDecision decide(TransactionId transactionId, ControlVector vector) {
        Objects.requireNonNull(transactionId, "transactionId");
        Objects.requireNonNull(vector, "vector");

        List<ReasonCode> rejectReasons = new ArrayList<>();
        List<ReasonCode> quarantineReasons = new ArrayList<>();
        List<ReasonCode> transformReasons = new ArrayList<>();

        collectSecurity(vector, rejectReasons);
        collectContract(vector.contract(), rejectReasons, quarantineReasons);
        collectEgress(vector.egress(), rejectReasons, quarantineReasons, transformReasons);
        collectOperational(vector.credentialResolvability(), quarantineReasons);
        collectOperational(vector.connectorSafety(), quarantineReasons);

        BoundaryOutcome outcome;
        List<ReasonCode> reasons;
        if (!rejectReasons.isEmpty()) {
            outcome = BoundaryOutcome.REJECT;
            reasons = List.copyOf(rejectReasons);
        } else if (!quarantineReasons.isEmpty()) {
            outcome = BoundaryOutcome.QUARANTINE;
            reasons = List.copyOf(quarantineReasons);
        } else if (!transformReasons.isEmpty()) {
            outcome = BoundaryOutcome.ALLOW_WITH_TRANSFORMATION;
            reasons = List.copyOf(transformReasons);
        } else {
            outcome = BoundaryOutcome.ALLOW;
            reasons = List.of();
        }

        return new BoundaryDecision(
                transactionId, outcome, reasons, BoundaryDecision.summaryOf(vector), clock.instant(), policyVersion);
    }

    private static void collectSecurity(ControlVector vector, List<ReasonCode> rejectReasons) {
        for (ControlResult result : vector.securityControls()) {
            if (isRequiredIdentityControl(result.kind()) && result.status() == ControlStatus.NOT_APPLICABLE) {
                rejectReasons.add(
                        result.kind() == ControlKind.IDENTITY
                                ? ReasonCode.IDENTITY_INVALID
                                : ReasonCode.AUTHORIZATION_MISSING);
                continue;
            }
            if (result.status() == ControlStatus.FAIL || result.status() == ControlStatus.UNAVAILABLE) {
                rejectReasons.addAll(result.reasonCodes());
            }
        }
    }

    private static boolean isRequiredIdentityControl(ControlKind kind) {
        return kind == ControlKind.IDENTITY || kind == ControlKind.AUTHORIZATION;
    }

    private static void collectContract(
            ControlResult contract, List<ReasonCode> rejectReasons, List<ReasonCode> quarantineReasons) {
        if (contract.status() == ControlStatus.UNAVAILABLE) {
            quarantineReasons.addAll(contract.reasonCodes());
            return;
        }
        if (contract.status() != ControlStatus.FAIL) {
            return;
        }
        if (containsAny(
                contract,
                ReasonCode.CONTRACT_UNSUPPORTED,
                ReasonCode.CONTRACT_VERSION_UNSUPPORTED,
                ReasonCode.CONTRACT_DRIFT_DETECTED)) {
            quarantineReasons.addAll(contract.reasonCodes());
        } else {
            rejectReasons.addAll(contract.reasonCodes());
        }
    }

    private static void collectEgress(
            ControlResult egress,
            List<ReasonCode> rejectReasons,
            List<ReasonCode> quarantineReasons,
            List<ReasonCode> transformReasons) {
        if (egress.status() == ControlStatus.UNAVAILABLE) {
            quarantineReasons.addAll(egress.reasonCodes());
            return;
        }
        if (egress.status() == ControlStatus.FAIL) {
            rejectReasons.addAll(egress.reasonCodes());
            return;
        }
        if (egress.status() == ControlStatus.PASS
                && (contains(egress, ReasonCode.EGRESS_TRANSFORMATION_REQUIRED)
                        || contains(egress, ReasonCode.EGRESS_REDACTION_REQUIRED))) {
            transformReasons.addAll(egress.reasonCodes());
        }
    }

    private static void collectOperational(ControlResult result, List<ReasonCode> quarantineReasons) {
        if (result.status() == ControlStatus.FAIL || result.status() == ControlStatus.UNAVAILABLE) {
            quarantineReasons.addAll(result.reasonCodes());
        }
    }

    private static boolean contains(ControlResult result, ReasonCode expected) {
        return result.reasonCodes().stream().anyMatch(code -> code.code().equals(expected.code()));
    }

    private static boolean containsAny(ControlResult result, ReasonCode... expected) {
        for (ReasonCode code : expected) {
            if (contains(result, code)) {
                return true;
            }
        }
        return false;
    }
}
