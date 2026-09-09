package dev.adellaghmari.boundarymesh.application.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class QuayForgeSigningContractTest {

    @Test
    void rawBodyChangesInvalidateSignatureAndComparisonIsConstantTimeApi() {
        byte[] secret = "test-fixture-secret".getBytes(StandardCharsets.UTF_8);
        byte[] body = "{\"event_id\":\"a\"}".getBytes(StandardCharsets.UTF_8);
        byte[] reordered = "{\"a\":1,\"event_id\":\"a\"}".getBytes(StandardCharsets.UTF_8);
        String header = QuayForgeSigningContract.signatureHeader(secret, "100", "nonce-1", body);
        String expected = QuayForgeSigningContract.signatureHeader(secret, "100", "nonce-1", body);
        assertThat(QuayForgeSigningContract.signaturesEqual(header, expected)).isTrue();
        String other = QuayForgeSigningContract.signatureHeader(secret, "100", "nonce-1", reordered);
        assertThat(QuayForgeSigningContract.signaturesEqual(header, other)).isFalse();
        assertThat(QuayForgeSigningContract.hasSignatureFormat(header)).isTrue();
        assertThat(QuayForgeSigningContract.hasSignatureFormat("v1=nope")).isFalse();
    }
}
