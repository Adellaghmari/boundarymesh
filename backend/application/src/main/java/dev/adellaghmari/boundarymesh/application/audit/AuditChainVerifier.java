package dev.adellaghmari.boundarymesh.application.audit;

import dev.adellaghmari.boundarymesh.domain.audit.AuditChainId;
import dev.adellaghmari.boundarymesh.domain.audit.AuditEntry;
import dev.adellaghmari.boundarymesh.domain.audit.AuditHash;
import dev.adellaghmari.boundarymesh.domain.audit.AuditVerificationResult;
import dev.adellaghmari.boundarymesh.domain.audit.AuditVerificationStatus;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class AuditChainVerifier {

    public AuditVerificationResult verify(AuditChainId chainId, List<AuditEntry> entries) {
        Objects.requireNonNull(chainId, "chainId");
        Objects.requireNonNull(entries, "entries");
        if (entries.isEmpty()) {
            return new AuditVerificationResult(
                    chainId, AuditVerificationStatus.EMPTY, 0, null, "Chain has no entries yet.");
        }
        List<AuditEntry> ordered = new ArrayList<>(entries);
        ordered.sort(Comparator.comparingLong(AuditEntry::sequence));
        if (isReordered(entries, ordered)) {
            return new AuditVerificationResult(
                    chainId,
                    AuditVerificationStatus.REORDERED,
                    entries.size(),
                    ordered.getFirst().sequence(),
                    "Entries are not in sequence order.");
        }
        AuditHash previous = AuditHash.GENESIS;
        long expectedSequence = 1;
        for (AuditEntry entry : ordered) {
            if (!entry.chainId().equals(chainId)) {
                return new AuditVerificationResult(
                        chainId,
                        AuditVerificationStatus.FOREIGN_CHAIN,
                        expectedSequence - 1,
                        entry.sequence(),
                        "Entry belongs to a different chain.");
            }
            if (entry.sequence() != expectedSequence) {
                return new AuditVerificationResult(
                        chainId,
                        AuditVerificationStatus.SEQUENCE_GAP,
                        expectedSequence - 1,
                        entry.sequence(),
                        "Expected sequence " + expectedSequence + " but found " + entry.sequence() + ".");
            }
            if (!entry.previousHash().equals(previous)) {
                return new AuditVerificationResult(
                        chainId,
                        AuditVerificationStatus.LINK_MISMATCH,
                        expectedSequence - 1,
                        entry.sequence(),
                        "previous_hash does not match the prior entry hash.");
            }
            AuditHash recomputed = AuditAppender.hashOf(entry);
            if (!recomputed.equals(entry.currentHash())) {
                return new AuditVerificationResult(
                        chainId,
                        AuditVerificationStatus.HASH_MISMATCH,
                        expectedSequence - 1,
                        entry.sequence(),
                        "Recalculated SHA-256 does not match stored current_hash.");
            }
            previous = entry.currentHash();
            expectedSequence++;
        }
        return new AuditVerificationResult(
                chainId,
                AuditVerificationStatus.VALID,
                ordered.size(),
                null,
                "Sequence, previous-hash linkage, and SHA-256 hashes match.");
    }

    private static boolean isReordered(List<AuditEntry> original, List<AuditEntry> sorted) {
        if (original.size() != sorted.size()) {
            return true;
        }
        for (int i = 0; i < original.size(); i++) {
            if (original.get(i).sequence() != sorted.get(i).sequence()) {
                return true;
            }
        }
        return false;
    }
}
