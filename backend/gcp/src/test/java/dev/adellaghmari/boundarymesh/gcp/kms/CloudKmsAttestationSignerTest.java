package dev.adellaghmari.boundarymesh.gcp.kms;

import static org.assertj.core.api.Assertions.assertThat;

import dev.adellaghmari.boundarymesh.application.attestation.AttestationFactory;
import dev.adellaghmari.boundarymesh.application.attestation.AttestationService;
import dev.adellaghmari.boundarymesh.application.attestation.InMemoryAttestationRepository;
import dev.adellaghmari.boundarymesh.application.attestation.JcaP256AttestationCrypto;
import dev.adellaghmari.boundarymesh.application.consent.ConsentEvaluation;
import dev.adellaghmari.boundarymesh.application.crossing.CrossingComposer.CrossingResult;
import dev.adellaghmari.boundarymesh.domain.BoundaryDecision;
import dev.adellaghmari.boundarymesh.domain.BoundaryOutcome;
import dev.adellaghmari.boundarymesh.domain.ControlKind;
import dev.adellaghmari.boundarymesh.domain.ControlResult;
import dev.adellaghmari.boundarymesh.domain.ControlVector;
import dev.adellaghmari.boundarymesh.domain.EnforcementTrace;
import dev.adellaghmari.boundarymesh.domain.PolicyVersion;
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
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CloudKmsAttestationSignerTest {

    private static final Instant FIXED = Instant.parse("2026-09-14T12:00:00Z");

    private KeyPair pair;
    private FakeKmsSigningClient fake;
    private CloudKmsAttestationSigner signer;

    @BeforeEach
    void openFakeKms() throws Exception {
        pair = p256();
        fake = new FakeKmsSigningClient(pair);
        signer = new CloudKmsAttestationSigner(KmsSettings.production(), fake);
    }

    @Test
    void kmsDerSignatureNormalizesToP1363AndVerifies() {
        byte[] canonical = "synthetic-canonical-attestation".getBytes(StandardCharsets.UTF_8);
        AttestationSignature signature = signer.sign(canonical);
        assertThat(signature.present()).isTrue();
        assertThat(signature.base64()).doesNotContain("BEGIN PRIVATE");
        byte[] p1363 = java.util.Base64.getDecoder().decode(signature.base64());
        assertThat(p1363).hasSize(64);
        assertThat(signer.verify(canonical, signature, signer.key())).isTrue();
        assertThat(signer.key().algorithm()).isEqualTo("ES256");
        assertThat(signer.key().keyId()).isEqualTo(KmsSettings.production().cryptoKeyName());
        assertThat(signer.key().keyVersion()).isEqualTo("1");
        assertThat(fake.lastDigest()).isEqualTo(CloudKmsAttestationSigner.sha256(canonical));
    }

    @Test
    void factoryProducesSignedAttestationWithKmsKeyReference() {
        AttestationFactory factory = new AttestationFactory(Clock.fixed(FIXED, ZoneOffset.UTC), signer);
        ExchangeAttestation attestation = factory.create(
                crossing(),
                "LIVEPUBSUB",
                "LIVEPUBSUB",
                "principal",
                "JWT_BEARER",
                DeliveryResult.notAttempted("n/a"),
                new AuditChainId("livepubsub"),
                new AuditChainHead(new AuditChainId("livepubsub"), 1, new AuditHash("a".repeat(64))));
        assertThat(attestation.status()).isEqualTo(AttestationStatus.SIGNED);
        assertThat(attestation.signer().keyId()).contains("boundarymesh-attestation");
        AttestationService service = new AttestationService(new InMemoryAttestationRepository(), signer);
        assertThat(service.verify(attestation).status()).isEqualTo(SignatureVerificationStatus.VALID);
        assertThat(attestation.boundaryOutcome()).isEqualTo(BoundaryOutcome.ALLOW);
    }

    @Test
    void unavailableProviderReturnsUnsignedWithoutFakeSignature() {
        fake.failWith(new UnavailableProviderException("timeout token=secret"));
        AttestationSignature signature = signer.sign("bytes".getBytes(StandardCharsets.UTF_8));
        assertThat(signature.present()).isFalse();
        AttestationFactory factory = new AttestationFactory(Clock.fixed(FIXED, ZoneOffset.UTC), signer);
        ExchangeAttestation unsigned = factory.create(
                crossing(),
                "LIVEPUBSUB",
                "LIVEPUBSUB",
                "principal",
                "JWT_BEARER",
                DeliveryResult.notAttempted("n/a"),
                new AuditChainId("livepubsub"),
                new AuditChainHead(new AuditChainId("livepubsub"), 1, new AuditHash("a".repeat(64))));
        assertThat(unsigned.status()).isEqualTo(AttestationStatus.UNSIGNED);
        assertThat(unsigned.boundaryOutcome()).isEqualTo(BoundaryOutcome.ALLOW);
        assertThat(unsigned.signature().present()).isFalse();
    }

    @Test
    void permissionDeniedReturnsUnsigned() {
        fake.failWith(new PermissionDeniedProviderException("PERMISSION_DENIED Bearer abc"));
        assertThat(signer.sign("bytes".getBytes(StandardCharsets.UTF_8)).present())
                .isFalse();
        CloudKmsAccessException wrapped =
                CloudKmsAccessException.wrap(new PermissionDeniedProviderException("PERMISSION_DENIED Bearer abc"));
        assertThat(wrapped.getMessage().toLowerCase(Locale.ROOT)).doesNotContain("bearer");
        assertThat(wrapped.getMessage()).doesNotContain("abc");
    }

    @Test
    void invalidDerResponseReturnsUnsigned() {
        fake.returnDer(new byte[] {0x01, 0x02});
        assertThat(signer.sign("bytes".getBytes(StandardCharsets.UTF_8)).present())
                .isFalse();
    }

    @Test
    void timeoutAndMalformedKeyReferenceReturnUnsigned() {
        fake.failWith(new DeadlineExceededProviderException("timeout token=secret"));
        assertThat(signer.sign("bytes".getBytes(StandardCharsets.UTF_8)).present())
                .isFalse();
        fake = new FakeKmsSigningClient(pair);
        fake.failWith(new InvalidArgumentProviderException("malformed key name"));
        signer = new CloudKmsAttestationSigner(KmsSettings.production(), fake);
        assertThat(signer.sign("bytes".getBytes(StandardCharsets.UTF_8)).present())
                .isFalse();
    }

    @Test
    void privateKeyPemIsRejectedForVerification() {
        fake.returnPem("-----BEGIN PRIVATE KEY-----\nZmFrZQ==\n-----END PRIVATE KEY-----\n");
        byte[] canonical = "bytes".getBytes(StandardCharsets.UTF_8);
        AttestationSignature signature = signer.sign(canonical);
        assertThat(signer.verify(canonical, signature, signer.key())).isFalse();
        assertThat(signature.present()).isFalse();
    }

    @Test
    void controlledFailuresKeepDecisionAndNeverProduceSignedInvalidBytes() {
        List<RuntimeException> failures = List.of(
                new PermissionDeniedProviderException("synthetic"),
                new UnavailableProviderException("synthetic"),
                new DeadlineExceededProviderException("synthetic"),
                new RuntimeException("synthetic provider failure"));
        for (RuntimeException failure : failures) {
            fake = new FakeKmsSigningClient(pair);
            fake.failWith(failure);
            assertUnsignedDecisionPreserved();
        }
        for (byte[] invalid : List.of(new byte[0], new byte[] {1, 2}, new byte[] {0x30, 6, 2, 1, 1, 2, 1, 1})) {
            fake = new FakeKmsSigningClient(pair);
            fake.returnDer(invalid);
            assertUnsignedDecisionPreserved();
        }
    }

    private void assertUnsignedDecisionPreserved() {
        signer = new CloudKmsAttestationSigner(KmsSettings.production(), fake);
        CrossingResult crossing = crossing();
        AuditChainId chain = new AuditChainId("synthetic-kms-failure");
        ExchangeAttestation result = new AttestationFactory(Clock.fixed(FIXED, ZoneOffset.UTC), signer)
                .create(
                        crossing,
                        "SYNTHETIC",
                        "SYNTHETIC",
                        "synthetic",
                        "SYNTHETIC",
                        DeliveryResult.notAttempted("synthetic"),
                        chain,
                        new AuditChainHead(chain, 1, new AuditHash("a".repeat(64))));
        assertThat(result.status()).isEqualTo(AttestationStatus.UNSIGNED);
        assertThat(result.signature().present()).isFalse();
        assertThat(result.boundaryOutcome()).isEqualTo(crossing.decision().outcome());
        assertThat(result.transactionId())
                .isEqualTo(crossing.decision().transactionId().value());
        assertThat(result.auditChainId()).isEqualTo(chain);
        assertThat(result.contractId()).isEqualTo("synthetic-kms-contract");
    }

    @Test
    void differentCurvePublicKeyIsRejected() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp384r1"));
        fake.returnPem(EcPublicKeys.toPem(generator.generateKeyPair().getPublic()));
        assertUnsignedDecisionPreserved();
    }

    @Test
    void localJcaSignerStillSignsAndVerifiesIndependently() {
        JcaP256AttestationCrypto local = new JcaP256AttestationCrypto();
        byte[] canonical = "local-jca-bytes".getBytes(StandardCharsets.UTF_8);
        AttestationSignature signature = local.sign(canonical);
        assertThat(local.verify(canonical, signature, local.key())).isTrue();
        assertThat(local.key().keyId()).isEqualTo(SignerKeyReference.LOCAL_KEY_ID);
        assertThat(signer.verify(canonical, signature, signer.key())).isFalse();
    }

    static CrossingResult crossing() {
        ControlVector vector = ControlVector.allPass();
        BoundaryDecision decision = new BoundaryDecision(
                TransactionId.random(),
                BoundaryOutcome.ALLOW,
                List.of(),
                BoundaryDecision.summaryOf(vector),
                FIXED,
                PolicyVersion.PHASE_10);
        return new CrossingResult(
                decision,
                vector,
                new ContractEvaluation(
                        new dev.adellaghmari.boundarymesh.domain.contract.ContractReference(
                                new dev.adellaghmari.boundarymesh.domain.contract.ContractId("synthetic-kms-contract"),
                                new dev.adellaghmari.boundarymesh.domain.contract.ContractVersion("v1"),
                                dev.adellaghmari.boundarymesh.domain.contract.ContractType.JSON_SCHEMA,
                                dev.adellaghmari.boundarymesh.domain.contract.ContractStatus.SUPPORTED),
                        ControlResult.pass(ControlKind.CONTRACT),
                        List.of(),
                        0,
                        "VALID"),
                new EgressEvaluation(
                        new EgressPolicyId("policy"),
                        new EgressPolicyVersion("v1"),
                        "LIVEPUBSUB",
                        ControlResult.pass(ControlKind.EGRESS),
                        List.of(),
                        new RedactionSummary(List.of(), List.of(), List.of(), List.of(), 0),
                        "{}"),
                new ConsentEvaluation(ConsentStatus.GRANTED, null, "ok", null),
                null,
                EnforcementTrace.from(vector));
    }

    private static KeyPair p256() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        return generator.generateKeyPair();
    }

    private static final class FakeKmsSigningClient implements KmsSigningClient {
        private final KeyPair pair;
        private RuntimeException failure;
        private byte[] forcedDer;
        private String forcedPem;
        private byte[] lastDigest;

        private FakeKmsSigningClient(KeyPair pair) {
            this.pair = pair;
        }

        private void failWith(RuntimeException failure) {
            this.failure = failure;
        }

        private void returnDer(byte[] der) {
            this.forcedDer = der;
        }

        private void returnPem(String pem) {
            this.forcedPem = pem;
        }

        private byte[] lastDigest() {
            return lastDigest;
        }

        @Override
        public byte[] signSha256Digest(String cryptoKeyVersionName, byte[] sha256Digest) {
            if (failure != null) {
                throw failure;
            }
            if (forcedDer != null) {
                return forcedDer;
            }
            lastDigest = sha256Digest.clone();
            try {
                Signature signature = Signature.getInstance("NONEwithECDSA");
                signature.initSign(pair.getPrivate());
                signature.update(sha256Digest);
                return signature.sign();
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }

        @Override
        public String publicKeyPem(String cryptoKeyVersionName) {
            if (forcedPem != null) {
                return forcedPem;
            }
            return EcPublicKeys.toPem(pair.getPublic());
        }
    }

    private static final class DeadlineExceededProviderException extends RuntimeException {
        private DeadlineExceededProviderException(String message) {
            super(message);
        }
    }

    private static final class InvalidArgumentProviderException extends RuntimeException {
        private InvalidArgumentProviderException(String message) {
            super(message);
        }
    }

    private static final class UnavailableProviderException extends RuntimeException {
        private UnavailableProviderException(String message) {
            super(message);
        }
    }

    private static final class PermissionDeniedProviderException extends RuntimeException {
        private PermissionDeniedProviderException(String message) {
            super(message);
        }
    }
}
