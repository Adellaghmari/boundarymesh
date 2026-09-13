package dev.adellaghmari.boundarymesh.gcp.firestore;

import static org.assertj.core.api.Assertions.assertThat;

import dev.adellaghmari.boundarymesh.gcp.GcpRuntimeMode;
import org.junit.jupiter.api.Test;

class FirestoreAdapterSelectionTest {

    @Test
    void localAndTestKeepInMemoryAdapters() {
        assertThat(FirestoreAdapterSelection.firestorePersistenceSelected(GcpRuntimeMode.LOCAL))
                .isFalse();
        assertThat(FirestoreAdapterSelection.firestorePersistenceSelected(GcpRuntimeMode.TEST))
                .isFalse();
        assertThat(FirestoreAdapterSelection.requiresAdc(GcpRuntimeMode.LOCAL)).isFalse();
        assertThat(FirestoreAdapterSelection.requiresAdc(GcpRuntimeMode.TEST)).isFalse();
        assertThat(FirestoreAdapterSelection.requiresAdc(GcpRuntimeMode.GCP_EMULATOR))
                .isFalse();
    }

    @Test
    void emulatorAndProdMaySelectFirestoreAdapters() {
        assertThat(FirestoreAdapterSelection.firestorePersistenceSelected(GcpRuntimeMode.GCP_EMULATOR))
                .isTrue();
        assertThat(FirestoreAdapterSelection.firestorePersistenceSelected(GcpRuntimeMode.PROD))
                .isTrue();
        assertThat(FirestoreAdapterSelection.requiresAdc(GcpRuntimeMode.PROD)).isTrue();
    }
}
