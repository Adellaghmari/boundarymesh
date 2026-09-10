package dev.adellaghmari.boundarymesh.api.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.adellaghmari.boundarymesh.application.consent.ConsentCatalog;
import dev.adellaghmari.boundarymesh.application.credential.CredentialCatalog;
import dev.adellaghmari.boundarymesh.application.credential.InMemoryCredentialCatalog;
import dev.adellaghmari.boundarymesh.application.crossing.CrossingComposer;
import dev.adellaghmari.boundarymesh.application.crossing.CrossingScenarios;
import dev.adellaghmari.boundarymesh.domain.IngressKind;
import java.time.Clock;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/dev/credential-lab", produces = MediaType.APPLICATION_JSON_VALUE)
@ConditionalOnProperty(name = "boundarymesh.dev-evaluate-enabled", havingValue = "true")
public class DevCredentialLabController {

    private final CrossingComposer composer;
    private final CredentialCatalog credentials;
    private final ConsentCatalog consents;
    private final Clock clock;

    public DevCredentialLabController(
            CrossingComposer composer, CredentialCatalog credentials, ConsentCatalog consents, Clock clock) {
        this.composer = composer;
        this.credentials = credentials;
        this.consents = consents;
        this.clock = clock;
    }

    @GetMapping
    public CatalogView catalog() {
        return CatalogView.from(credentials, consents);
    }

    @PostMapping("/rotate/covecrm")
    public CatalogView rotate() {
        if (credentials instanceof InMemoryCredentialCatalog catalog) {
            catalog.rotate("covecrm-api", "3", clock.instant());
        }
        return catalog();
    }

    @PostMapping("/{scenario}")
    public DecisionResponse run(@PathVariable String scenario) {
        var command = CrossingScenarios.delivery(scenario);
        var result = composer.compose(command);
        return DecisionResponse.fromCrossing(result, IngressKind.SYNTHETIC_EVALUATE, null, true, command.payloadJson());
    }

    public record CatalogView(String banner, List<CredentialRow> credentials, List<ConsentRow> consents) {

        static CatalogView from(CredentialCatalog credentials, ConsentCatalog consents) {
            return new CatalogView(
                    "LOCAL SECURITY MODEL / SYNTHETIC DEMO. Google Secret Manager remains PLANNED.",
                    credentials.list().stream()
                            .map(item -> new CredentialRow(
                                    item.reference().id().value(),
                                    item.reference().version().value(),
                                    item.reference().status().name(),
                                    item.destination(),
                                    item.connector().value(),
                                    item.rotatedAt().toString()))
                            .toList(),
                    consents.list().stream()
                            .map(item -> new ConsentRow(
                                    item.id().value(),
                                    item.destination(),
                                    item.operation().value(),
                                    item.status().name(),
                                    item.kind().name(),
                                    item.permissions().stream()
                                            .map(permission -> permission.value())
                                            .toList()))
                            .toList());
        }
    }

    public record CredentialRow(
            String id,
            String version,
            String status,
            String destination,
            String connector,
            @JsonProperty("rotated_at") String rotatedAt) {}

    public record ConsentRow(
            String id, String destination, String operation, String status, String kind, List<String> permissions) {}
}
