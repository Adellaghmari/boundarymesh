package dev.adellaghmari.boundarymesh.domain;

import dev.adellaghmari.boundarymesh.domain.exception.InvariantViolationException;
import java.util.List;
import java.util.Objects;

/**
 * Deterministic evidence consumed by the Boundary Enforcement Engine. Phase 1 models results only;
 * JWT/HMAC/Graph checks are not performed here.
 */
public record ControlVector(
        ControlResult identity,
        ControlResult authorization,
        ControlResult authenticity,
        ControlResult freshness,
        ControlResult replay,
        ControlResult contract,
        ControlResult egress,
        ControlResult credentialResolvability,
        ControlResult connectorSafety) {

    public ControlVector {
        requireKind(identity, ControlKind.IDENTITY);
        requireKind(authorization, ControlKind.AUTHORIZATION);
        requireKind(authenticity, ControlKind.AUTHENTICITY);
        requireKind(freshness, ControlKind.FRESHNESS);
        requireKind(replay, ControlKind.REPLAY);
        requireKind(contract, ControlKind.CONTRACT);
        requireKind(egress, ControlKind.EGRESS);
        requireKind(credentialResolvability, ControlKind.CREDENTIAL_RESOLVABILITY);
        requireKind(connectorSafety, ControlKind.CONNECTOR_SAFETY);
    }

    public static ControlVector of(
            ControlResult identity,
            ControlResult authorization,
            ControlResult authenticity,
            ControlResult freshness,
            ControlResult replay,
            ControlResult contract,
            ControlResult egress,
            ControlResult credentialResolvability,
            ControlResult connectorSafety) {
        return new ControlVector(
                identity,
                authorization,
                authenticity,
                freshness,
                replay,
                contract,
                egress,
                credentialResolvability,
                connectorSafety);
    }

    public static ControlVector allPass() {
        return new ControlVector(
                ControlResult.pass(ControlKind.IDENTITY),
                ControlResult.pass(ControlKind.AUTHORIZATION),
                ControlResult.pass(ControlKind.AUTHENTICITY),
                ControlResult.pass(ControlKind.FRESHNESS),
                ControlResult.pass(ControlKind.REPLAY),
                ControlResult.pass(ControlKind.CONTRACT),
                ControlResult.pass(ControlKind.EGRESS),
                ControlResult.pass(ControlKind.CREDENTIAL_RESOLVABILITY),
                ControlResult.pass(ControlKind.CONNECTOR_SAFETY));
    }

    public List<ControlResult> asList() {
        return List.of(
                identity,
                authorization,
                authenticity,
                freshness,
                replay,
                contract,
                egress,
                credentialResolvability,
                connectorSafety);
    }

    public List<ControlResult> securityControls() {
        return List.of(identity, authorization, authenticity, freshness, replay);
    }

    public ControlVector withIdentity(ControlResult identity) {
        return new ControlVector(
                identity,
                authorization,
                authenticity,
                freshness,
                replay,
                contract,
                egress,
                credentialResolvability,
                connectorSafety);
    }

    public ControlVector withAuthorization(ControlResult authorization) {
        return new ControlVector(
                identity,
                authorization,
                authenticity,
                freshness,
                replay,
                contract,
                egress,
                credentialResolvability,
                connectorSafety);
    }

    public ControlVector withAuthenticity(ControlResult authenticity) {
        return new ControlVector(
                identity,
                authorization,
                authenticity,
                freshness,
                replay,
                contract,
                egress,
                credentialResolvability,
                connectorSafety);
    }

    public ControlVector withFreshness(ControlResult freshness) {
        return new ControlVector(
                identity,
                authorization,
                authenticity,
                freshness,
                replay,
                contract,
                egress,
                credentialResolvability,
                connectorSafety);
    }

    public ControlVector withReplay(ControlResult replay) {
        return new ControlVector(
                identity,
                authorization,
                authenticity,
                freshness,
                replay,
                contract,
                egress,
                credentialResolvability,
                connectorSafety);
    }

    public ControlVector withContract(ControlResult contract) {
        return new ControlVector(
                identity,
                authorization,
                authenticity,
                freshness,
                replay,
                contract,
                egress,
                credentialResolvability,
                connectorSafety);
    }

    public ControlVector withEgress(ControlResult egress) {
        return new ControlVector(
                identity,
                authorization,
                authenticity,
                freshness,
                replay,
                contract,
                egress,
                credentialResolvability,
                connectorSafety);
    }

    public ControlVector withCredentialResolvability(ControlResult credentialResolvability) {
        return new ControlVector(
                identity,
                authorization,
                authenticity,
                freshness,
                replay,
                contract,
                egress,
                credentialResolvability,
                connectorSafety);
    }

    public ControlVector withConnectorSafety(ControlResult connectorSafety) {
        return new ControlVector(
                identity,
                authorization,
                authenticity,
                freshness,
                replay,
                contract,
                egress,
                credentialResolvability,
                connectorSafety);
    }

    private static void requireKind(ControlResult result, ControlKind expected) {
        Objects.requireNonNull(result, expected.name());
        if (result.kind() != expected) {
            throw new InvariantViolationException(
                    "CONTROL_KIND_MISMATCH", "expected " + expected + " but was " + result.kind());
        }
    }
}
