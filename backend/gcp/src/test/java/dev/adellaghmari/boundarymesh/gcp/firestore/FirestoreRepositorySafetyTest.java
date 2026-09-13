package dev.adellaghmari.boundarymesh.gcp.firestore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.adellaghmari.boundarymesh.domain.attestation.AttestationId;
import dev.adellaghmari.boundarymesh.domain.quarantine.QuarantineId;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FirestoreRepositorySafetyTest {

    @Test
    void missingRecordsStayEmpty() {
        FirestoreFacade facade = mock(FirestoreFacade.class);
        when(facade.get(any())).thenReturn(Optional.empty());
        assertThat(new FirestoreAttestationRepository(facade).find(new AttestationId("missing-attestation")))
                .isEmpty();
        assertThat(new FirestoreQuarantineRepository(facade).find(new QuarantineId("missing-quarantine")))
                .isEmpty();
        assertThat(new FirestoreIdempotencyStore(facade).find("missing-event-key"))
                .isEmpty();
    }

    @Test
    void productionOpenRefusesEmulatorFallback() {
        FirestoreSettings emulator = FirestoreSettings.emulator("boundarymesh-dev", "127.0.0.1:18085");
        assertThatThrownBy(() -> GoogleFirestoreFacade.openProduction(emulator))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("emulator");
        assertThat(GoogleFirestoreFacade.open(emulator)).isInstanceOf(EmulatorHttpFirestoreFacade.class);
    }
}
