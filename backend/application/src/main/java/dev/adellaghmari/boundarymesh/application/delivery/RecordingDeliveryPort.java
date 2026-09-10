package dev.adellaghmari.boundarymesh.application.delivery;

import dev.adellaghmari.boundarymesh.domain.ReasonCode;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryResult;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryResultKind;
import dev.adellaghmari.boundarymesh.domain.delivery.ProviderFailureCategory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Test capture. Does not perform an external call and never reads secrets. */
public final class RecordingDeliveryPort implements DeliveryPort {

    private final List<DeliveryRequest> requests = new ArrayList<>();
    private final Set<String> seenInstructions = new HashSet<>();
    private DeliveryResult override;

    public void succeedWith(DeliveryResult result) {
        this.override = result;
    }

    public void reset() {
        requests.clear();
        seenInstructions.clear();
        override = null;
    }

    @Override
    public DeliveryResult deliver(DeliveryRequest request) {
        requests.add(request);
        String instructionId = request.instruction().eventId().value();
        if (!seenInstructions.add(instructionId)) {
            return new DeliveryResult(
                    DeliveryResultKind.DUPLICATE_SUPPRESSED,
                    0,
                    ProviderFailureCategory.DUPLICATE,
                    ReasonCode.DELIVERY_DUPLICATE_SUPPRESSED,
                    "Idempotent consumer recognized a duplicate delivery instruction.",
                    Duration.ZERO,
                    request.instruction().destinationSystem().toLowerCase(),
                    "recording.delivery",
                    "CLOSED",
                    200,
                    request.instruction().credentialReference(),
                    request.instruction().consentId(),
                    false);
        }
        if (override != null) {
            return override;
        }
        return new DeliveryResult(
                DeliveryResultKind.DELIVERED,
                1,
                ProviderFailureCategory.SUCCESS,
                null,
                "Synthetic in-process delivery. Not a Camel worker call.",
                Duration.ZERO,
                request.instruction().destinationSystem().toLowerCase(),
                "recording.delivery",
                "CLOSED",
                200,
                request.instruction().credentialReference(),
                request.instruction().consentId(),
                true);
    }

    public List<DeliveryRequest> requests() {
        return List.copyOf(requests);
    }

    public int invocationCount() {
        return requests.size();
    }
}
