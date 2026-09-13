package dev.adellaghmari.boundarymesh.application.audit;

import dev.adellaghmari.boundarymesh.domain.audit.AuditChainHead;
import dev.adellaghmari.boundarymesh.domain.audit.AuditEntry;

/**
 * Builds the next audit entry from the head observed inside an atomic append. Must stay pure: no
 * I/O and no repository calls. Implementations may invoke this more than once under contention.
 */
@FunctionalInterface
public interface AuditEntryFactory {

    AuditEntry create(AuditChainHead currentHead);
}
