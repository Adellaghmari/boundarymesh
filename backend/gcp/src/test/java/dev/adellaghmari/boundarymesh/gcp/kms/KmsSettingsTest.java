package dev.adellaghmari.boundarymesh.gcp.kms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class KmsSettingsTest {

    @Test
    void productionCoordinatesAreStable() {
        KmsSettings settings = KmsSettings.production();
        assertThat(settings.projectId()).isEqualTo("boundarymesh-dev");
        assertThat(settings.location()).isEqualTo("europe-west4");
        assertThat(settings.keyRing()).isEqualTo("boundarymesh-attestation");
        assertThat(settings.cryptoKey()).isEqualTo("exchange-attestation");
        assertThat(settings.cryptoKeyVersion()).isEqualTo("1");
        assertThat(settings.cryptoKeyName())
                .isEqualTo(
                        "projects/boundarymesh-dev/locations/europe-west4/keyRings/boundarymesh-attestation/cryptoKeys/exchange-attestation");
        assertThat(settings.cryptoKeyVersionName()).endsWith("/cryptoKeyVersions/1");
        assertThat(settings.keyRingName())
                .isEqualTo("projects/boundarymesh-dev/locations/europe-west4/keyRings/boundarymesh-attestation");
        assertThat(KmsSettings.ALGORITHM).isEqualTo("EC_SIGN_P256_SHA256");
    }

    @Test
    void blankCoordinatesAreRejected() {
        assertThatThrownBy(() ->
                        new KmsSettings(" ", "europe-west4", "boundarymesh-attestation", "exchange-attestation", "1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsResourcePathsAsSegments() {
        assertThatThrownBy(() -> new KmsSettings(
                        "projects/boundarymesh-dev",
                        "europe-west4",
                        "boundarymesh-attestation",
                        "exchange-attestation",
                        "1"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
