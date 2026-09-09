package dev.adellaghmari.boundarymesh.application.webhook;

import dev.adellaghmari.boundarymesh.domain.BoundaryDecision;
import dev.adellaghmari.boundarymesh.domain.ControlVector;

public record WebhookDefenseResult(
        BoundaryDecision decision, ControlVector vector, boolean duplicateDelivery, String eventId) {}
