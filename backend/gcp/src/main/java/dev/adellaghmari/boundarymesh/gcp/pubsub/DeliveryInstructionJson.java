package dev.adellaghmari.boundarymesh.gcp.pubsub;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.adellaghmari.boundarymesh.application.outbox.DeliveryInstructionMessage;
import dev.adellaghmari.boundarymesh.domain.BoundaryOutcome;
import dev.adellaghmari.boundarymesh.domain.TransactionId;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialId;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialReference;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialStatus;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialVersion;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryInstruction;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryInstructionId;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** Versioned DeliveryInstruction JSON. Never includes tokens or secrets. */
final class DeliveryInstructionJson {

    private static final Gson GSON = new Gson();

    private DeliveryInstructionJson() {}

    static String write(DeliveryInstructionMessage message) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schema_version", message.schemaVersion());
        root.put("instruction_id", message.instructionId());
        root.put("integration_id", message.integrationId());
        root.put("destination_system", message.destinationSystem());
        root.put("instruction", instruction(message.instruction()));
        return GSON.toJson(root);
    }

    static DeliveryInstructionMessage read(String json) {
        Map<String, Object> root = GSON.fromJson(json, new TypeToken<Map<String, Object>>() {}.getType());
        if (root == null) {
            throw new IllegalArgumentException("empty delivery message");
        }
        String schema = string(root, "schema_version");
        @SuppressWarnings("unchecked")
        Map<String, Object> raw = (Map<String, Object>) root.get("instruction");
        if (raw == null) {
            throw new IllegalArgumentException("missing instruction");
        }
        return new DeliveryInstructionMessage(schema, instruction(raw));
    }

    private static Map<String, Object> instruction(DeliveryInstruction instruction) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("event_id", instruction.eventId().value());
        fields.put("transaction_id", instruction.transactionId().value());
        fields.put("integration_id", instruction.integrationId());
        fields.put("source_system", instruction.sourceSystem());
        fields.put("destination_system", instruction.destinationSystem());
        fields.put("contract_reference", instruction.contractReference());
        fields.put("egress_policy_reference", instruction.egressPolicyReference());
        fields.put("boundary_outcome", instruction.boundaryOutcome().name());
        fields.put("payload_json", instruction.payloadJson());
        fields.put("correlation_id", instruction.correlationId());
        fields.put("created_at", instruction.createdAt().toString());
        if (instruction.credentialReference() != null) {
            Map<String, Object> credential = new LinkedHashMap<>();
            credential.put("id", instruction.credentialReference().id().value());
            credential.put(
                    "version", instruction.credentialReference().version().value());
            credential.put("status", instruction.credentialReference().status().name());
            fields.put("credential_reference", credential);
        }
        fields.put("consent_id", instruction.consentId());
        return fields;
    }

    @SuppressWarnings("unchecked")
    private static DeliveryInstruction instruction(Map<String, Object> fields) {
        CredentialReference credential = null;
        Object rawCredential = fields.get("credential_reference");
        if (rawCredential instanceof Map<?, ?> map) {
            Map<String, Object> raw = (Map<String, Object>) map;
            credential = new CredentialReference(
                    new CredentialId(string(raw, "id")),
                    new CredentialVersion(string(raw, "version")),
                    CredentialStatus.valueOf(string(raw, "status")));
        }
        return new DeliveryInstruction(
                new DeliveryInstructionId(string(fields, "event_id")),
                TransactionId.of(string(fields, "transaction_id")),
                string(fields, "integration_id"),
                string(fields, "source_system"),
                string(fields, "destination_system"),
                optional(fields, "contract_reference"),
                optional(fields, "egress_policy_reference"),
                BoundaryOutcome.valueOf(string(fields, "boundary_outcome")),
                string(fields, "payload_json"),
                string(fields, "correlation_id"),
                Instant.parse(string(fields, "created_at")),
                credential,
                optional(fields, "consent_id"));
    }

    private static String string(Map<String, Object> fields, String key) {
        Object value = fields.get(key);
        if (value == null) {
            throw new IllegalArgumentException("missing field " + key);
        }
        return String.valueOf(value);
    }

    private static String optional(Map<String, Object> fields, String key) {
        Object value = fields.get(key);
        return value == null ? null : String.valueOf(value);
    }
}
