package dev.adellaghmari.boundarymesh.api.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.adellaghmari.boundarymesh.api.security.JwtToPrincipalMapper;
import dev.adellaghmari.boundarymesh.application.crossing.CrossingComposer;
import dev.adellaghmari.boundarymesh.application.crossing.CrossingScenarios;
import dev.adellaghmari.boundarymesh.domain.IngressKind;
import dev.adellaghmari.boundarymesh.domain.identity.BoundaryPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/dev/crossings", produces = MediaType.APPLICATION_JSON_VALUE)
@ConditionalOnProperty(name = "boundarymesh.dev-evaluate-enabled", havingValue = "true")
public class DevCrossingController {

    private static final Logger log = LoggerFactory.getLogger(DevCrossingController.class);

    private final CrossingComposer composer;
    private final JwtToPrincipalMapper mapper;

    public DevCrossingController(CrossingComposer composer, JwtToPrincipalMapper mapper) {
        this.composer = composer;
        this.mapper = mapper;
    }

    @PostMapping(path = "/evaluate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public DecisionResponse evaluate(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CrossingEvaluateRequest request) {
        BoundaryPrincipal principal = mapper.from(jwt);
        CrossingComposer.CrossingCommand command =
                CrossingScenarios.composed(request.scenario()).withPrincipal(principal);
        MDC.put("principal_reference", principal.id().value());
        CrossingComposer.CrossingResult result = composer.compose(command);
        MDC.put("transaction_id", result.decision().transactionId().value());
        MDC.put("boundary_outcome", result.decision().outcome().name());
        if (result.contract().reference() != null) {
            MDC.put("contract_id", result.contract().reference().id().value());
            MDC.put("contract_version", result.contract().reference().version().value());
        }
        MDC.put("contract_result", result.contract().classification());
        MDC.put("egress_policy_id", result.egress().policyId().value());
        MDC.put("egress_policy_version", result.egress().policyVersion().value());
        MDC.put("transformation_count", String.valueOf(result.egress().summary().transformationCount()));
        log.info("Composed synthetic crossing evaluated");
        return DecisionResponse.fromCrossing(
                result, IngressKind.TOKEN_PROTECTED_API, principal, true, command.payloadJson());
    }

    public record CrossingEvaluateRequest(@NotBlank @JsonProperty("scenario") String scenario) {}
}
