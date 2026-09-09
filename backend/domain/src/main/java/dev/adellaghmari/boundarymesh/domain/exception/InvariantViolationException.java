package dev.adellaghmari.boundarymesh.domain.exception;

public final class InvariantViolationException extends DomainException {

    public InvariantViolationException(String errorCode, String message) {
        super(errorCode, message);
    }
}
