package dev.adellaghmari.boundarymesh.gcp.kms;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CloudKmsAccessExceptionTest {

    @Test
    void wrapsProviderErrorWithoutCopyingTokenText() {
        RuntimeException leaked = new UnauthenticatedProviderException(
                "UNAUTHENTICATED: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.payload");
        CloudKmsAccessException wrapped = CloudKmsAccessException.wrap(leaked);
        assertThat(wrapped.kind()).isEqualTo(CloudKmsAccessException.Kind.PERMISSION_DENIED);
        assertThat(wrapped.getMessage()).isEqualTo("Cloud KMS call failed (PERMISSION_DENIED)");
        assertThat(wrapped.getMessage()).doesNotContain("Bearer");
        assertThat(wrapped.getCause()).isNull();
    }

    @Test
    void timeoutAndInvalidArgumentAreClassifiedWithoutCopyingProviderText() {
        CloudKmsAccessException timeout = CloudKmsAccessException.wrap(
                new DeadlineExceededProviderException("DEADLINE_EXPIRED token=ya29.secret"));
        assertThat(timeout.kind()).isEqualTo(CloudKmsAccessException.Kind.UNAVAILABLE);
        assertThat(timeout.getMessage()).isEqualTo("Cloud KMS call failed (UNAVAILABLE)");
        assertThat(timeout.getMessage()).doesNotContain("ya29");
        CloudKmsAccessException invalid =
                CloudKmsAccessException.wrap(new InvalidArgumentProviderException("invalid cryptoKeyVersions path"));
        assertThat(invalid.kind()).isEqualTo(CloudKmsAccessException.Kind.INVALID_ARGUMENT);
        assertThat(invalid.getMessage()).doesNotContain("cryptoKeyVersions");
        CloudKmsAccessException provider = CloudKmsAccessException.wrap(
                new RuntimeException("-----BEGIN EC PRIVATE KEY-----\nfake\n-----END EC PRIVATE KEY-----"));
        assertThat(provider.kind()).isEqualTo(CloudKmsAccessException.Kind.PROVIDER_ERROR);
        assertThat(provider.getMessage()).doesNotContain("PRIVATE KEY");
        assertThat(provider.getCause()).isNull();
    }

    private static final class DeadlineExceededProviderException extends RuntimeException {
        private DeadlineExceededProviderException(String message) {
            super(message);
        }
    }

    private static final class InvalidArgumentProviderException extends RuntimeException {
        private InvalidArgumentProviderException(String message) {
            super(message);
        }
    }

    private static final class UnauthenticatedProviderException extends RuntimeException {
        private UnauthenticatedProviderException(String message) {
            super(message);
        }
    }
}
