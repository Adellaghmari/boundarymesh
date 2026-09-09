package dev.adellaghmari.boundarymesh.domain.exception;

/**
 * Expected domain failures that are not enforcement outcomes. Security results stay on
 * {@code BoundaryDecision}, not on the stack.
 */
public abstract class DomainException extends RuntimeException {

    private final String errorCode;

    protected DomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String errorCode() {
        return errorCode;
    }
}
