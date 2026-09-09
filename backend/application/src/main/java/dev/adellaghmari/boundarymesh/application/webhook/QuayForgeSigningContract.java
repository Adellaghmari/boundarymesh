package dev.adellaghmari.boundarymesh.application.webhook;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * QuayForge signed-webhook contract.
 *
 * <p>Headers: {@code X-Velora-Connector-Id}, {@code X-Velora-Timestamp}, {@code X-Velora-Nonce},
 * {@code X-Velora-Signature}.
 *
 * <p>Canonical UTF-8 string:
 *
 * <pre>
 * v1:{timestamp}:{nonce}:{sha256hex(rawBody)}
 * </pre>
 *
 * HMAC-SHA256 over that string. Header value: {@code v1=<lowercase-hex>}.
 *
 * <p>The raw request body bytes are hashed; JSON is never reserialized before verification.
 */
public final class QuayForgeSigningContract {

    public static final String CONTRACT_VERSION_HEADER = "X-Velora-Contract-Version";
    public static final String CONNECTOR_HEADER = "X-Velora-Connector-Id";
    public static final String TIMESTAMP_HEADER = "X-Velora-Timestamp";
    public static final String NONCE_HEADER = "X-Velora-Nonce";
    public static final String SIGNATURE_HEADER = "X-Velora-Signature";
    public static final String VERSION_PREFIX = "v1=";
    public static final String ALGORITHM = "HmacSHA256";

    private QuayForgeSigningContract() {}

    public static byte[] canonicalBytes(String timestamp, String nonce, byte[] rawBody) {
        String bodyHash = HexFormat.of().formatHex(sha256(rawBody));
        return ("v1:" + timestamp + ":" + nonce + ":" + bodyHash).getBytes(StandardCharsets.UTF_8);
    }

    public static String signatureHeader(byte[] secret, String timestamp, String nonce, byte[] rawBody) {
        return VERSION_PREFIX + HexFormat.of().formatHex(hmac(secret, canonicalBytes(timestamp, nonce, rawBody)));
    }

    public static boolean signaturesEqual(String receivedHeader, String expectedHeader) {
        byte[] left = receivedHeader.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
        byte[] right = expectedHeader.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(left, right);
    }

    public static boolean hasSignatureFormat(String header) {
        return header != null
                && header.regionMatches(true, 0, VERSION_PREFIX, 0, VERSION_PREFIX.length())
                && header.length() == VERSION_PREFIX.length() + 64
                && header.substring(VERSION_PREFIX.length()).matches("[0-9a-fA-F]{64}");
    }

    public static byte[] hmac(byte[] secret, byte[] canonical) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret, ALGORITHM));
            return mac.doFinal(canonical);
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("HMAC-SHA256 is required", ex);
        }
    }

    private static byte[] sha256(byte[] rawBody) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(rawBody);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required", ex);
        }
    }
}
