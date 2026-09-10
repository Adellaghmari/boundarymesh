package dev.adellaghmari.boundarymesh.application.crossing;

import dev.adellaghmari.boundarymesh.application.contract.ContractEvaluator;
import dev.adellaghmari.boundarymesh.application.crossing.CrossingComposer.CrossingCommand;
import dev.adellaghmari.boundarymesh.domain.ControlKind;
import dev.adellaghmari.boundarymesh.domain.ControlResult;
import dev.adellaghmari.boundarymesh.domain.exception.InvalidInputException;
import java.util.Locale;
import java.util.UUID;

public final class CrossingScenarios {

    private CrossingScenarios() {}

    public static CrossingCommand contract(String scenario) {
        String eventId = "evt-" + UUID.randomUUID();
        return switch (normalize(scenario)) {
            case "valid-v1", "valid" -> command("v1", QuayForgeDemoPayloads.validV1(eventId), "RIVERTIDE", false);
            case "valid-v2" -> command("v2", QuayForgeDemoPayloads.v2Shape(eventId), "RIVERTIDE", false);
            case "missing-required" ->
                command("v1", QuayForgeDemoPayloads.missingRequired(eventId), "RIVERTIDE", false);
            case "wrong-type" -> command("v1", QuayForgeDemoPayloads.wrongType(eventId), "RIVERTIDE", false);
            case "unsupported-version" -> command("v3", QuayForgeDemoPayloads.validV1(eventId), "RIVERTIDE", false);
            case "contract-drift" -> command("v1", QuayForgeDemoPayloads.v2Shape(eventId), "RIVERTIDE", false);
            case "too-many-violations" ->
                command("v1", QuayForgeDemoPayloads.manyViolations(eventId), "RIVERTIDE", false);
            case "contract-unavailable" -> command("v1", QuayForgeDemoPayloads.validV1(eventId), "RIVERTIDE", true);
            default -> unknown(scenario);
        };
    }

    public static CrossingCommand egress(String destination, String scenario) {
        String dest = destination == null || destination.isBlank()
                ? "RIVERTIDE"
                : destination.trim().toUpperCase(Locale.ROOT);
        String eventId = "evt-" + UUID.randomUUID();
        return switch (normalize(scenario)) {
            case "allow", "all-permitted" -> command("v1", QuayForgeDemoPayloads.validV1(eventId), dest, false);
            case "email-only" -> command("v1", QuayForgeDemoPayloads.emailOnlyV1(eventId), dest, false);
            case "redact", "email-redaction" -> command("v1", QuayForgeDemoPayloads.transformV1(eventId), dest, false);
            case "deny", "forbidden-field" -> command("v1", QuayForgeDemoPayloads.denyV1(eventId), dest, false);
            case "nested" -> command("v1", QuayForgeDemoPayloads.nestedV1(eventId), dest, false);
            case "implicit-deny" -> command("v1", QuayForgeDemoPayloads.implicitV1(eventId), dest, false);
            case "minimization" -> command("v1", QuayForgeDemoPayloads.minimizationV1(eventId), dest, false);
            case "policy-unavailable" -> command("v1", QuayForgeDemoPayloads.validV1(eventId), "UNKNOWN-DEST", false);
            default -> unknown(scenario);
        };
    }

    public static CrossingCommand composed(String scenario) {
        String eventId = "evt-" + UUID.randomUUID();
        return switch (normalize(scenario)) {
            case "composed-allow", "all-pass" ->
                command("v1", QuayForgeDemoPayloads.validV1(eventId), "RIVERTIDE", false);
            case "composed-transform", "egress-redaction" ->
                command("v1", QuayForgeDemoPayloads.transformV1(eventId), "RIVERTIDE", false);
            case "composed-quarantine", "contract-drift" ->
                command("v1", QuayForgeDemoPayloads.v2Shape(eventId), "RIVERTIDE", false);
            case "composed-reject-egress", "forbidden-field" ->
                command("v1", QuayForgeDemoPayloads.denyV1(eventId), "RIVERTIDE", false);
            case "composed-reject-schema" ->
                command("v1", QuayForgeDemoPayloads.missingRequired(eventId), "RIVERTIDE", false);
            case "composed-unsupported" -> command("v3", QuayForgeDemoPayloads.validV1(eventId), "RIVERTIDE", false);
            default -> unknown(scenario);
        };
    }

    public static CrossingCommand delivery(String scenario) {
        String eventId = "evt-" + UUID.randomUUID();
        return switch (normalize(scenario)) {
            case "transformed-delivery", "success" ->
                command("v1", QuayForgeDemoPayloads.transformV1(eventId), "LUMENWAY", false)
                        .withProfiles("active", "valid", "closed");
            case "lumenway-translate" ->
                command("v1", QuayForgeDemoPayloads.transformV1(eventId), "LUMENWAY", false)
                        .withProfiles("active", "valid", "closed");
            case "rivertide-strict" ->
                command("v1", QuayForgeDemoPayloads.transformV1(eventId), "RIVERTIDE", false)
                        .withProfiles("active", "valid", "closed");
            case "credential-missing" ->
                command("v1", QuayForgeDemoPayloads.transformV1(eventId), "COVECRM", false)
                        .withProfiles("missing", "valid", "closed");
            case "credential-revoked" ->
                command("v1", QuayForgeDemoPayloads.transformV1(eventId), "COVECRM", false)
                        .withProfiles("revoked", "valid", "closed");
            case "credential-rotated" ->
                command("v1", QuayForgeDemoPayloads.transformV1(eventId), "COVECRM", false)
                        .withProfiles("rotated", "valid", "closed");
            case "credential-unavailable" ->
                command("v1", QuayForgeDemoPayloads.transformV1(eventId), "COVECRM", false)
                        .withProfiles("unavailable", "valid", "closed");
            case "consent-missing" ->
                command("v1", QuayForgeDemoPayloads.transformV1(eventId), "RIVERTIDE", false)
                        .withProfiles("active", "missing", "closed");
            case "consent-revoked" ->
                command("v1", QuayForgeDemoPayloads.transformV1(eventId), "RIVERTIDE", false)
                        .withProfiles("active", "revoked", "closed");
            case "circuit-open" ->
                command("v1", QuayForgeDemoPayloads.transformV1(eventId), "COVECRM", false)
                        .withProfiles("active", "valid", "open");
            case "reject-egress" ->
                command("v1", QuayForgeDemoPayloads.denyV1(eventId), "COVECRM", false)
                        .withProfiles("active", "valid", "closed");
            case "duplicate" ->
                command("v1", QuayForgeDemoPayloads.transformV1(eventId), "LUMENWAY", false)
                        .withProfiles("active", "valid", "closed");
            case "rate-limit", "rate-limit-then-success", "server-error", "recovery" ->
                command("v1", QuayForgeDemoPayloads.transformV1(eventId), "COVECRM", false)
                        .withProfiles("active", "valid", "closed");
            default -> unknown(scenario);
        };
    }

    private static CrossingCommand command(String version, String payload, String destination, boolean unavailable) {
        return new CrossingCommand(
                null,
                ControlResult.pass(ControlKind.IDENTITY),
                ControlResult.pass(ControlKind.AUTHENTICITY),
                ControlResult.pass(ControlKind.FRESHNESS),
                ControlResult.pass(ControlKind.REPLAY),
                ContractEvaluator.QUAYFORGE_ORDERS,
                version,
                payload,
                destination,
                unavailable,
                null,
                "active",
                "valid",
                "closed");
    }

    private static String normalize(String scenario) {
        return scenario == null ? "" : scenario.trim().toLowerCase(Locale.ROOT);
    }

    private static CrossingCommand unknown(String scenario) {
        throw new InvalidInputException("UNKNOWN_SCENARIO", "Unknown synthetic scenario: " + scenario);
    }
}
