package dev.adellaghmari.boundarymesh.application.attestation;

import static org.assertj.core.api.Assertions.assertThat;

import dev.adellaghmari.boundarymesh.application.canon.Rfc8785Canonicalizer;
import dev.adellaghmari.boundarymesh.application.consent.ConsentEvaluation;
import dev.adellaghmari.boundarymesh.application.crossing.CrossingComposer.CrossingResult;
import dev.adellaghmari.boundarymesh.domain.BoundaryDecision;
import dev.adellaghmari.boundarymesh.domain.BoundaryOutcome;
import dev.adellaghmari.boundarymesh.domain.ControlKind;
import dev.adellaghmari.boundarymesh.domain.ControlResult;
import dev.adellaghmari.boundarymesh.domain.ControlVector;
import dev.adellaghmari.boundarymesh.domain.EnforcementTrace;
import dev.adellaghmari.boundarymesh.domain.PolicyVersion;
import dev.adellaghmari.boundarymesh.domain.ReasonCode;
import dev.adellaghmari.boundarymesh.domain.TransactionId;
import dev.adellaghmari.boundarymesh.domain.attestation.AttestationSignature;
import dev.adellaghmari.boundarymesh.domain.attestation.AttestationStatus;
import dev.adellaghmari.boundarymesh.domain.attestation.ExchangeAttestation;
import dev.adellaghmari.boundarymesh.domain.attestation.SignatureVerificationStatus;
import dev.adellaghmari.boundarymesh.domain.attestation.SignerKeyReference;
import dev.adellaghmari.boundarymesh.domain.audit.AuditChainHead;
import dev.adellaghmari.boundarymesh.domain.audit.AuditChainId;
import dev.adellaghmari.boundarymesh.domain.audit.AuditHash;
import dev.adellaghmari.boundarymesh.domain.consent.ConsentStatus;
import dev.adellaghmari.boundarymesh.domain.contract.ContractEvaluation;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryResult;
import dev.adellaghmari.boundarymesh.domain.egress.EgressEvaluation;
import dev.adellaghmari.boundarymesh.domain.egress.EgressPolicyId;
import dev.adellaghmari.boundarymesh.domain.egress.EgressPolicyVersion;
import dev.adellaghmari.boundarymesh.domain.egress.RedactionSummary;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExchangeAttestationTest {

    private static final Instant FIXED = Instant.parse("2026-09-10T12:00:00Z");
    private JcaP256AttestationCrypto crypto;
    private AttestationFactory factory;
    private AttestationService service;

    @BeforeEach
    void setup() {
        crypto = new JcaP256AttestationCrypto();
        factory = new AttestationFactory(Clock.fixed(FIXED, ZoneOffset.UTC), crypto);
        service = new AttestationService(new InMemoryAttestationRepository(), crypto);
    }

    @Test
    void signedAttestationVerifies() {
        ExchangeAttestation attestation = signed();
        assertThat(attestation.status()).isEqualTo(AttestationStatus.SIGNED);
        assertThat(attestation.signature().present()).isTrue();
        assertThat(service.verify(attestation).status()).isEqualTo(SignatureVerificationStatus.VALID);
        assertThat(attestation.auditChainId().value()).isEqualTo("quayforge-orders");
        assertThat(new String(AttestationCanonicalizer.bytes(attestation), StandardCharsets.UTF_8))
                .doesNotContain("TEST-SECRET");
        assertThat(attestation.signature().base64()).doesNotContain("BEGIN PRIVATE");
    }

    @Test
    void propertyOrderDoesNotChangeCanonicalBytes() {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("z", "last");
        a.put("a", "first");
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("a", "first");
        b.put("z", "last");
        assertThat(Rfc8785Canonicalizer.canonicalize(a)).isEqualTo(Rfc8785Canonicalizer.canonicalize(b));
    }

    @Test
    void changedOutcomeInvalidatesSignature() {
        assertThat(service.verify(replaceOutcome(signed(), BoundaryOutcome.REJECT))
                        .status())
                .isEqualTo(SignatureVerificationStatus.INVALID);
    }

    @Test
    void changedDestinationInvalidatesSignature() {
        ExchangeAttestation original = signed();
        ExchangeAttestation mutated = copy(
                original,
                original.sourceSystem(),
                "TAMPERED",
                original.contractVersion(),
                original.auditHead(),
                original.signature());
        assertThat(service.verify(mutated).status()).isEqualTo(SignatureVerificationStatus.INVALID);
    }

    @Test
    void changedPolicyVersionInvalidatesSignature() {
        ExchangeAttestation original = signed();
        ExchangeAttestation mutated = copy(
                original,
                original.sourceSystem(),
                original.destinationSystem(),
                "tampered-v9",
                original.auditHead(),
                original.signature());
        assertThat(service.verify(mutated).status()).isEqualTo(SignatureVerificationStatus.INVALID);
    }

    @Test
    void changedAuditHeadInvalidatesSignature() {
        ExchangeAttestation original = signed();
        ExchangeAttestation mutated = copy(
                original,
                original.sourceSystem(),
                original.destinationSystem(),
                original.contractVersion(),
                new AuditHash("a".repeat(64)),
                original.signature());
        assertThat(service.verify(mutated).status()).isEqualTo(SignatureVerificationStatus.INVALID);
    }

    @Test
    void corruptedSignatureIsInvalid() {
        ExchangeAttestation original = signed();
        assertThat(service.verify(original.signed(new AttestationSignature("AAAA")))
                        .status())
                .isEqualTo(SignatureVerificationStatus.INVALID);
    }

    @Test
    void wrongKeyIsInvalid() {
        ExchangeAttestation original = signed();
        AttestationService other =
                new AttestationService(new InMemoryAttestationRepository(), new JcaP256AttestationCrypto());
        assertThat(other.verify(original).status()).isEqualTo(SignatureVerificationStatus.INVALID);
    }

    @Test
    void unsignedIsExplicitAndNotInvalid() {
        AttestationFactory failing =
                new AttestationFactory(Clock.fixed(FIXED, ZoneOffset.UTC), new AttestationSigner() {
                    @Override
                    public SignerKeyReference key() {
                        return new SignerKeyReference("local-jca-p256", "unavailable", "ES256");
                    }

                    @Override
                    public AttestationSignature sign(byte[] canonicalBytes) {
                        throw new IllegalStateException("signer unavailable");
                    }
                });
        ExchangeAttestation unsigned = failing.create(
                crossing(),
                "QUAYFORGE",
                "COVECRM",
                "principal",
                "JWT_BEARER",
                DeliveryResult.notAttempted("n/a"),
                new AuditChainId("quayforge-orders"),
                AuditChainHead.empty(new AuditChainId("quayforge-orders")));
        assertThat(unsigned.status()).isEqualTo(AttestationStatus.UNSIGNED);
        assertThat(unsigned.signature().present()).isFalse();
        assertThat(service.verify(unsigned).status()).isEqualTo(SignatureVerificationStatus.UNSIGNED);
    }

    private ExchangeAttestation signed() {
        return factory.create(
                crossing(),
                "QUAYFORGE",
                "COVECRM",
                "velora-quayforge-connector",
                "JWT_BEARER",
                DeliveryResult.notAttempted("delivered-in-test"),
                new AuditChainId("quayforge-orders"),
                new AuditChainHead(new AuditChainId("quayforge-orders"), 3, new AuditHash("b".repeat(64))));
    }

    private static ExchangeAttestation replaceOutcome(ExchangeAttestation original, BoundaryOutcome outcome) {
        return new ExchangeAttestation(
                original.id(),
                original.schemaVersion(),
                original.transactionId(),
                original.integrationId(),
                original.sourceSystem(),
                original.destinationSystem(),
                original.occurredAt(),
                original.principalReference(),
                original.authenticationMethod(),
                original.controlResults(),
                original.contractId(),
                original.contractVersion(),
                original.contractResult(),
                original.egressPolicyId(),
                original.transformationCount(),
                original.credentialReference(),
                original.consentResult(),
                original.connectorRoute(),
                original.deliveryResult(),
                outcome,
                original.auditChainId(),
                original.auditSequence(),
                original.auditHead(),
                original.signer(),
                original.status(),
                original.signature());
    }

    private static ExchangeAttestation copy(
            ExchangeAttestation original,
            String source,
            String destination,
            String contractVersion,
            AuditHash auditHead,
            AttestationSignature signature) {
        return new ExchangeAttestation(
                original.id(),
                original.schemaVersion(),
                original.transactionId(),
                original.integrationId(),
                source,
                destination,
                original.occurredAt(),
                original.principalReference(),
                original.authenticationMethod(),
                original.controlResults(),
                original.contractId(),
                contractVersion,
                original.contractResult(),
                original.egressPolicyId(),
                original.transformationCount(),
                original.credentialReference(),
                original.consentResult(),
                original.connectorRoute(),
                original.deliveryResult(),
                original.boundaryOutcome(),
                original.auditChainId(),
                original.auditSequence(),
                auditHead,
                original.signer(),
                original.status(),
                signature);
    }

    private static CrossingResult crossing() {
        ControlVector vector = ControlVector.allPass();
        BoundaryDecision decision = new BoundaryDecision(
                TransactionId.of("11111111-2222-3333-4444-555555555555"),
                BoundaryOutcome.ALLOW_WITH_TRANSFORMATION,
                List.of(ReasonCode.EGRESS_REDACTION_REQUIRED),
                BoundaryDecision.summaryOf(vector),
                FIXED,
                PolicyVersion.PHASE_10);
        return new CrossingResult(
                decision,
                vector,
                new ContractEvaluation(null, ControlResult.pass(ControlKind.CONTRACT), List.of(), 0, "VALID"),
                new EgressEvaluation(
                        new EgressPolicyId("covecrm"),
                        new EgressPolicyVersion("v1"),
                        "COVECRM",
                        ControlResult.pass(ControlKind.EGRESS),
                        List.of(),
                        new RedactionSummary(List.of(), List.of(), List.of(), List.of(), 2),
                        "{}"),
                new ConsentEvaluation(ConsentStatus.GRANTED, null, "ok", null),
                null,
                EnforcementTrace.from(vector));
    }
}
