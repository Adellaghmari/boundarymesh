package dev.adellaghmari.boundarymesh.application.outbox;

/** Publisher failure. Message is a classification only. */
public final class MessagePublishException extends RuntimeException {

    private final PublishFailureKind kind;

    public MessagePublishException(PublishFailureKind kind, String classification) {
        super(classification);
        this.kind = kind;
    }

    public PublishFailureKind kind() {
        return kind;
    }
}
