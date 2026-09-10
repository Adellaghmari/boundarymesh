package dev.adellaghmari.boundarymesh.domain.audit;

import java.util.Objects;

public record AuditVerificationResult(
        AuditChainId chainId,
        AuditVerificationStatus status,
        long entriesChecked,
        Long breakSequence,
        String explanation) {

    public AuditVerificationResult {
        Objects.requireNonNull(chainId, "chainId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(explanation, "explanation");
    }

    public boolean valid() {
        return status == AuditVerificationStatus.VALID;
    }
}
