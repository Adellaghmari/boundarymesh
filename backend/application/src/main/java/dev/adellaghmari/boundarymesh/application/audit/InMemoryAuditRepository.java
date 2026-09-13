package dev.adellaghmari.boundarymesh.application.audit;

import dev.adellaghmari.boundarymesh.domain.audit.AuditChainHead;
import dev.adellaghmari.boundarymesh.domain.audit.AuditChainId;
import dev.adellaghmari.boundarymesh.domain.audit.AuditEntry;
import dev.adellaghmari.boundarymesh.domain.exception.InvariantViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/** In-memory adapter. Firestore remains PLANNED. Does not survive restart. */
public final class InMemoryAuditRepository implements AuditRepository {

    private final Map<AuditChainId, List<AuditEntry>> chains = new ConcurrentHashMap<>();
    private final Map<AuditChainId, ReentrantLock> locks = new ConcurrentHashMap<>();

    ReentrantLock lockFor(AuditChainId chainId) {
        return locks.computeIfAbsent(chainId, ignored -> new ReentrantLock());
    }

    @Override
    public Optional<AuditChainHead> head(AuditChainId chainId) {
        List<AuditEntry> entries = chains.get(chainId);
        if (entries == null || entries.isEmpty()) {
            return Optional.of(AuditChainHead.empty(chainId));
        }
        AuditEntry last = entries.getLast();
        return Optional.of(new AuditChainHead(chainId, last.sequence(), last.currentHash()));
    }

    @Override
    public AuditEntry appendNext(AuditChainId chainId, AuditEntryFactory factory) {
        ReentrantLock lock = lockFor(chainId);
        lock.lock();
        try {
            AuditChainHead head = head(chainId).orElse(AuditChainHead.empty(chainId));
            AuditEntry entry = factory.create(head);
            append(entry, head);
            return entry;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void append(AuditEntry entry, AuditChainHead expectedHead) {
        ReentrantLock lock = lockFor(entry.chainId());
        lock.lock();
        try {
            AuditChainHead actual = head(entry.chainId()).orElse(AuditChainHead.empty(entry.chainId()));
            if (actual.sequence() != expectedHead.sequence()
                    || !actual.headHash().equals(expectedHead.headHash())) {
                throw new InvariantViolationException(
                        "AUDIT_APPEND_CONFLICT", "audit head changed before append completed");
            }
            chains.computeIfAbsent(entry.chainId(), ignored -> new ArrayList<>())
                    .add(entry);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<AuditEntry> entries(AuditChainId chainId, int limit) {
        List<AuditEntry> all = chains.getOrDefault(chainId, List.of());
        if (all.size() <= limit) {
            return List.copyOf(all);
        }
        return List.copyOf(all.subList(all.size() - limit, all.size()));
    }

    @Override
    public List<AuditChainId> chainIds() {
        return List.copyOf(chains.keySet());
    }

    public void clear() {
        chains.clear();
    }
}
