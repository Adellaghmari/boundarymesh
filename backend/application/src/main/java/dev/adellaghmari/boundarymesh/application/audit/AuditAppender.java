package dev.adellaghmari.boundarymesh.application.audit;

import dev.adellaghmari.boundarymesh.application.canon.Rfc8785Canonicalizer;
import dev.adellaghmari.boundarymesh.application.canon.Sha256;
import dev.adellaghmari.boundarymesh.domain.audit.AuditChainId;
import dev.adellaghmari.boundarymesh.domain.audit.AuditEntry;
import dev.adellaghmari.boundarymesh.domain.audit.AuditEntryId;
import dev.adellaghmari.boundarymesh.domain.audit.AuditEventType;
import dev.adellaghmari.boundarymesh.domain.audit.AuditHash;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public final class AuditAppender {

    private final AuditRepository repository;
    private final Clock clock;

    public AuditAppender(AuditRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository);
        this.clock = Objects.requireNonNull(clock);
    }

    public AuditEntry append(AuditChainId chainId, AuditEventType type, Map<String, String> attributes) {
        Map<String, String> safeAttributes = attributes == null ? Map.of() : attributes;
        return repository.appendNext(chainId, head -> {
            Instant now = clock.instant();
            AuditEntry unsigned = new AuditEntry(
                    AuditEntryId.random(),
                    chainId,
                    head.sequence() + 1,
                    now,
                    type,
                    safeAttributes,
                    head.headHash(),
                    AuditHash.GENESIS);
            return unsigned.withCurrentHash(hashOf(unsigned));
        });
    }

    public static AuditHash hashOf(AuditEntry entry) {
        return new AuditHash(Sha256.hex(Rfc8785Canonicalizer.canonicalize(canonicalMap(entry))));
    }

    public static Map<String, Object> canonicalMap(AuditEntry entry) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("attributes", new TreeMap<>(entry.attributes()));
        body.put("chain_id", entry.chainId().value());
        body.put("event_type", entry.eventType().name());
        body.put("occurred_at", entry.occurredAt().toString());
        body.put("previous_hash", entry.previousHash().hex());
        body.put("schema_version", AuditEntry.SCHEMA_VERSION);
        body.put("sequence", entry.sequence());
        return body;
    }
}
