package dev.adellaghmari.boundarymesh.application.credential;

import java.util.Arrays;
import java.util.Objects;

/**
 * Bounded secret handle for just-in-time connector use. Not a domain record. Never logged.
 */
public final class SecretMaterial implements AutoCloseable {

    private final char[] value;

    public SecretMaterial(char[] value) {
        Objects.requireNonNull(value, "value");
        this.value = Arrays.copyOf(value, value.length);
        Arrays.fill(value, '\0');
    }

    public static SecretMaterial fromPlain(String value) {
        return new SecretMaterial(value.toCharArray());
    }

    public String reveal() {
        return new String(value);
    }

    @Override
    public void close() {
        Arrays.fill(value, '\0');
    }

    @Override
    public String toString() {
        return "SecretMaterial[redacted]";
    }
}
