package dev.adellaghmari.boundarymesh.application.identity;

import static org.assertj.core.api.Assertions.assertThat;

import dev.adellaghmari.boundarymesh.domain.ControlStatus;
import dev.adellaghmari.boundarymesh.domain.ReasonCode;
import dev.adellaghmari.boundarymesh.domain.identity.AuthenticationMethod;
import dev.adellaghmari.boundarymesh.domain.identity.BoundaryPermission;
import dev.adellaghmari.boundarymesh.domain.identity.BoundaryPrincipal;
import dev.adellaghmari.boundarymesh.domain.identity.Issuer;
import dev.adellaghmari.boundarymesh.domain.identity.PrincipalId;
import dev.adellaghmari.boundarymesh.domain.identity.PrincipalType;
import java.util.Set;
import org.junit.jupiter.api.Test;

class IdentityControlFactoryTest {

    @Test
    void authorizationFailsWhenEvaluateScopeMissing() {
        BoundaryPrincipal principal = new BoundaryPrincipal(
                PrincipalId.of("machine-1"),
                PrincipalType.APPLICATION,
                Issuer.of("http://127.0.0.1:8080/api/v1/dev/issuer"),
                AuthenticationMethod.JWT_BEARER,
                Set.of("boundary.read"),
                Set.of(),
                "kid-1");
        assertThat(IdentityControlFactory.identity(principal).status()).isEqualTo(ControlStatus.PASS);
        assertThat(IdentityControlFactory.authorization(principal, BoundaryPermission.BOUNDARY_EVALUATE)
                        .reasonCodes())
                .contains(ReasonCode.AUTHZ_MISSING_SCOPE);
    }
}
