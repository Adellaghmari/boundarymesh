package dev.adellaghmari.boundarymesh.gcp.firestore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class FirestoreDocumentIdsTest {

    @Test
    void safeSlugsPassThroughAndSequencesArePadded() {
        assertThat(FirestoreDocumentIds.requireSafe("quayforge-orders")).isEqualTo("quayforge-orders");
        assertThat(FirestoreDocumentIds.sequence(7)).isEqualTo("00000000000000000007");
    }

    @Test
    void slashesAreHashedInsteadOfCreatingNestedPaths() {
        String id = FirestoreDocumentIds.requireSafe("fake/event/key");
        assertThat(id).startsWith("h_");
        assertThat(id).doesNotContain("/");
    }

    @Test
    void blankAndDotIdsAreRejected() {
        assertThatThrownBy(() -> FirestoreDocumentIds.requireSafe(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FirestoreDocumentIds.requireSafe("..")).isInstanceOf(IllegalArgumentException.class);
    }
}
