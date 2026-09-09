package dev.adellaghmari.boundarymesh.application.identity;

import dev.adellaghmari.boundarymesh.domain.ControlKind;
import dev.adellaghmari.boundarymesh.domain.ControlResult;
import dev.adellaghmari.boundarymesh.domain.ReasonCode;
import dev.adellaghmari.boundarymesh.domain.identity.BoundaryPermission;
import dev.adellaghmari.boundarymesh.domain.identity.BoundaryPrincipal;
import java.util.Objects;

/** Maps a verified principal to identity and authorization control results. No Spring types. */
public final class IdentityControlFactory {

    private IdentityControlFactory() {}

    public static ControlResult identity(BoundaryPrincipal principal) {
        Objects.requireNonNull(principal, "principal");
        return new ControlResult(
                ControlKind.IDENTITY,
                dev.adellaghmari.boundarymesh.domain.ControlStatus.PASS,
                java.util.List.of(),
                "Issuer accepted and machine identity authenticated.",
                principal.keyId());
    }

    public static ControlResult authorization(BoundaryPrincipal principal, BoundaryPermission required) {
        Objects.requireNonNull(principal, "principal");
        Objects.requireNonNull(required, "required");
        if (principal.hasScope(required)) {
            return new ControlResult(
                    ControlKind.AUTHORIZATION,
                    dev.adellaghmari.boundarymesh.domain.ControlStatus.PASS,
                    java.util.List.of(),
                    "Required permission " + required.scope() + " is granted.",
                    null);
        }
        return ControlResult.fail(
                ControlKind.AUTHORIZATION,
                ReasonCode.AUTHZ_MISSING_SCOPE,
                "Required permission " + required.scope() + " is missing.");
    }
}
