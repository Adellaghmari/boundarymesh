package dev.adellaghmari.boundarymesh.domain.audit;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * One hash-chained audit record. Safe metadata only. The local store is mutable; verification
 * detects retrospective modification.
 */
public record AuditEntry(
        AuditEntryId id,
        AuditChainId chainId,
        long sequence,
        Instant occurredAt,
        AuditEventType eventType,
        Map<String, String> attributes,
        AuditHash previousHash,
        AuditHash currentHash) {

    public static final String SCHEMA_VERSION = "audit-entry/v1";

    public AuditEntry {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(chainId, "chainId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(attributes, "attributes");
        Objects.requireNonNull(previousHash, "previousHash");
        Objects.requireNonNull(currentHash, "currentHash");
        if (sequence < 1) {
            throw new IllegalArgumentException("sequence must be >= 1");
        }
        attributes = Map.copyOf(new TreeMap<>(attributes));
    }

    public AuditEntry withCurrentHash(AuditHash hash) {
        return new AuditEntry(id, chainId, sequence, occurredAt, eventType, attributes, previousHash, hash);
    }
}
