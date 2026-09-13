package dev.adellaghmari.boundarymesh.application.outbox;

/** Safe classification only. Never store exception bodies, payloads, or secrets. */
public enum PublishFailureKind {
    TRANSIENT,
    PERMANENT,
    MARK_FAILED
}
