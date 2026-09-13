package dev.adellaghmari.boundarymesh.gcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class GcpLiveGuardTest {

    @Test
    void refusesEmulatorHostAndDefaultDatabase() {
        assertThatThrownBy(() -> GcpLiveGuard.refuseEmulatorHost("127.0.0.1:8080"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FIRESTORE_EMULATOR_HOST");
        assertThatThrownBy(() -> GcpLiveGuard.refusePubSubEmulatorHost("127.0.0.1:8085"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PUBSUB_EMULATOR_HOST");
        assertThatCode(() -> GcpLiveGuard.refuseEmulatorHost(null)).doesNotThrowAnyException();
        assertThatCode(() -> GcpLiveGuard.refuseEmulatorHost("")).doesNotThrowAnyException();
        assertThatCode(() -> GcpLiveGuard.refusePubSubEmulatorHost(null)).doesNotThrowAnyException();
        assertThat(GcpLiveGuard.REQUIRED_PROJECT).isEqualTo("boundarymesh-dev");
        assertThat(GcpLiveGuard.REQUIRED_DATABASE).isEqualTo("boundarymesh");
        assertThat(GcpLiveGuard.REQUIRED_DATABASE).isNotEqualTo("(default)");
        assertThat(GcpLiveGuard.LIVE_PUBSUB_TEST_TAG).isEqualTo("gcp-live-pubsub");
    }
}
