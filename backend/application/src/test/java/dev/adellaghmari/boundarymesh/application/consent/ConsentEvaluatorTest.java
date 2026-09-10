package dev.adellaghmari.boundarymesh.application.consent;

import static org.assertj.core.api.Assertions.assertThat;

import dev.adellaghmari.boundarymesh.domain.ReasonCode;
import dev.adellaghmari.boundarymesh.domain.consent.ConnectorOperation;
import dev.adellaghmari.boundarymesh.domain.consent.ConsentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConsentEvaluatorTest {

    private InMemoryConsentCatalog catalog;
    private ConsentEvaluator evaluator;

    @BeforeEach
    void setUp() {
        catalog = InMemoryConsentCatalog.veloraDefaults();
        evaluator = new ConsentEvaluator(catalog);
    }

    @Test
    void validConsentPasses() {
        var result = evaluator.evaluate("RIVERTIDE", ConnectorOperation.SETTLEMENT_WRITE, "valid");
        assertThat(result.status()).isEqualTo(ConsentStatus.GRANTED);
        assertThat(result.blocksCrossing()).isFalse();
    }

    @Test
    void missingConsentBlocksEvenWhenCredentialWouldExist() {
        var result = evaluator.evaluate("RIVERTIDE", ConnectorOperation.SETTLEMENT_WRITE, "missing");
        assertThat(result.status()).isEqualTo(ConsentStatus.MISSING);
        assertThat(result.reason()).isEqualTo(ReasonCode.CONSENT_MISSING);
        assertThat(result.blocksCrossing()).isTrue();
    }

    @Test
    void revokedConsentBlocks() {
        var result = evaluator.evaluate("RIVERTIDE", ConnectorOperation.SETTLEMENT_WRITE, "revoked");
        assertThat(result.status()).isEqualTo(ConsentStatus.REVOKED);
        assertThat(result.reason()).isEqualTo(ReasonCode.CONSENT_REVOKED);
    }
}
