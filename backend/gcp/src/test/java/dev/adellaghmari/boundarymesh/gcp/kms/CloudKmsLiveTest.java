package dev.adellaghmari.boundarymesh.gcp.kms;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.cloud.kms.v1.CryptoKey.CryptoKeyPurpose;
import com.google.cloud.kms.v1.CryptoKeyVersion.CryptoKeyVersionAlgorithm;
import com.google.cloud.kms.v1.CryptoKeyVersion.CryptoKeyVersionState;
import com.google.cloud.kms.v1.KeyManagementServiceClient;
import com.google.cloud.kms.v1.ProtectionLevel;
import dev.adellaghmari.boundarymesh.application.attestation.AttestationCanonicalizer;
import dev.adellaghmari.boundarymesh.application.attestation.AttestationFactory;
import dev.adellaghmari.boundarymesh.application.attestation.AttestationService;
import dev.adellaghmari.boundarymesh.application.attestation.InMemoryAttestationRepository;
import dev.adellaghmari.boundarymesh.domain.BoundaryOutcome;
import dev.adellaghmari.boundarymesh.domain.attestation.AttestationStatus;
import dev.adellaghmari.boundarymesh.domain.attestation.ExchangeAttestation;
import dev.adellaghmari.boundarymesh.domain.attestation.SignatureVerificationStatus;
import dev.adellaghmari.boundarymesh.domain.audit.AuditChainHead;
import dev.adellaghmari.boundarymesh.domain.audit.AuditChainId;
import dev.adellaghmari.boundarymesh.domain.audit.AuditHash;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryResult;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Real ADC + official SDK, exactly two signs, no persistence or infrastructure writes. */
@Tag("gcp-live-kms")
class CloudKmsLiveTest {
    private static final String TARGET = "projects/boundarymesh-dev/locations/europe-west4/"
            + "keyRings/boundarymesh-attestation/cryptoKeys/exchange-attestation/cryptoKeyVersions/1";

    @Test
    void realKmsAttestationVerifiesAndSurvivesClientRecreation() throws Exception {
        assertThat(System.getenv("GOOGLE_APPLICATION_CREDENTIALS")).isNullOrEmpty();
        KmsSettings settings = KmsSettings.production();
        assertThat(settings.projectId()).isEqualTo("boundarymesh-dev");
        assertThat(settings.location()).isEqualTo("europe-west4");
        assertThat(settings.keyRing()).isEqualTo("boundarymesh-attestation");
        assertThat(settings.cryptoKey()).isEqualTo("exchange-attestation");
        assertThat(settings.cryptoKeyVersion()).isEqualTo("1");
        assertThat(settings.cryptoKeyVersionName()).isEqualTo(TARGET);
        assertThat(KmsSettings.ALGORITHM).isEqualTo("EC_SIGN_P256_SHA256");
        checkMetadata(settings);
        String runId = "livekms-"
                + DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                        .withZone(ZoneOffset.UTC)
                        .format(Instant.now())
                + "-" + UUID.randomUUID().toString().substring(0, 8);
        System.out.println("LIVE_KMS_RUN=" + runId);
        ExchangeAttestation attestation;
        byte[] firstPublicKey;
        // A transparent recorder delegates every operation to the real production SDK client.
        // No mock transport, local signer, private key, or fallback exists in this suite.
        try (GoogleKmsSigningClient production = GoogleKmsSigningClient.connect()) {
            assertThat(production.sdkClientType()).isEqualTo(KeyManagementServiceClient.class);
            RecordingClient recording = new RecordingClient(production);
            try (CloudKmsAttestationSigner signer = new CloudKmsAttestationSigner(settings, recording)) {
                AuditChainId chain = new AuditChainId(runId);
                attestation = new AttestationFactory(Clock.systemUTC(), signer)
                        .create(
                                CloudKmsAttestationSignerTest.crossing(),
                                "SYNTHETIC",
                                "SYNTHETIC",
                                runId,
                                "SYNTHETIC",
                                DeliveryResult.notAttempted("synthetic KMS verification"),
                                chain,
                                new AuditChainHead(chain, 1, new AuditHash("a".repeat(64))));
                assertThat(attestation.status()).isEqualTo(AttestationStatus.SIGNED);
                assertThat(attestation.signer()).isEqualTo(signer.key());
                assertThat(attestation.signer().keyId()).isEqualTo(settings.cryptoKeyName());
                assertThat(attestation.signer().keyVersion()).isEqualTo("1");
                assertThat(attestation.signer().algorithm()).isEqualTo("ES256");
                assertThat(attestation.contractId()).isEqualTo("synthetic-kms-contract");
                assertThat(attestation.contractVersion()).isEqualTo("v1");
                assertThat(attestation.egressPolicyId()).isEqualTo("policy");
                assertThat(attestation.boundaryOutcome()).isEqualTo(BoundaryOutcome.ALLOW);
                assertThat(attestation.auditChainId()).isEqualTo(chain);
                assertThat(attestation.auditSequence()).isEqualTo(1);
                assertThat(attestation.auditHead().hex()).isEqualTo("a".repeat(64));
                byte[] canonical = AttestationCanonicalizer.bytes(attestation);
                assertThat(recording.calls).isEqualTo(1);
                assertThat(recording.digest)
                        .isEqualTo(MessageDigest.getInstance("SHA-256").digest(canonical));
                assertThat(recording.target).isEqualTo(TARGET);
                byte[] p1363 =
                        Base64.getDecoder().decode(attestation.signature().base64());
                assertThat(p1363).hasSize(64);
                assertThat(Base64.getEncoder().encodeToString(p1363))
                        .isEqualTo(attestation.signature().base64());
                proveActualDer(recording.der, p1363);
                ECPublicKey publicKey = (ECPublicKey) signer.publicKey();
                assertThat(publicKey.getParams().getCurve().getField().getFieldSize())
                        .isEqualTo(256);
                assertThat(recording.publicKeyCalls).isEqualTo(1);
                firstPublicKey = publicKey.getEncoded();
                Signature independentDerVerifier = Signature.getInstance("SHA256withECDSA");
                independentDerVerifier.initVerify(publicKey);
                independentDerVerifier.update(canonical);
                assertThat(independentDerVerifier.verify(recording.der)).isTrue();
                AttestationService service = new AttestationService(new InMemoryAttestationRepository(), signer);
                assertThat(service.verify(attestation).status()).isEqualTo(SignatureVerificationStatus.VALID);
                byte[] altered = canonical.clone();
                altered[altered.length / 2] ^= 1;
                assertThat(signer.verify(altered, attestation.signature(), attestation.signer()))
                        .isFalse();
                System.out.println("LIVE_KMS_SIGNER=CloudKmsAttestationSigner SDK=KeyManagementServiceClient ADC=REAL");
                System.out.println("LIVE_KMS_SIGN=SUCCESS FORMAT=DER P1363_BYTES=64 SHA256=VERIFIED");
                System.out.println("LIVE_KMS_PUBLIC_KEY=NIST_P256 ATTESTATION=SIGNED VERIFY=VALID ALTERED=INVALID");
            }
        }
        try (CloudKmsAttestationSigner recreated = CloudKmsAttestationSigner.connect(settings)) {
            assertThat(recreated.publicKey().getEncoded()).isEqualTo(firstPublicKey);
            byte[] canonical = AttestationCanonicalizer.bytes(attestation);
            assertThat(recreated.verify(canonical, attestation.signature(), attestation.signer()))
                    .isTrue();
            var second = recreated.sign(canonical);
            assertThat(second.present()).isTrue();
            assertThat(recreated.verify(canonical, second, recreated.key())).isTrue();
            System.out.println("LIVE_KMS_RECREATED_CLIENT=VALID SECOND_SIGN=SUCCESS PRIVATE_KEY=KMS_ONLY");
        }
        checkMetadata(settings);
        System.out.println("LIVE_KMS_METADATA=UNCHANGED VERSION_COUNT=1 CLOUD_DOCUMENT_WRITES=0");
    }

    private static void checkMetadata(KmsSettings settings) throws Exception {
        try (KeyManagementServiceClient metadata = KeyManagementServiceClient.create()) {
            assertThat(metadata.getSettings().getEndpoint()).isEqualTo("cloudkms.googleapis.com:443");
            var version = metadata.getCryptoKeyVersion(TARGET);
            assertThat(version.getName()).isEqualTo(TARGET);
            assertThat(version.getState()).isEqualTo(CryptoKeyVersionState.ENABLED);
            assertThat(version.getAlgorithm()).isEqualTo(CryptoKeyVersionAlgorithm.EC_SIGN_P256_SHA256);
            assertThat(version.getProtectionLevel()).isEqualTo(ProtectionLevel.SOFTWARE);
            var key = metadata.getCryptoKey(settings.cryptoKeyName());
            assertThat(key.getPurpose()).isEqualTo(CryptoKeyPurpose.ASYMMETRIC_SIGN);
            assertThat(key.hasRotationPeriod()).isFalse();
            assertThat(key.hasNextRotationTime()).isFalse();
            assertThat(metadata.listCryptoKeyVersions(settings.cryptoKeyName()).iterateAll())
                    .extracting(v -> v.getName())
                    .containsExactly(TARGET);
            assertThat(metadata.listImportJobs(settings.keyRingName()).iterateAll())
                    .isEmpty();
        }
    }

    private static void proveActualDer(byte[] der, byte[] p1363) {
        assertThat(der[0]).isEqualTo((byte) 0x30);
        assertThat(der[1] & 0xff).isEqualTo(der.length - 2);
        int offset = 2;
        for (int coordinate = 0; coordinate < 2; coordinate++) {
            assertThat(der[offset++]).isEqualTo((byte) 2);
            int length = der[offset++] & 0xff;
            byte[] integer = Arrays.copyOfRange(der, offset, offset + length);
            assertThat(new BigInteger(integer).signum()).isPositive();
            byte[] fixed = Arrays.copyOfRange(p1363, coordinate * 32, (coordinate + 1) * 32);
            assertThat(new BigInteger(1, fixed)).isEqualTo(new BigInteger(integer));
            int magnitudeOffset = integer[0] == 0 ? 1 : 0;
            int magnitudeLength = length - magnitudeOffset;
            assertThat(Arrays.copyOfRange(fixed, 32 - magnitudeLength, 32))
                    .isEqualTo(Arrays.copyOfRange(integer, magnitudeOffset, length));
            assertThat(Arrays.copyOfRange(fixed, 0, 32 - magnitudeLength)).isEqualTo(new byte[32 - magnitudeLength]);
            offset += length;
        }
        assertThat(offset).isEqualTo(der.length);
        assertThat(EcdsaP1363Codec.p1363ToDer(p1363)).isEqualTo(der);
    }

    private static final class RecordingClient implements KmsSigningClient {
        private final GoogleKmsSigningClient delegate;
        private byte[] digest;
        private byte[] der;
        private String target;
        private int calls;
        private int publicKeyCalls;

        private RecordingClient(GoogleKmsSigningClient delegate) {
            this.delegate = delegate;
        }

        @Override
        public byte[] signSha256Digest(String name, byte[] value) {
            target = name;
            digest = value.clone();
            der = delegate.signSha256Digest(name, value);
            calls++;
            return der.clone();
        }

        @Override
        public String publicKeyPem(String name) {
            publicKeyCalls++;
            return delegate.publicKeyPem(name);
        }
    }
}
