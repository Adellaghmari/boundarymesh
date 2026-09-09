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
    public static final ReasonCode AUTHN_EXPIRED = new ReasonCode("AUTHN_EXPIRED", "The presented token is expired.");
    public static final ReasonCode AUTHN_WRONG_ISSUER =
            new ReasonCode("AUTHN_WRONG_ISSUER", "The token issuer is not an allowed issuer.");
    public static final ReasonCode AUTHN_WRONG_AUDIENCE =
            new ReasonCode("AUTHN_WRONG_AUDIENCE", "The token audience does not target BoundaryMesh.");
    public static final ReasonCode AUTHN_INVALID_JWT =
            new ReasonCode("AUTHN_INVALID_JWT", "The token signature or structure is invalid.");
    public static final ReasonCode AUTHN_UNKNOWN_KID =
            new ReasonCode("AUTHN_UNKNOWN_KID", "The token key id is not present in the current JWKS.");
    public static final ReasonCode AUTHN_NOT_YET_VALID =
            new ReasonCode("AUTHN_NOT_YET_VALID", "The token is not valid yet.");
    public static final ReasonCode AUTHORIZATION_MISSING =
            new ReasonCode("AUTHORIZATION_MISSING", "Authenticated identity lacks permission for this crossing.");
    public static final ReasonCode AUTHORIZATION_UNAVAILABLE = new ReasonCode(
            "AUTHORIZATION_UNAVAILABLE",
            "Authorization could not be evaluated because a required verifier was unavailable.");
    public static final ReasonCode AUTHZ_MISSING_SCOPE =
            new ReasonCode("AUTHZ_MISSING_SCOPE", "Authenticated principal is missing a required scope.");
    public static final ReasonCode AUTHENTICITY_INVALID =
            new ReasonCode("AUTHENTICITY_INVALID", "Message authenticity could not be verified.");
    public static final ReasonCode AUTHENTICITY_INVALID_SIGNATURE =
            new ReasonCode("AUTHENTICITY_INVALID_SIGNATURE", "The webhook HMAC signature is invalid.");
    public static final ReasonCode AUTHENTICITY_MISSING_SIGNATURE =
            new ReasonCode("AUTHENTICITY_MISSING_SIGNATURE", "The webhook signature header is missing or malformed.");
    public static final ReasonCode AUTHENTICITY_UNAVAILABLE = new ReasonCode(
            "AUTHENTICITY_UNAVAILABLE",
            "Authenticity could not be evaluated because a required verifier was unavailable.");
    public static final ReasonCode FRESHNESS_EXPIRED =
            new ReasonCode("FRESHNESS_EXPIRED", "The transaction was outside the permitted freshness window.");
    public static final ReasonCode FRESHNESS_FUTURE =
            new ReasonCode("FRESHNESS_FUTURE", "The signed timestamp is too far in the future.");
    public static final ReasonCode FRESHNESS_UNAVAILABLE =
            new ReasonCode("FRESHNESS_UNAVAILABLE", "Freshness could not be evaluated.");
    public static final ReasonCode REPLAY_DETECTED =
            new ReasonCode("REPLAY_DETECTED", "A previously used nonce or equivalent replay marker was detected.");
    public static final ReasonCode DUPLICATE_DELIVERY = new ReasonCode(
            "DUPLICATE_DELIVERY", "The provider delivered a previously accepted event with a new security nonce.");
    public static final ReasonCode REPLAY_UNAVAILABLE =
            new ReasonCode("REPLAY_UNAVAILABLE", "Replay checking could not be evaluated.");
    public static final ReasonCode CONTRACT_INVALID =
            new ReasonCode("CONTRACT_INVALID", "The payload did not satisfy the supported integration contract.");
    public static final ReasonCode CONTRACT_SCHEMA_INVALID = new ReasonCode(
            "CONTRACT_SCHEMA_INVALID", "The payload failed JSON Schema validation for a supported version.");
    public static final ReasonCode CONTRACT_MALFORMED_PAYLOAD =
            new ReasonCode("CONTRACT_MALFORMED_PAYLOAD", "The payload is not usable JSON for contract evaluation.");
    public static final ReasonCode CONTRACT_UNSUPPORTED = new ReasonCode(
            "CONTRACT_UNSUPPORTED", "The contract or schema version is not supported and was held for operations.");
    public static final ReasonCode CONTRACT_VERSION_UNSUPPORTED = new ReasonCode(
            "CONTRACT_VERSION_UNSUPPORTED", "The declared contract version is not in the supported set.");
    public static final ReasonCode CONTRACT_DRIFT_DETECTED = new ReasonCode(
            "CONTRACT_DRIFT_DETECTED",
            "Declared version no longer matches observed structure; treated as provider contract drift.");
    public static final ReasonCode CONTRACT_UNAVAILABLE =
            new ReasonCode("CONTRACT_UNAVAILABLE", "Contract validation could not be evaluated.");
    public static final ReasonCode EGRESS_TRANSFORMATION_REQUIRED = new ReasonCode(
            "EGRESS_TRANSFORMATION_REQUIRED", "The payload may cross only after deterministic transformation.");
    public static final ReasonCode EGRESS_REDACTION_REQUIRED =
            new ReasonCode("EGRESS_REDACTION_REQUIRED", "Destination policy requires deterministic field redaction.");
    public static final ReasonCode EGRESS_FIELD_DENIED =
            new ReasonCode("EGRESS_FIELD_DENIED", "A destination policy denied one or more fields.");
    public static final ReasonCode EGRESS_POLICY_UNAVAILABLE =
            new ReasonCode("EGRESS_POLICY_UNAVAILABLE", "No destination egress policy could be resolved.");
    public static final ReasonCode EGRESS_POLICY_INVALID =
            new ReasonCode("EGRESS_POLICY_INVALID", "The loaded egress policy is invalid and was not applied.");
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
