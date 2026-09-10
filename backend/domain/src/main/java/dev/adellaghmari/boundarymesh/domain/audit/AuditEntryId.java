package dev.adellaghmari.boundarymesh.domain.audit;

import java.util.Objects;
import java.util.UUID;

public record AuditEntryId(String value) {

    public AuditEntryId {
        Objects.requireNonNull(value, "audit entry id");
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("audit entry id");
        }
    }

    public static AuditEntryId random() {
        return new AuditEntryId(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return value;
    }
}
