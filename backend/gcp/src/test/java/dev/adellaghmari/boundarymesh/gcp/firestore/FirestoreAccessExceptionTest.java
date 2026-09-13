package dev.adellaghmari.boundarymesh.gcp.firestore;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FirestoreAccessExceptionTest {

    @Test
    void wrapsProviderErrorWithoutCopyingCredentialOrPayloadText() {
        RuntimeException leaked = new UnauthenticatedProviderException(
                "UNAUTHENTICATED: invalid token Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.payload and document {\"authorization\":\"secret\"}");
        FirestoreAccessException wrapped = FirestoreAccessException.wrap(leaked);
        assertThat(wrapped.kind()).isEqualTo(FirestoreAccessException.Kind.PERMISSION_DENIED);
        assertThat(wrapped.getMessage()).isEqualTo("Firestore call failed (PERMISSION_DENIED)");
        assertThat(wrapped.getMessage()).doesNotContain("Bearer");
        assertThat(wrapped.getMessage()).doesNotContain("eyJhbGciOi");
        assertThat(wrapped.getMessage()).doesNotContain("authorization");
        assertThat(wrapped.getCause()).isNull();
    }

    @Test
    void classifiesNotFoundWithoutLeakingDocumentBody() {
        RuntimeException missing = new NotFoundProviderException("NOT_FOUND: no document auditChains/secret-payload");
        FirestoreAccessException wrapped = FirestoreAccessException.wrap(missing);
        assertThat(wrapped.kind()).isEqualTo(FirestoreAccessException.Kind.NOT_FOUND);
        assertThat(wrapped.getMessage()).doesNotContain("secret-payload");
        assertThat(wrapped.getMessage()).doesNotContain("auditChains");
    }

    private static final class UnauthenticatedProviderException extends RuntimeException {
        private UnauthenticatedProviderException(String message) {
            super(message);
        }
    }

    private static final class NotFoundProviderException extends RuntimeException {
        private NotFoundProviderException(String message) {
            super(message);
        }
    }
}
