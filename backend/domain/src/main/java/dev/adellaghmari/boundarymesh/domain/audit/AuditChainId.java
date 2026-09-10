package dev.adellaghmari.boundarymesh.domain.audit;

import dev.adellaghmari.boundarymesh.domain.exception.InvalidInputException;
import java.util.Objects;
import java.util.regex.Pattern;

/** One hash chain per integration. Avoids a global append hotspot. */
public record AuditChainId(String value) {

    private static final Pattern PATTERN = Pattern.compile("^[a-z0-9][a-z0-9-]{1,63}$");

    public AuditChainId {
        Objects.requireNonNull(value, "audit chain id");
        value = value.trim();
        if (!PATTERN.matcher(value).matches()) {
            throw new InvalidInputException("AUDIT_CHAIN_ID_INVALID", "audit chain id must be a lowercase slug");
        }
    }

    public static AuditChainId ofIntegration(String integrationId) {
        return new AuditChainId(integrationId);
    }

    @Override
    public String toString() {
        return value;
    }
}
