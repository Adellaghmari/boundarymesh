package dev.adellaghmari.boundarymesh.application.webhook;

import dev.adellaghmari.boundarymesh.domain.BoundaryDecision;
import java.util.Optional;

/** Delivery idempotency, separate from replay defense. */
public interface IdempotencyStore {

    Optional<BoundaryDecision> find(String eventKey);

    void putIfAbsent(String eventKey, BoundaryDecision decision);
}
