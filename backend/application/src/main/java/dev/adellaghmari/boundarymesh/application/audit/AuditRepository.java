package dev.adellaghmari.boundarymesh.application.audit;

import dev.adellaghmari.boundarymesh.domain.audit.AuditChainHead;
import dev.adellaghmari.boundarymesh.domain.audit.AuditChainId;
import dev.adellaghmari.boundarymesh.domain.audit.AuditEntry;
import java.util.List;
import java.util.Optional;

public interface AuditRepository {

    Optional<AuditChainHead> head(AuditChainId chainId);

    void append(AuditEntry entry, AuditChainHead expectedHead);

    List<AuditEntry> entries(AuditChainId chainId, int limit);

    List<AuditChainId> chainIds();
}
