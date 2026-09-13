package dev.adellaghmari.boundarymesh.gcp.pubsub;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PubSubSettingsTest {

    @Test
    void emulatorUsesNamedDeliveryTopicAndPullSubscription() {
        PubSubSettings settings = PubSubSettings.emulator("boundarymesh-dev", "127.0.0.1:18086");
        assertThat(settings.topicId()).isEqualTo("boundarymesh-deliveries");
        assertThat(settings.subscriptionId()).isEqualTo("boundarymesh-delivery-worker");
        assertThat(settings.usesEmulator()).isTrue();
    }

    @Test
    void productionDoesNotSelectEmulator() {
        PubSubSettings settings = PubSubSettings.production("boundarymesh-dev");
        assertThat(settings.usesEmulator()).isFalse();
        assertThat(settings.topicId()).isEqualTo(PubSubSettings.DELIVERY_TOPIC);
    }
}
