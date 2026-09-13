package dev.adellaghmari.boundarymesh.gcp.firestore;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

class FirestoreSensitiveFieldGuardTest {

    @Test
    void rejectsAuthorizationAndPrivateKeyMaterial() {
        assertThatThrownBy(() -> FirestoreSensitiveFieldGuard.reject(Map.of("authorization", "Bearer fake")))
                .hasMessageContaining("forbidden");
        assertThatThrownBy(() -> FirestoreSensitiveFieldGuard.reject(Map.of("note", "-----BEGIN PRIVATE KEY-----")))
                .hasMessageContaining("forbidden");
        assertThatThrownBy(() -> FirestoreSensitiveFieldGuard.reject(Map.of("client_secret", "fake-secret")))
                .hasMessageContaining("forbidden");
    }

    @Test
    void acceptsSyntheticOperationalFields() {
        assertThatCode(() -> FirestoreSensitiveFieldGuard.reject(
                        Map.of("nonce_hash", "fake-replay-nonce-15fa", "event_key", "fake-event-key-15fa")))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsControlKindAuthorizationStatus() {
        assertThatCode(() -> FirestoreSensitiveFieldGuard.reject(
                        Map.of("applied_controls", Map.of("AUTHORIZATION", "NOT_APPLICABLE"))))
                .doesNotThrowAnyException();
    }
}
