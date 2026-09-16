package dev.adellaghmari.boundarymesh.webedge;

final class InvalidSessionException extends RuntimeException {
    InvalidSessionException() {
        super("Authentication required");
    }
}
