package dev.adellaghmari.boundarymesh.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.adellaghmari.boundarymesh.api.web.DeliveryStateController;
import dev.adellaghmari.boundarymesh.application.delivery.DeliveryStateQueryService;
import dev.adellaghmari.boundarymesh.application.delivery.InMemoryDeliveryIdempotencyStore;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryResult;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryResultKind;
import dev.adellaghmari.boundarymesh.domain.delivery.ProviderFailureCategory;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class DeliveryStateControllerTests {

    private static final String ID = "dlv-ef133a64-fe3c-4cd3-afc6-6c4486f3dd00";

    @Test
    void exposesOnlySafeTerminalStateAndNotFoundForUnknownOrMalformedIds() {
        var store = new InMemoryDeliveryIdempotencyStore();
        var controller = new DeliveryStateController(new DeliveryStateQueryService(store));
        assertEquals(404, controller.one(ID).getStatusCode().value());
        assertEquals(404, controller.one("../admin").getStatusCode().value());

        store.remember(
                ID,
                new DeliveryResult(
                        DeliveryResultKind.QUARANTINED,
                        1,
                        ProviderFailureCategory.CREDENTIAL_OR_CONSENT,
                        null,
                        "internal diagnostic that must not be serialized",
                        Duration.ZERO,
                        "connector",
                        "route",
                        "closed",
                        null,
                        null,
                        null,
                        false));
        var view = controller.one(ID).getBody();
        assertEquals("QUARANTINED", view.state());
        assertEquals("CREDENTIAL_OR_CONSENT", view.providerCategory());
        assertFalse(view.providerCalled());
        assertTrue(view.terminal());
        assertEquals(6, view.getClass().getRecordComponents().length);
    }
}
