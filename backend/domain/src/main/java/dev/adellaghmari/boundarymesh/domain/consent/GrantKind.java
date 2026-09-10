package dev.adellaghmari.boundarymesh.domain.consent;

/**
 * Future Microsoft Entra/Graph mapping: APPLICATION ≈ app-only / app-role; DELEGATED ≈ delegated
 * scope. The generic core does not call Entra or Graph.
 */
public enum GrantKind {
    APPLICATION,
    DELEGATED
}
