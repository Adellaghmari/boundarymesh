package dev.adellaghmari.boundarymesh.gcp.firestore;

import dev.adellaghmari.boundarymesh.application.audit.AuditEntryFactory;
import dev.adellaghmari.boundarymesh.application.audit.AuditRepository;
import dev.adellaghmari.boundarymesh.domain.audit.AuditChainHead;
import dev.adellaghmari.boundarymesh.domain.audit.AuditChainId;
import dev.adellaghmari.boundarymesh.domain.audit.AuditEntry;
import dev.adellaghmari.boundarymesh.domain.exception.InvariantViolationException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Firestore audit adapter. Head read, entry create, and head advancement commit in one transaction.
 * No process-local lock.
 */
public final class FirestoreAuditRepository implements AuditRepository {

    private final FirestoreFacade firestore;

    public FirestoreAuditRepository(FirestoreFacade firestore) {
        this.firestore = Objects.requireNonNull(firestore, "firestore");
    }

    @Override
    public Optional<AuditChainHead> head(AuditChainId chainId) {
        return firestore
                .get(FirestoreCollections.auditHead(chainId))
                .map(FirestoreCodecs::auditHead)
                .or(() -> Optional.of(AuditChainHead.empty(chainId)));
    }

    @Override
    public void append(AuditEntry entry, AuditChainHead expectedHead) {
        firestore.runTransaction(transaction -> {
            commit(transaction, entry, expectedHead);
            return null;
        });
    }

    @Override
    public AuditEntry appendNext(AuditChainId chainId, AuditEntryFactory factory) {
        return firestore.runTransaction(transaction -> {
            AuditChainHead head = transaction
                    .get(FirestoreCollections.auditHead(chainId))
                    .map(FirestoreCodecs::auditHead)
                    .orElse(AuditChainHead.empty(chainId));
            AuditEntry entry = factory.create(head);
            commit(transaction, entry, head);
            return entry;
        });
    }

    @Override
    public List<AuditEntry> entries(AuditChainId chainId, int limit) {
        List<AuditEntry> all = new ArrayList<>();
        for (var row : firestore.listSubcollection(
                FirestoreCollections.auditHead(chainId), FirestoreCollections.AUDIT_ENTRIES, "sequence", false, 0)) {
            all.add(FirestoreCodecs.auditEntry(row));
        }
        all.sort(Comparator.comparingLong(AuditEntry::sequence));
        if (all.size() <= limit) {
            return List.copyOf(all);
        }
        return List.copyOf(all.subList(all.size() - limit, all.size()));
    }

    @Override
    public List<AuditChainId> chainIds() {
        return firestore.listDocumentIds(FirestoreCollections.AUDIT_CHAINS).stream()
                .map(AuditChainId::new)
                .toList();
    }

    private static void commit(FirestoreTransaction transaction, AuditEntry entry, AuditChainHead expectedHead) {
        AuditChainHead actual = transaction
                .get(FirestoreCollections.auditHead(entry.chainId()))
                .map(FirestoreCodecs::auditHead)
                .orElse(AuditChainHead.empty(entry.chainId()));
        if (actual.sequence() != expectedHead.sequence() || !actual.headHash().equals(expectedHead.headHash())) {
            throw new InvariantViolationException(
                    "AUDIT_APPEND_CONFLICT", "audit head changed before append completed");
        }
        if (entry.sequence() != actual.sequence() + 1 || !entry.previousHash().equals(actual.headHash())) {
            throw new InvariantViolationException("AUDIT_APPEND_CONFLICT", "audit entry does not continue the head");
        }
        transaction.create(
                FirestoreCollections.auditEntry(entry.chainId(), entry.sequence()), FirestoreCodecs.auditEntry(entry));
        transaction.set(FirestoreCollections.auditHead(entry.chainId()), FirestoreCodecs.auditHead(entry));
    }
}
