package dev.adellaghmari.boundarymesh.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.adellaghmari.boundarymesh.domain.exception.InvalidInputException;
import dev.adellaghmari.boundarymesh.domain.exception.InvariantViolationException;
import org.junit.jupiter.api.Test;

class ControlVectorTest {

    @Test
    void allPassVectorHasNineControls() {
        ControlVector vector = ControlVector.allPass();
        assertThat(vector.asList()).hasSize(ControlKind.values().length);
        assertThat(vector.asList()).allMatch(result -> result.status() == ControlStatus.PASS);
    }

    @Test
    void rejectsKindMismatch() {
        assertThatThrownBy(() -> ControlVector.allPass().withIdentity(ControlResult.pass(ControlKind.AUTHORIZATION)))
                .isInstanceOf(InvariantViolationException.class)
                .hasFieldOrPropertyWithValue("errorCode", "CONTROL_KIND_MISMATCH");
    }

    @Test
    void failRequiresReasonCode() {
        assertThatThrownBy(() -> new ControlResult(
                        ControlKind.IDENTITY, ControlStatus.FAIL, java.util.List.of(), "failed", null))
                .isInstanceOf(InvalidInputException.class)
                .hasFieldOrPropertyWithValue("errorCode", "CONTROL_REASON_REQUIRED");
    }

    @Test
    void transactionIdMustBeUuid() {
        assertThatThrownBy(() -> TransactionId.of("not-a-uuid")).isInstanceOf(InvalidInputException.class);
        assertThat(TransactionId.of("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee").value())
                .isEqualTo("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    }

    @Test
    void integrationIdMustBeSlug() {
        assertThatThrownBy(() -> IntegrationId.of("Helix")).isInstanceOf(InvalidInputException.class);
        assertThat(IntegrationId.of("helix-calendar-crm").value()).isEqualTo("helix-calendar-crm");
    }
}
