package dev.adellaghmari.boundarymesh.gcp.firestore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class FirestoreSettingsTest {

    @Test
    void emulatorDefaultsToNamedBoundarymeshDatabaseWithoutAdc() {
        FirestoreSettings settings = FirestoreSettings.emulator("boundarymesh-dev", "127.0.0.1:18085");
        assertThat(settings.databaseId()).isEqualTo(FirestoreSettings.DEFAULT_DATABASE_ID);
        assertThat(settings.databaseId()).isEqualTo("boundarymesh");
        assertThat(settings.usesEmulator()).isTrue();
        assertThat(settings.emulatorHost()).contains("127.0.0.1:18085");
    }

    @Test
    void productionUsesNamedDatabaseAndDoesNotSelectEmulator() {
        FirestoreSettings settings = FirestoreSettings.production("boundarymesh-dev");
        assertThat(settings.databaseId()).isEqualTo("boundarymesh");
        assertThat(settings.usesEmulator()).isFalse();
        assertThat(settings.databaseResourceName()).isEqualTo("projects/boundarymesh-dev/databases/boundarymesh");
    }

    @Test
    void defaultDatabaseIdIsRejected() {
        assertThatThrownBy(() -> FirestoreSettings.emulator("boundarymesh-dev", "(default)", "127.0.0.1:18085"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("boundarymesh");
    }
}
