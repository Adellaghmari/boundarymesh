package dev.adellaghmari.boundarymesh.gcp.pubsub;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PubSubAccessExceptionTest {

    @Test
    void wrapsProviderErrorWithoutCopyingPayloadOrTokenText() {
        RuntimeException leaked = new UnauthenticatedProviderException(
                "UNAUTHENTICATED: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.payload {\"authorization\":\"secret\"}");
        PubSubAccessException wrapped = PubSubAccessException.wrap(leaked);
        assertThat(wrapped.kind()).isEqualTo(PubSubAccessException.Kind.PERMISSION_DENIED);
        assertThat(wrapped.getMessage()).isEqualTo("Pub/Sub call failed (PERMISSION_DENIED)");
        assertThat(wrapped.getMessage()).doesNotContain("Bearer");
        assertThat(wrapped.getMessage()).doesNotContain("authorization");
        assertThat(wrapped.getCause()).isNull();
    }

    private static final class UnauthenticatedProviderException extends RuntimeException {
        private UnauthenticatedProviderException(String message) {
            super(message);
        }
    }
}
