package dev.adellaghmari.boundarymesh.api.web;

import dev.adellaghmari.boundarymesh.application.crossing.CrossingComposer;
import dev.adellaghmari.boundarymesh.application.crossing.CrossingScenarios;
import dev.adellaghmari.boundarymesh.domain.IngressKind;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/dev/contract-lab", produces = MediaType.APPLICATION_JSON_VALUE)
@ConditionalOnProperty(name = "boundarymesh.dev-security-tools-enabled", havingValue = "true")
public class DevContractLabController {

    private final CrossingComposer composer;

    public DevContractLabController(CrossingComposer composer) {
        this.composer = composer;
    }

    @PostMapping("/{scenario}")
    public DecisionResponse run(@PathVariable String scenario) {
        CrossingComposer.CrossingCommand command = CrossingScenarios.contract(scenario);
        CrossingComposer.CrossingResult result = composer.compose(command);
        return DecisionResponse.fromCrossing(result, IngressKind.SYNTHETIC_EVALUATE, null, true, command.payloadJson());
    }
}
