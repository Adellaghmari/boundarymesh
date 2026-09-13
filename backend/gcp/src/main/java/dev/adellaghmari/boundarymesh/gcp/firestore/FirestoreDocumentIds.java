package dev.adellaghmari.boundarymesh.gcp.firestore;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Stable Firestore document IDs. Never embeds secrets. */
public final class FirestoreDocumentIds {

    private static final Pattern SAFE = Pattern.compile("^[A-Za-z0-9._:~-]{1,1500}$");

    private FirestoreDocumentIds() {}

    public static String requireSafe(String raw) {
        Objects.requireNonNull(raw, "document id");
        String trimmed = raw.trim();
        if (trimmed.isBlank() || ".".equals(trimmed) || "..".equals(trimmed)) {
            throw new IllegalArgumentException("Firestore document id is invalid");
        }
        if (SAFE.matcher(trimmed).matches()) {
            return trimmed;
        }
        return "h_" + sha256Hex(trimmed);
    }

    public static String sequence(long sequence) {
        if (sequence < 1) {
            throw new IllegalArgumentException("sequence must be >= 1");
        }
        return String.format(Locale.ROOT, "%020d", sequence);
    }

    static String sha256Hex(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
