package dev.adellaghmari.boundarymesh.domain;

/**
 * Deterministic enforcement outcome. Not an AI recommendation and not a release policy gate.
 *
 * <ul>
 *   <li>{@link #REJECT} — the transaction must not cross. Security verification failed or a hard
 *       policy denial occurred.
 *   <li>{@link #QUARANTINE} — withheld for a recoverable operational reason (unsupported contract,
 *       credential missing, connector unsafe). Not a security reject.
 *   <li>{@link #ALLOW_WITH_TRANSFORMATION} — may cross only after deterministic transformation
 *       (for example approved redaction).
 *   <li>{@link #ALLOW} — may continue unchanged under current policy.
 * </ul>
 */
public enum BoundaryOutcome {
    ALLOW,
    ALLOW_WITH_TRANSFORMATION,
    QUARANTINE,
    REJECT;

    public boolean crossesBoundary() {
        return this == ALLOW || this == ALLOW_WITH_TRANSFORMATION;
    }
}
