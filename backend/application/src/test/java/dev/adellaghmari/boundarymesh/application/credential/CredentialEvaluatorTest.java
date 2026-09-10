package dev.adellaghmari.boundarymesh.application.credential;

import static org.assertj.core.api.Assertions.assertThat;

import dev.adellaghmari.boundarymesh.domain.ControlStatus;
import dev.adellaghmari.boundarymesh.domain.ReasonCode;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CredentialEvaluatorTest {

    private InMemoryCredentialCatalog catalog;
    private CredentialEvaluator evaluator;

    @BeforeEach
    void setUp() {
        catalog = InMemoryCredentialCatalog.veloraDefaults(Instant.parse("2026-09-10T00:00:00Z"));
        evaluator = new CredentialEvaluator(catalog);
    }

    @Test
    void activeReferencePasses() {
        var result = evaluator.evaluate("COVECRM", "active");
        assertThat(result.status()).isEqualTo(ControlStatus.PASS);
        assertThat(result.metadataRef()).isEqualTo("covecrm-api:v2");
    }

    @Test
    void missingReferenceFails() {
        var result = evaluator.evaluate("COVECRM", "missing");
        assertThat(result.status()).isEqualTo(ControlStatus.FAIL);
        assertThat(result.reasonCodes()).contains(ReasonCode.CREDENTIAL_MISSING);
    }

    @Test
    void revokedVersionFailsWithoutFallback() {
        var result = evaluator.evaluate("COVECRM", "revoked");
        assertThat(result.status()).isEqualTo(ControlStatus.FAIL);
        assertThat(result.reasonCodes()).contains(ReasonCode.CREDENTIAL_REVOKED);
        assertThat(catalog.find("covecrm-api", "1")).isPresent();
        assertThat(catalog.find("covecrm-api", "1")
                        .orElseThrow()
                        .reference()
                        .status()
                        .name())
                .isEqualTo("RETIRED");
    }

    @Test
    void rotationMovesNewDeliveriesToNewVersion() {
        catalog.rotate("covecrm-api", "3", Instant.parse("2026-09-10T01:00:00Z"));
        var result = evaluator.evaluate("COVECRM", "rotated");
        assertThat(result.metadataRef()).isEqualTo("covecrm-api:v3");
        assertThat(catalog.find("covecrm-api", "2")
                        .orElseThrow()
                        .reference()
                        .status()
                        .name())
                .isEqualTo("RETIRED");
    }

    @Test
    void providerUnavailableIsUnavailable() {
        var result = evaluator.evaluate("COVECRM", "unavailable");
        assertThat(result.status()).isEqualTo(ControlStatus.UNAVAILABLE);
        assertThat(result.reasonCodes()).contains(ReasonCode.CREDENTIAL_PROVIDER_UNAVAILABLE);
    }
}
