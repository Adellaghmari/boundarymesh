package dev.adellaghmari.boundarymesh.api;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ApiClasspathTest {

    @Test
    void publicApiDoesNotLoadCamel() {
        assertThatThrownBy(() -> Class.forName("org.apache.camel.CamelContext"))
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void publicApiDoesNotLoadResilience4j() {
        assertThatThrownBy(() -> Class.forName("io.github.resilience4j.retry.Retry"))
                .isInstanceOf(ClassNotFoundException.class);
    }
}
