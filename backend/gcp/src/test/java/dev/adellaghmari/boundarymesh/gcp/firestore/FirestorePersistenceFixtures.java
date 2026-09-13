package dev.adellaghmari.boundarymesh.gcp.firestore;

import dev.adellaghmari.boundarymesh.application.graph.ObservedCrossingStore;
import dev.adellaghmari.boundarymesh.domain.BoundaryDecision;
import dev.adellaghmari.boundarymesh.domain.BoundaryOutcome;
import dev.adellaghmari.boundarymesh.domain.ControlKind;
import dev.adellaghmari.boundarymesh.domain.ControlStatus;
import dev.adellaghmari.boundarymesh.domain.PolicyVersion;
import dev.adellaghmari.boundarymesh.domain.ReasonCode;
import dev.adellaghmari.boundarymesh.domain.TransactionId;
import dev.adellaghmari.boundarymesh.domain.attestation.AttestationId;
import dev.adellaghmari.boundarymesh.domain.attestation.AttestationSignature;
import dev.adellaghmari.boundarymesh.domain.attestation.AttestationStatus;
import dev.adellaghmari.boundarymesh.domain.attestation.AttestationVersion;
import dev.adellaghmari.boundarymesh.domain.attestation.ExchangeAttestation;
import dev.adellaghmari.boundarymesh.domain.attestation.SignerKeyReference;
import dev.adellaghmari.boundarymesh.domain.audit.AuditChainId;
import dev.adellaghmari.boundarymesh.domain.audit.AuditHash;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryResult;
import dev.adellaghmari.boundarymesh.domain.quarantine.QuarantineId;
import dev.adellaghmari.boundarymesh.domain.quarantine.QuarantineItem;
import dev.adellaghmari.boundarymesh.domain.quarantine.QuarantineReason;
import dev.adellaghmari.boundarymesh.domain.quarantine.QuarantineStatus;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Clearly fake values for emulator tests. No tokens, secrets, or ADC material. */
final class FirestorePersistenceFixtures {

    static final Instant FIXED = Instant.parse("2026-09-13T10:00:00Z");

    private FirestorePersistenceFixtures() {}

    static BoundaryDecision decision(BoundaryOutcome outcome) {
        EnumMap<ControlKind, ControlStatus> controls = new EnumMap<>(ControlKind.class);
        for (ControlKind kind : ControlKind.values()) {
            controls.put(kind, ControlStatus.NOT_APPLICABLE);
        }
        List<ReasonCode> reasons =
                outcome == BoundaryOutcome.ALLOW ? List.of() : List.of(ReasonCode.PROVIDER_TEMPORARY_FAILURE);
        return new BoundaryDecision(TransactionId.random(), outcome, reasons, controls, FIXED, PolicyVersion.PHASE_1);
    }

    static DeliveryResult deliveryResult(String explanation) {
        return DeliveryResult.notAttempted(explanation);
    }

    static ExchangeAttestation attestation(String integrationId) {
        return attestation(AttestationId.random().value(), integrationId);
    }

    static ExchangeAttestation attestation(String attestationId, String integrationId) {
        return new ExchangeAttestation(
                new AttestationId(attestationId),
                AttestationVersion.V1,
                TransactionId.random().value(),
                integrationId,
                "QUAYFORGE",
                "COVECRM",
                FIXED,
                "fake-principal-ref-15fa",
                "none",
                Map.of("identity", "PASS"),
                "quayforge-orders/v1",
                "1",
                "PASS",
                "egress-1",
                0,
                "covecrm-api:v1",
                "GRANTED",
                "covecrm",
                "NOT_ATTEMPTED",
                BoundaryOutcome.QUARANTINE,
                new AuditChainId(integrationId),
                1L,
                AuditHash.GENESIS,
                new SignerKeyReference(
                        SignerKeyReference.LOCAL_KEY_ID, "fake-emulator-1", SignerKeyReference.ALGORITHM),
                AttestationStatus.SIGNED,
                new AttestationSignature("ZmFrZS1hdHRlc3RhdGlvbi1zaWduYXR1cmUtbm90LWEtdG9rZW4"));
    }

    static QuarantineItem quarantine(String integrationId) {
        return quarantine(QuarantineId.random().value(), integrationId);
    }

    static QuarantineItem quarantine(String quarantineId, String integrationId) {
        return new QuarantineItem(
                new QuarantineId(quarantineId),
                TransactionId.random(),
                integrationId,
                "QUAYFORGE",
                "COVECRM",
                QuarantineReason.PROVIDER_TEMPORARY_FAILURE,
                ReasonCode.PROVIDER_TEMPORARY_FAILURE,
                QuarantineStatus.OPEN,
                0,
                QuarantineItem.DEFAULT_MAX_RECOVERY,
                true,
                "quayforge-orders/v1",
                "covecrm",
                "covecrm-api:v1",
                "CLOSED",
                FIXED,
                FIXED,
                List.of());
    }

    static ObservedCrossingStore.Observation observation(String integrationId, String destination) {
        return new ObservedCrossingStore.Observation(
                integrationId,
                "QUAYFORGE",
                destination,
                "QUARANTINE",
                "NOT_ATTEMPTED",
                integrationId,
                "fake-attestation-15fa",
                true,
                FIXED);
    }
}
