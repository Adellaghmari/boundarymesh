package dev.adellaghmari.boundarymesh.gcp.secret;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SecretResourceNameTest {

    @Test
    void rejectsMalformedCoordinates() {
        assertThatThrownBy(() -> new SecretResourceName(" ", "covecrm-api", "1"))
                .isInstanceOf(SecretAccessException.class)
                .extracting(error -> ((SecretAccessException) error).kind())
                .isEqualTo(SecretAccessFailureKind.INVALID_REFERENCE);
        assertThatThrownBy(() -> new SecretResourceName("boundarymesh-dev", "../etc/passwd", "1"))
                .isInstanceOf(SecretAccessException.class);
        assertThatThrownBy(() -> new SecretResourceName("boundarymesh-dev", "covecrm-api", "latest-ish"))
                .isInstanceOf(SecretAccessException.class);
    }

    @Test
    void formatsStableResourceName() {
        assertThat(SecretResourceName.latest("boundarymesh-dev", "hmac-quayforge")
                        .resourceName())
                .isEqualTo("projects/boundarymesh-dev/secrets/hmac-quayforge/versions/latest");
    }
}
