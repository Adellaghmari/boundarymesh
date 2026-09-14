package dev.adellaghmari.boundarymesh.gcp.kms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import org.junit.jupiter.api.Test;

class EcdsaP1363CodecTest {

    @Test
    void derAndP1363RoundTripVerifiesWithJca() throws Exception {
        KeyPair pair = p256();
        byte[] message = "synthetic-attestation-bytes".getBytes();
        Signature derSigner = Signature.getInstance("SHA256withECDSA");
        derSigner.initSign(pair.getPrivate());
        derSigner.update(message);
        byte[] der = derSigner.sign();
        byte[] p1363 = EcdsaP1363Codec.derToP1363(der);
        assertThat(p1363).hasSize(64);
        Signature p1363Verifier = Signature.getInstance("SHA256withECDSAinP1363Format");
        p1363Verifier.initVerify(pair.getPublic());
        p1363Verifier.update(message);
        assertThat(p1363Verifier.verify(p1363)).isTrue();
        byte[] derAgain = EcdsaP1363Codec.p1363ToDer(p1363);
        Signature derVerifier = Signature.getInstance("SHA256withECDSA");
        derVerifier.initVerify(pair.getPublic());
        derVerifier.update(message);
        assertThat(derVerifier.verify(derAgain)).isTrue();
    }

    @Test
    void rejectsNonDerInput() {
        assertThatThrownBy(() -> EcdsaP1363Codec.derToP1363(new byte[] {0x01, 0x02}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EcdsaP1363Codec.p1363ToDer(new byte[] {0x01}))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void normalizesSignPaddingAndLeftPadsShortCoordinatesWithoutTruncation() {
        byte[] p1363 = new byte[64];
        p1363[0] = (byte) 0x80;
        p1363[63] = 1;
        byte[] der = EcdsaP1363Codec.p1363ToDer(p1363);
        assertThat(der[3]).isEqualTo((byte) 33);
        assertThat(der[4]).isZero();
        assertThat(der[5]).isEqualTo((byte) 0x80);
        assertThat(EcdsaP1363Codec.derToP1363(der)).isEqualTo(p1363);
    }

    @Test
    void rejectsTruncationTrailingBytesNegativeZeroAndNonMinimalIntegers() {
        for (String hex : java.util.List.of(
                "3006020101020100",
                "3006020180020101",
                "300702020001020101",
                "3006020101020201",
                "300702010102010100",
                "308106020101020101")) {
            assertThatThrownBy(() ->
                            EcdsaP1363Codec.derToP1363(java.util.HexFormat.of().parseHex(hex)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        byte[] valid = java.util.HexFormat.of().parseHex("3006020101020101");
        for (int length = 0; length < valid.length; length++) {
            byte[] truncated = java.util.Arrays.copyOf(valid, length);
            assertThatThrownBy(() -> EcdsaP1363Codec.derToP1363(truncated))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    private static KeyPair p256() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        return generator.generateKeyPair();
    }
}
