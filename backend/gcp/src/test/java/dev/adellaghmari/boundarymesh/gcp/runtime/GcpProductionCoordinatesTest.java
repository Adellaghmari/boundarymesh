package dev.adellaghmari.boundarymesh.gcp.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.adellaghmari.boundarymesh.gcp.kms.KmsSettings;
import org.junit.jupiter.api.Test;

class GcpProductionCoordinatesTest {

    @Test
    void acceptsOnlyTheLockedProductionTopology() {
        GcpProductionCoordinates coordinates = GcpProductionCoordinates.boundaryMeshDev();

        assertThat(coordinates.firestoreSettings().databaseId()).isEqualTo("boundarymesh");
        assertThat(coordinates.pubSubSettings().topicId()).isEqualTo("boundarymesh-deliveries");
        assertThat(coordinates.pubSubSettings().subscriptionId()).isEqualTo("boundarymesh-delivery-worker");
        assertThat(coordinates.kmsSettings().cryptoKeyVersionName())
                .isEqualTo("projects/boundarymesh-dev/locations/europe-west4/keyRings/"
                        + "boundarymesh-attestation/cryptoKeys/exchange-attestation/cryptoKeyVersions/1");
        assertThat(KmsSettings.ALGORITHM).isEqualTo("EC_SIGN_P256_SHA256");
    }

    @Test
    void rejectsTopologyDrift() {
        GcpProductionCoordinates expected = GcpProductionCoordinates.boundaryMeshDev();

        assertThatThrownBy(() -> new GcpProductionCoordinates(
                        "other-project",
                        expected.location(),
                        expected.firestoreDatabase(),
                        expected.pubSubTopic(),
                        expected.pubSubSubscription(),
                        expected.kmsKeyRing(),
                        expected.kmsCryptoKey(),
                        expected.kmsCryptoKeyVersion()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("project must be boundarymesh-dev");
    }
}
