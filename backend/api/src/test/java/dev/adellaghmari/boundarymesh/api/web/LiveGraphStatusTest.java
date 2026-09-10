package dev.adellaghmari.boundarymesh.api.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LiveGraphStatusTest {

    @Test
    void configuredLiveModeIsNotVerified() {
        assertThat(LiveGraphStatus.connectorMode(true)).isEqualTo(LiveGraphStatus.LIVE_CONFIGURED);
        assertThat(LiveGraphStatus.pullMode(true, 401)).isEqualTo(LiveGraphStatus.LIVE_CONFIGURED);
        assertThat(LiveGraphStatus.pullLive(true, 401)).isFalse();
        assertThat(LiveGraphStatus.pullMode(true, 403)).isEqualTo(LiveGraphStatus.LIVE_CONFIGURED);
        assertThat(LiveGraphStatus.pullMode(true, 500)).isEqualTo(LiveGraphStatus.LIVE_CONFIGURED);
    }

    @Test
    void onlyProvider2xxIsLiveVerified() {
        assertThat(LiveGraphStatus.pullMode(true, 200)).isEqualTo(LiveGraphStatus.LIVE_VERIFIED);
        assertThat(LiveGraphStatus.pullLive(true, 200)).isTrue();
        assertThat(LiveGraphStatus.pullMode(true, 204)).isEqualTo(LiveGraphStatus.LIVE_VERIFIED);
        assertThat(LiveGraphStatus.pullMode(false, 200)).isEqualTo(LiveGraphStatus.CONTRACT_HARNESS);
        assertThat(LiveGraphStatus.pullLive(false, 200)).isFalse();
    }

    @Test
    void emptyCalendarDoesNotBlockLiveVerified() {
        assertThat(LiveGraphStatus.pullLive(true, 200)).isTrue();
        assertThat(LiveGraphStatus.connectorMode(true)).isEqualTo(LiveGraphStatus.LIVE_CONFIGURED);
    }
}
