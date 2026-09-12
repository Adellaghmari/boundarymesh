package dev.adellaghmari.boundarymesh.gcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Opt-in template for later Firestore/Pub/Sub emulator clients. Excluded from default {@code
 * mvnw verify} via the {@code gcp-emulator} Surefire group.
 */
@Tag(GcpEmulatorGuard.EMULATOR_TEST_TAG)
class GcpEmulatorIntegrationTemplateTest {

    @Test
    void laterPhasesAttachEmulatorClientsHereWithoutCallingGoogleApisInPhase15C() {
        assumeTrue(GcpEmulatorGuard.emulatorIntegrationEnabled());
        GcpEmulatorEndpoints endpoints = GcpEmulatorEndpoints.localDefaults();
        assertThat(endpoints.firestoreHost()).isNotBlank();
        assertThat(endpoints.pubsubHost()).isNotBlank();
    }
}
