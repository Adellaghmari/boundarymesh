package dev.adellaghmari.boundarymesh.application.crossing;

import dev.adellaghmari.boundarymesh.application.delivery.ProviderScenario;
import dev.adellaghmari.boundarymesh.domain.ControlKind;
import dev.adellaghmari.boundarymesh.domain.ControlResult;
import dev.adellaghmari.boundarymesh.domain.ControlStatus;
import dev.adellaghmari.boundarymesh.domain.TransactionId;
import dev.adellaghmari.boundarymesh.domain.exception.InvalidInputException;
import java.util.List;
import java.util.Objects;

/**
 * Product submission boundary for a transaction admitted by the private Cloud Run IAM edge.
 *
 * <p>The service owns no HTTP or cloud SDK concerns. It converts a validated transaction request
 * into the same {@link CrossingDeliveryService} command used by the existing delivery pipeline.
 * The Cloud Run service is the external authentication boundary; contract, egress, credential,
 * consent, connector-safety, evidence, and delivery controls remain inside the application flow.
 */
public final class TransactionSubmissionService {

    private final CrossingDeliveryService deliveries;

    public TransactionSubmissionService(CrossingDeliveryService deliveries) {
        this.deliveries = Objects.requireNonNull(deliveries, "deliveries");
    }

    public CrossingDeliveryService.CrossingDeliveryResult submit(Submission request) {
        Objects.requireNonNull(request, "request");
        CrossingComposer.CrossingCommand command = new CrossingComposer.CrossingCommand(
                null,
                admittedByCloudRun(ControlKind.IDENTITY, "Private Cloud Run IAM admitted the transaction submitter."),
                admittedByCloudRun(
                        ControlKind.AUTHENTICITY, "Private Cloud Run IAM protected the transaction ingress."),
                ControlResult.notApplicable(ControlKind.FRESHNESS),
                ControlResult.notApplicable(ControlKind.REPLAY),
                request.integrationId(),
                request.contractVersion(),
                request.payloadJson(),
                request.destinationSystem(),
                false,
                request.transactionId(),
                "active",
                "valid",
                "closed");
        return deliveries.execute(command, ProviderScenario.SUCCESS);
    }

    private static ControlResult admittedByCloudRun(ControlKind kind, String explanation) {
        return new ControlResult(kind, ControlStatus.PASS, List.of(), explanation, "cloud-run-iam");
    }

    /** A producer-selected UUID is the transaction correlation and idempotency reference. */
    public record Submission(
            TransactionId transactionId,
            String integrationId,
            String contractVersion,
            String destinationSystem,
            String payloadJson) {

        public Submission {
            Objects.requireNonNull(transactionId, "transactionId");
            integrationId = required(integrationId, "INTEGRATION_ID_REQUIRED", "integration id is required");
            contractVersion = required(contractVersion, "CONTRACT_VERSION_REQUIRED", "contract version is required");
            destinationSystem = required(destinationSystem, "DESTINATION_REQUIRED", "destination system is required");
            payloadJson = required(payloadJson, "PAYLOAD_REQUIRED", "payload is required");
        }

        private static String required(String value, String code, String message) {
            if (value == null || value.isBlank()) {
                throw new InvalidInputException(code, message);
            }
            return value.trim();
        }
    }
}
