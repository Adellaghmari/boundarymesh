package dev.adellaghmari.boundarymesh.api.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.adellaghmari.boundarymesh.application.crossing.TransactionSubmissionService;
import dev.adellaghmari.boundarymesh.application.crossing.TransactionSubmissionService.Submission;
import dev.adellaghmari.boundarymesh.domain.TransactionId;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryInstruction;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/** Private Cloud Run transaction ingress for governed asynchronous delivery. */
@RestController
@RequestMapping(path = "/api/v1/transactions", produces = MediaType.APPLICATION_JSON_VALUE)
public class TransactionController {

    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);

    private final TransactionSubmissionService submissions;

    public TransactionController(TransactionSubmissionService submissions) {
        this.submissions = submissions;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TransactionSubmissionResponse> submit(@Valid @RequestBody TransactionRequest request) {
        var result = submissions.submit(new Submission(
                TransactionId.of(request.transactionId().toString()),
                request.integrationId(),
                request.contractVersion(),
                request.destinationSystem(),
                request.payload().toString()));
        MDC.put("transaction_id", result.crossing().decision().transactionId().value());
        MDC.put("boundary_outcome", result.crossing().decision().outcome().name());
        log.info("Boundary transaction submitted for governed delivery");
        TransactionSubmissionResponse response = TransactionSubmissionResponse.from(result);
        return ResponseEntity.status(response.queued() ? HttpStatus.ACCEPTED : HttpStatus.OK)
                .body(response);
    }

    public record TransactionRequest(
            @NotNull @JsonProperty("transaction_id") UUID transactionId,
            @NotBlank @JsonProperty("integration_id") String integrationId,
            @NotBlank @JsonProperty("contract_version") String contractVersion,
            @NotBlank @JsonProperty("destination_system") String destinationSystem,
            @NotNull JsonNode payload) {}

    public record TransactionSubmissionResponse(
            @JsonProperty("transaction_id") String transactionId,
            String outcome,
            @JsonProperty("submission_status") String submissionStatus,
            @JsonProperty("delivery_status") String deliveryStatus,
            @JsonProperty("instruction_id") String instructionId,
            @JsonProperty("attestation_id") String attestationId,
            @JsonProperty("attestation_status") String attestationStatus,
            @JsonProperty("audit_chain_id") String auditChainId,
            @JsonProperty("quarantine_id") String quarantineId,
            boolean queued) {

        static TransactionSubmissionResponse from(
                dev.adellaghmari.boundarymesh.application.crossing.CrossingDeliveryService.CrossingDeliveryResult
                        result) {
            DeliveryInstruction instruction = result.instruction().orElse(null);
            boolean queued = instruction != null;
            return new TransactionSubmissionResponse(
                    result.crossing().decision().transactionId().value(),
                    result.crossing().decision().outcome().name(),
                    queued ? "ACCEPTED_FOR_ASYNCHRONOUS_DELIVERY" : "NOT_QUEUED",
                    result.delivery().kind().name(),
                    instruction == null ? null : instruction.eventId().value(),
                    result.attestation() == null
                            ? null
                            : result.attestation().id().value(),
                    result.attestation() == null
                            ? null
                            : result.attestation().status().name(),
                    result.attestation() == null
                            ? null
                            : result.attestation().auditChainId().value(),
                    result.quarantine() == null
                            ? null
                            : result.quarantine().id().value(),
                    queued);
        }
    }
}
