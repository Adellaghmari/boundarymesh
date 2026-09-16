package dev.adellaghmari.boundarymesh.webedge;

final class CoreUnavailableException extends RuntimeException {
    CoreUnavailableException() {
        super("Core API unavailable");
    }
}
