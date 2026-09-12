package dev.adellaghmari.boundarymesh.gcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class GcpEmulatorFoundationTest {

    @Test
    void defaultVerifyDoesNotRequireCloudCredentials() {
        assertThat(GcpEmulatorGuard.cloudCredentialsRequiredForDefaultTests()).isFalse();
    }

    @Test
    void localEmulatorEndpointsAvoidApiAndWorkerPorts() {
        GcpEmulatorEndpoints endpoints = GcpEmulatorEndpoints.localDefaults();
        assertThat(endpoints.firestoreHost()).isEqualTo("127.0.0.1:18085");
        assertThat(endpoints.pubsubHost()).isEqualTo("127.0.0.1:18086");
        assertThat(endpoints.firestoreHost()).doesNotContain(":8080");
        assertThat(endpoints.pubsubHost()).doesNotContain(":8081");
    }

    @Test
    void blankEmulatorHostsAreRejected() {
        assertThatThrownBy(() -> new GcpEmulatorEndpoints(" ", "127.0.0.1:18086"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void defaultRuntimeModeIsLocalAndNoProductionAdapterExists() {
        assertThat(GcpRuntimeMode.LOCAL).isNotEqualTo(GcpRuntimeMode.PROD);
        assertThat(GcpPortMapping.values()).isNotEmpty().allMatch(mapping -> !mapping.productionAdapterImplemented());
        assertThat(GcpPortMapping.OUTBOX.localImplementation()).isEqualTo("ABSENT");
    }
}
