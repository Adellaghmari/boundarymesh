package dev.adellaghmari.boundarymesh.application.canon;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class Rfc8785CanonicalizerTest {

    @Test
    void sortsObjectMembersAndOmitsWhitespace() {
        Map<String, Object> left = new LinkedHashMap<>();
        left.put("b", 2L);
        left.put("a", 1L);
        Map<String, Object> right = Map.of("a", 1L, "b", 2L);
        assertThat(Rfc8785Canonicalizer.canonicalizeToString(left)).isEqualTo("{\"a\":1,\"b\":2}");
        assertThat(Rfc8785Canonicalizer.canonicalize(left)).isEqualTo(Rfc8785Canonicalizer.canonicalize(right));
    }

    @Test
    void stringEscapesAreStable() {
        assertThat(Rfc8785Canonicalizer.canonicalizeToString("a\"b\\c")).isEqualTo("\"a\\\"b\\\\c\"");
        assertThat(Rfc8785Canonicalizer.canonicalizeToString("\u0001")).isEqualTo("\"\\u0001\"");
    }

    @Test
    void arraysPreserveOrder() {
        assertThat(Rfc8785Canonicalizer.canonicalizeToString(List.of("b", "a"))).isEqualTo("[\"b\",\"a\"]");
    }

    @Test
    void utf8BytesAreCanonical() {
        byte[] bytes = Rfc8785Canonicalizer.canonicalize(Map.of("x", "ok"));
        assertThat(new String(bytes, StandardCharsets.UTF_8)).isEqualTo("{\"x\":\"ok\"}");
    }

    @Test
    void floatingPointValuesAreRejected() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> Rfc8785Canonicalizer.canonicalize(Map.of("n", 1.5)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
