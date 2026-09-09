package dev.adellaghmari.boundarymesh.domain;

import dev.adellaghmari.boundarymesh.domain.exception.InvalidInputException;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Stable machine code plus a safe human description. Never contains secrets or payload values.
 */
public record ReasonCode(String code, String description) {

    private static final Pattern CODE = Pattern.compile("^[A-Z][A-Z0-9_]{1,63}$");

    public static final ReasonCode IDENTITY_INVALID =
            new ReasonCode("IDENTITY_INVALID", "Identity could not be established or was cryptographically invalid.");
    public static final ReasonCode IDENTITY_UNAVAILABLE = new ReasonCode(
            "IDENTITY_UNAVAILABLE", "Identity could not be verified because a required verifier was unavailable.");
    public static final ReasonCode AUTHORIZATION_MISSING =
            new ReasonCode("AUTHORIZATION_MISSING", "Authenticated identity lacks permission for this crossing.");
    public static final ReasonCode AUTHORIZATION_UNAVAILABLE = new ReasonCode(
            "AUTHORIZATION_UNAVAILABLE",
            "Authorization could not be evaluated because a required verifier was unavailable.");
    public static final ReasonCode AUTHENTICITY_INVALID =
            new ReasonCode("AUTHENTICITY_INVALID", "Message authenticity could not be verified.");
    public static final ReasonCode AUTHENTICITY_UNAVAILABLE = new ReasonCode(
            "AUTHENTICITY_UNAVAILABLE",
            "Authenticity could not be evaluated because a required verifier was unavailable.");
    public static final ReasonCode FRESHNESS_EXPIRED =
            new ReasonCode("FRESHNESS_EXPIRED", "The transaction was outside the permitted freshness window.");
    public static final ReasonCode FRESHNESS_UNAVAILABLE =
            new ReasonCode("FRESHNESS_UNAVAILABLE", "Freshness could not be evaluated.");
    public static final ReasonCode REPLAY_DETECTED =
            new ReasonCode("REPLAY_DETECTED", "A previously used nonce or equivalent replay marker was detected.");
    public static final ReasonCode REPLAY_UNAVAILABLE =
            new ReasonCode("REPLAY_UNAVAILABLE", "Replay checking could not be evaluated.");
    public static final ReasonCode CONTRACT_INVALID =
            new ReasonCode("CONTRACT_INVALID", "The payload did not satisfy the supported integration contract.");
    public static final ReasonCode CONTRACT_UNSUPPORTED = new ReasonCode(
            "CONTRACT_UNSUPPORTED", "The contract or schema version is not supported and was held for operations.");
    public static final ReasonCode CONTRACT_UNAVAILABLE =
            new ReasonCode("CONTRACT_UNAVAILABLE", "Contract validation could not be evaluated.");
    public static final ReasonCode EGRESS_TRANSFORMATION_REQUIRED = new ReasonCode(
            "EGRESS_TRANSFORMATION_REQUIRED", "The payload may cross only after deterministic transformation.");
    public static final ReasonCode EGRESS_FIELD_DENIED =
            new ReasonCode("EGRESS_FIELD_DENIED", "A destination policy denied one or more fields.");
    public static final ReasonCode CREDENTIAL_UNAVAILABLE = new ReasonCode(
            "CREDENTIAL_UNAVAILABLE", "A required connector credential reference could not be resolved.");
    public static final ReasonCode CONNECTOR_UNAVAILABLE =
            new ReasonCode("CONNECTOR_UNAVAILABLE", "The destination connector is not currently safe to receive work.");

    public ReasonCode {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(description, "description");
        if (!CODE.matcher(code).matches()) {
            throw new InvalidInputException("REASON_CODE_INVALID", "reason code must be an uppercase snake token");
        }
        if (description.isBlank() || description.length() > 240) {
            throw new InvalidInputException(
                    "REASON_DESCRIPTION_INVALID", "reason description must be 1–240 characters");
        }
        if (description.toLowerCase().contains("bearer ")
                || description.toLowerCase().contains("secret")) {
            throw new InvalidInputException("REASON_DESCRIPTION_UNSAFE", "reason description must not include secrets");
        }
    }

    public static ReasonCode of(String code, String description) {
        return new ReasonCode(code, description);
    }
}
