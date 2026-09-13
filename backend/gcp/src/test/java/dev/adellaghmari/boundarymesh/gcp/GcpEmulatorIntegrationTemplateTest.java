package dev.adellaghmari.boundarymesh.gcp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Opt-in emulator group. Default {@code mvnw verify} excludes {@code gcp-emulator} and {@code
 * gcp-live}. Persistence proofs live in {@code FirestorePersistenceEmulatorTest}.
 */
@Tag(GcpEmulatorGuard.EMULATOR_TEST_TAG)
class GcpEmulatorIntegrationTemplateTest {

    @Test
    void documentedEmulatorEndpointsRemainLocal() {
        GcpEmulatorEndpoints endpoints = GcpEmulatorEndpoints.localDefaults();
        assertThat(endpoints.firestoreHost()).isEqualTo("127.0.0.1:18085");
        assertThat(endpoints.pubsubHost()).isEqualTo("127.0.0.1:18086");
    }
}
