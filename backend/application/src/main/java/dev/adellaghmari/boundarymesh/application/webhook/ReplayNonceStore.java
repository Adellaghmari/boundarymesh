package dev.adellaghmari.boundarymesh.application.webhook;

import java.time.Instant;

/** Security replay store. Future Firestore adapter can implement this port. */
public interface ReplayNonceStore {

    /**
     * Atomically records {@code nonceHash} if absent.
     *
     * @return {@code true} if this is the first observation
     */
    boolean recordIfAbsent(String nonceHash, Instant expiresAt, Instant now);
}
