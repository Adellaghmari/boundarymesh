package dev.adellaghmari.boundarymesh.domain.exception;

/**
 * Placeholder for later Firestore/Pub/Sub/KMS adapter failures. Not used as an enforcement
 * outcome.
 */
public final class InfrastructureException extends DomainException {

    public InfrastructureException(String errorCode, String message) {
        super(errorCode, message);
    }

    public InfrastructureException(String errorCode, String message, Throwable cause) {
        super(errorCode, message);
        initCause(cause);
    }
}
