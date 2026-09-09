package dev.adellaghmari.boundarymesh.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/** SHA-256 hex reference. This is hashing, not encryption. */
public final class Sha256Reference {

    private Sha256Reference() {}

    public static String ofUtf8(String value) {
        return ofBytes(Objects.requireNonNull(value, "value").getBytes(StandardCharsets.UTF_8));
    }

    public static String ofBytes(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required", ex);
        }
    }
}
