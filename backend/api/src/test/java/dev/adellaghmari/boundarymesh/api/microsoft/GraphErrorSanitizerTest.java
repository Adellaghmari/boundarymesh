package dev.adellaghmari.boundarymesh.api.microsoft;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class GraphErrorSanitizerTest {

    @Test
    void extractsCanonicalGraphCode() {
        assertThat(GraphErrorSanitizer.code("{\"error\":{\"code\":\"InvalidAuthenticationToken\"}}"))
                .isEqualTo("InvalidAuthenticationToken");
        assertThat(GraphErrorSanitizer.category(401, "InvalidAuthenticationToken", false))
                .isEqualTo("INVALID_AUTHENTICATION_TOKEN");
    }

    @Test
    void empty401IsEmptyBodyCategory() {
        assertThat(GraphErrorSanitizer.code("")).isNull();
        assertThat(GraphErrorSanitizer.category(401, null, true)).isEqualTo("EMPTY_BODY");
    }

    @Test
    void detectsWwwAuthenticateWithoutCopyingValue() {
        assertThat(GraphErrorSanitizer.wwwAuthenticatePresent(List.of("Content-Type", "WWW-Authenticate")))
                .isTrue();
        assertThat(GraphErrorSanitizer.wwwAuthenticatePresent(List.of("Retry-After")))
                .isFalse();
    }
}
