package dev.adellaghmari.boundarymesh.api.web;

import dev.adellaghmari.boundarymesh.api.security.JwtToPrincipalMapper;
import dev.adellaghmari.boundarymesh.application.BoundaryEnforcementEngine;
import dev.adellaghmari.boundarymesh.application.identity.IdentityControlFactory;
import dev.adellaghmari.boundarymesh.domain.BoundaryDecision;
import dev.adellaghmari.boundarymesh.domain.ControlKind;
import dev.adellaghmari.boundarymesh.domain.ControlResult;
import dev.adellaghmari.boundarymesh.domain.ControlVector;
import dev.adellaghmari.boundarymesh.domain.IngressKind;
import dev.adellaghmari.boundarymesh.domain.TransactionId;
import dev.adellaghmari.boundarymesh.domain.identity.BoundaryPermission;
import dev.adellaghmari.boundarymesh.domain.identity.BoundaryPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/identity", produces = MediaType.APPLICATION_JSON_VALUE)
public class IdentityEvaluateController {

    private static final Logger log = LoggerFactory.getLogger(IdentityEvaluateController.class);

    private final BoundaryEnforcementEngine engine;
    private final JwtToPrincipalMapper mapper;

    public IdentityEvaluateController(BoundaryEnforcementEngine engine, JwtToPrincipalMapper mapper) {
        this.engine = engine;
        this.mapper = mapper;
    }

    @PostMapping("/evaluate")
    public DecisionResponse evaluate(@AuthenticationPrincipal Jwt jwt) {
        BoundaryPrincipal principal = mapper.from(jwt);
        MDC.put("principal_reference", principal.id().value());
        MDC.put("issuer", principal.issuer().value());
        ControlVector vector = ControlVector.of(
                IdentityControlFactory.identity(principal),
                IdentityControlFactory.authorization(principal, BoundaryPermission.BOUNDARY_EVALUATE),
                ControlResult.notApplicable(ControlKind.AUTHENTICITY),
                ControlResult.notApplicable(ControlKind.FRESHNESS),
                ControlResult.notApplicable(ControlKind.REPLAY),
                ControlResult.pass(ControlKind.CONTRACT),
                ControlResult.pass(ControlKind.EGRESS),
                ControlResult.pass(ControlKind.CREDENTIAL_RESOLVABILITY),
                ControlResult.pass(ControlKind.CONNECTOR_SAFETY));
        TransactionId transactionId =
                TransactionId.of(java.util.UUID.randomUUID().toString());
        BoundaryDecision decision = engine.decide(transactionId, vector);
        MDC.put("boundary_outcome", decision.outcome().name());
        log.info("Identity-protected crossing evaluated");
        return DecisionResponse.from(decision, vector, IngressKind.TOKEN_PROTECTED_API, principal, false);
    }
}
