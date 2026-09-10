package dev.adellaghmari.boundarymesh.application.consent;

import dev.adellaghmari.boundarymesh.domain.ReasonCode;
import dev.adellaghmari.boundarymesh.domain.consent.ConsentGrant;
import dev.adellaghmari.boundarymesh.domain.consent.ConsentStatus;
import java.util.Objects;

public record ConsentEvaluation(ConsentStatus status, ReasonCode reason, String explanation, ConsentGrant grant) {

    public ConsentEvaluation {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(explanation, "explanation");
    }

    public boolean blocksCrossing() {
        return status != ConsentStatus.GRANTED;
    }
}
