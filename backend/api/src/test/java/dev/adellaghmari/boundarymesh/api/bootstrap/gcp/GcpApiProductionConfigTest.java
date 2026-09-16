package dev.adellaghmari.boundarymesh.api.bootstrap.gcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.adellaghmari.boundarymesh.application.attestation.AttestationSigner;
import dev.adellaghmari.boundarymesh.application.attestation.AttestationVerifier;
import dev.adellaghmari.boundarymesh.gcp.runtime.GcpApiRuntime;
import org.junit.jupiter.api.Test;

class GcpApiProductionConfigTest {

    @Test
    void exposesKmsSigningAndVerificationAsUnambiguousSpringPortTypes() {
        GcpApiRuntime runtime = mock(GcpApiRuntime.class);
        DualAttestationCrypto crypto = mock(DualAttestationCrypto.class);
        when(runtime.attestationSigner()).thenReturn(crypto);
        when(runtime.attestationVerifier()).thenReturn(crypto);

        GcpApiProductionConfig config = new GcpApiProductionConfig();
        AttestationSigner signer = config.gcpAttestationSigner(runtime);
        AttestationVerifier verifier = config.gcpAttestationVerifier(runtime);

        assertThat(signer).isNotInstanceOf(AttestationVerifier.class);
        assertThat(verifier).isNotInstanceOf(AttestationSigner.class);
    }

    private interface DualAttestationCrypto extends AttestationSigner, AttestationVerifier {}
}
