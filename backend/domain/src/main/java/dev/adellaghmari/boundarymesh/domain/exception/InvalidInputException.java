package dev.adellaghmari.boundarymesh.domain.exception;

public final class InvalidInputException extends DomainException {

    public InvalidInputException(String errorCode, String message) {
        super(errorCode, message);
    }
}
