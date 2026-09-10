package dev.adellaghmari.boundarymesh.domain.audit;

import java.util.Objects;

public record AuditChainHead(AuditChainId chainId, long sequence, AuditHash headHash) {

    public AuditChainHead {
        Objects.requireNonNull(chainId, "chainId");
        Objects.requireNonNull(headHash, "headHash");
        if (sequence < 0) {
            throw new IllegalArgumentException("sequence");
        }
    }

    public static AuditChainHead empty(AuditChainId chainId) {
        return new AuditChainHead(chainId, 0, AuditHash.GENESIS);
    }
}
