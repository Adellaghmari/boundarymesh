package dev.adellaghmari.boundarymesh.domain.audit;

import dev.adellaghmari.boundarymesh.domain.exception.InvalidInputException;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** SHA-256 digest encoded as lowercase hex. Not a claim of immutability. */
public record AuditHash(String hex) {

    private static final Pattern HEX = Pattern.compile("^[0-9a-f]{64}$");
    public static final AuditHash GENESIS = new AuditHash("0".repeat(64));

    public AuditHash {
        Objects.requireNonNull(hex, "audit hash");
        hex = hex.trim().toLowerCase(Locale.ROOT);
        if (!HEX.matcher(hex).matches()) {
            throw new InvalidInputException("AUDIT_HASH_INVALID", "audit hash must be 64 lowercase hex characters");
        }
    }

    public String shortened() {
        return hex.substring(0, 12);
    }

    @Override
    public String toString() {
        return hex;
    }
}
