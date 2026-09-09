package dev.adellaghmari.boundarymesh.domain;

/**
 * Deterministic result of one control. Never a probability or AI score.
 *
 * <ul>
 *   <li>{@link #PASS} — the control completed and the policy condition was satisfied.
 *   <li>{@link #FAIL} — the control completed and the policy condition was not satisfied.
 *   <li>{@link #NOT_APPLICABLE} — this control does not apply to the ingress type (does not
 *       contribute to the outcome).
 *   <li>{@link #UNAVAILABLE} — the control could not be evaluated (missing JWKS, secret, and so
 *       on). Required security controls that are unavailable fail closed to {@code REJECT}.
 * </ul>
 */
public enum ControlStatus {
    PASS,
    FAIL,
    NOT_APPLICABLE,
    UNAVAILABLE
}
