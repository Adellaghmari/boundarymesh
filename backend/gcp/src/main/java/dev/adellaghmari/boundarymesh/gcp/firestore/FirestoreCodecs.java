package dev.adellaghmari.boundarymesh.gcp.firestore;

import dev.adellaghmari.boundarymesh.application.graph.ObservedCrossingStore;
import dev.adellaghmari.boundarymesh.application.outbox.OutboxRecord;
import dev.adellaghmari.boundarymesh.application.outbox.OutboxStatus;
import dev.adellaghmari.boundarymesh.application.outbox.PublishFailureKind;
import dev.adellaghmari.boundarymesh.domain.BoundaryDecision;
import dev.adellaghmari.boundarymesh.domain.BoundaryOutcome;
import dev.adellaghmari.boundarymesh.domain.ControlKind;
import dev.adellaghmari.boundarymesh.domain.ControlStatus;
import dev.adellaghmari.boundarymesh.domain.PolicyVersion;
import dev.adellaghmari.boundarymesh.domain.ReasonCode;
import dev.adellaghmari.boundarymesh.domain.TransactionId;
import dev.adellaghmari.boundarymesh.domain.attestation.AttestationId;
import dev.adellaghmari.boundarymesh.domain.attestation.AttestationSignature;
import dev.adellaghmari.boundarymesh.domain.attestation.AttestationStatus;
import dev.adellaghmari.boundarymesh.domain.attestation.AttestationVersion;
import dev.adellaghmari.boundarymesh.domain.attestation.ExchangeAttestation;
import dev.adellaghmari.boundarymesh.domain.attestation.SignerKeyReference;
import dev.adellaghmari.boundarymesh.domain.audit.AuditChainHead;
import dev.adellaghmari.boundarymesh.domain.audit.AuditChainId;
import dev.adellaghmari.boundarymesh.domain.audit.AuditEntry;
import dev.adellaghmari.boundarymesh.domain.audit.AuditEntryId;
import dev.adellaghmari.boundarymesh.domain.audit.AuditEventType;
import dev.adellaghmari.boundarymesh.domain.audit.AuditHash;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialId;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialReference;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialStatus;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialVersion;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryInstruction;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryInstructionId;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryResult;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryResultKind;
import dev.adellaghmari.boundarymesh.domain.delivery.ProviderFailureCategory;
import dev.adellaghmari.boundarymesh.domain.quarantine.QuarantineId;
import dev.adellaghmari.boundarymesh.domain.quarantine.QuarantineItem;
import dev.adellaghmari.boundarymesh.domain.quarantine.QuarantineReason;
import dev.adellaghmari.boundarymesh.domain.quarantine.QuarantineStatus;
import dev.adellaghmari.boundarymesh.domain.quarantine.RecoveryAttempt;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Document codecs. Instants that participate in hashes are stored as {@link Instant#toString()}. */
final class FirestoreCodecs {

    private FirestoreCodecs() {}

    static Map<String, Object> auditHead(AuditEntry entry) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("chain_id", entry.chainId().value());
        fields.put("sequence", entry.sequence());
        fields.put("head_hash", entry.currentHash().hex());
        fields.put("updated_at", Instant.now().toString());
        return fields;
    }

    static AuditChainHead auditHead(Map<String, Object> fields) {
        return new AuditChainHead(
                new AuditChainId(string(fields, "chain_id")),
                longValue(fields, "sequence"),
                new AuditHash(string(fields, "head_hash")));
    }

    static Map<String, Object> auditEntry(AuditEntry entry) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("entry_id", entry.id().value());
        fields.put("chain_id", entry.chainId().value());
        fields.put("sequence", entry.sequence());
        fields.put("occurred_at", entry.occurredAt().toString());
        fields.put("event_type", entry.eventType().name());
        fields.put("attributes", new TreeMap<>(entry.attributes()));
        fields.put("previous_hash", entry.previousHash().hex());
        fields.put("current_hash", entry.currentHash().hex());
        fields.put("schema_version", AuditEntry.SCHEMA_VERSION);
        return fields;
    }

    static AuditEntry auditEntry(Map<String, Object> fields) {
        return new AuditEntry(
                new AuditEntryId(string(fields, "entry_id")),
                new AuditChainId(string(fields, "chain_id")),
                longValue(fields, "sequence"),
                Instant.parse(string(fields, "occurred_at")),
                AuditEventType.valueOf(string(fields, "event_type")),
                stringMap(fields.get("attributes")),
                new AuditHash(string(fields, "previous_hash")),
                new AuditHash(string(fields, "current_hash")));
    }

    static Map<String, Object> replayNonce(String nonceHash, Instant expiresAt, Instant claimedAt) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("nonce_hash", nonceHash);
        fields.put("claimed_at", claimedAt.toString());
        fields.put("expires_at", expiresAt.toString());
        fields.put("expires_at_ms", expiresAt.toEpochMilli());
        return fields;
    }

    static Instant replayExpiresAt(Map<String, Object> fields) {
        return Instant.parse(string(fields, "expires_at"));
    }

    static Map<String, Object> idempotency(String eventKey, BoundaryDecision decision, Instant createdAt) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("event_key", eventKey);
        fields.put("decision", decision(decision));
        fields.put("created_at", createdAt.toString());
        return fields;
    }

    static BoundaryDecision decision(Map<String, Object> fields) {
        return decisionFrom(map(fields.get("decision")));
    }

    static Map<String, Object> deliveryIdempotency(
            String deliveryInstructionId, DeliveryResult result, Instant createdAt) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("delivery_instruction_id", deliveryInstructionId);
        fields.put("result", deliveryResult(result));
        fields.put("created_at", createdAt.toString());
        return fields;
    }

    static DeliveryResult deliveryResultFromDocument(Map<String, Object> fields) {
        return deliveryResult(map(fields.get("result")));
    }

    static Map<String, Object> quarantine(QuarantineItem item) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("quarantine_id", item.id().value());
        fields.put("transaction_id", item.transactionId().value());
        fields.put("integration_id", item.integrationId());
        fields.put("source_system", item.sourceSystem());
        fields.put("destination_system", item.destinationSystem());
        fields.put("reason", item.reason().name());
        fields.put("reason_code", item.reasonCode().code());
        fields.put("reason_description", item.reasonCode().description());
        fields.put("status", item.status().name());
        fields.put("recovery_attempts", item.recoveryAttempts());
        fields.put("max_recovery_attempts", item.maxRecoveryAttempts());
        fields.put("eligible", item.eligible());
        fields.put("contract_reference", item.contractReference());
        fields.put("policy_reference", item.policyReference());
        fields.put("credential_reference", item.credentialReference());
        fields.put("connector_state", item.connectorState());
        fields.put("created_at", item.createdAt().toString());
        fields.put("updated_at", item.updatedAt().toString());
        List<Map<String, Object>> attempts = new ArrayList<>();
        for (RecoveryAttempt attempt : item.attempts()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("attempt_number", attempt.attemptNumber());
            row.put("attempted_at", attempt.attemptedAt().toString());
            row.put("resulting_status", attempt.resultingStatus().name());
            row.put("note", attempt.note());
            attempts.add(row);
        }
        fields.put("attempts", attempts);
        return fields;
    }

    static QuarantineItem quarantineItem(Map<String, Object> fields) {
        List<RecoveryAttempt> attempts = new ArrayList<>();
        Object rawAttempts = fields.get("attempts");
        if (rawAttempts instanceof List<?> list) {
            for (Object row : list) {
                Map<String, Object> attempt = map(row);
                attempts.add(new RecoveryAttempt(
                        (int) longValue(attempt, "attempt_number"),
                        Instant.parse(string(attempt, "attempted_at")),
                        QuarantineStatus.valueOf(string(attempt, "resulting_status")),
                        stringOrNull(attempt, "note")));
            }
        }
        return new QuarantineItem(
                new QuarantineId(string(fields, "quarantine_id")),
                TransactionId.of(string(fields, "transaction_id")),
                stringOrNull(fields, "integration_id"),
                stringOrNull(fields, "source_system"),
                stringOrNull(fields, "destination_system"),
                QuarantineReason.valueOf(string(fields, "reason")),
                new ReasonCode(string(fields, "reason_code"), string(fields, "reason_description")),
                QuarantineStatus.valueOf(string(fields, "status")),
                (int) longValue(fields, "recovery_attempts"),
                (int) longValue(fields, "max_recovery_attempts"),
                bool(fields, "eligible"),
                stringOrNull(fields, "contract_reference"),
                stringOrNull(fields, "policy_reference"),
                stringOrNull(fields, "credential_reference"),
                stringOrNull(fields, "connector_state"),
                Instant.parse(string(fields, "created_at")),
                Instant.parse(string(fields, "updated_at")),
                attempts);
    }

    static Map<String, Object> attestation(ExchangeAttestation attestation) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("attestation_id", attestation.id().value());
        fields.put("schema_version", attestation.schemaVersion().value());
        fields.put("transaction_id", attestation.transactionId());
        fields.put("integration_id", attestation.integrationId());
        fields.put("source_system", attestation.sourceSystem());
        fields.put("destination_system", attestation.destinationSystem());
        fields.put("occurred_at", attestation.occurredAt().toString());
        fields.put("principal_reference", attestation.principalReference());
        fields.put("authentication_method", attestation.authenticationMethod());
        fields.put("control_results", new TreeMap<>(attestation.controlResults()));
        fields.put("contract_id", attestation.contractId());
        fields.put("contract_version", attestation.contractVersion());
        fields.put("contract_result", attestation.contractResult());
        fields.put("egress_policy_id", attestation.egressPolicyId());
        fields.put("transformation_count", attestation.transformationCount());
        fields.put("credential_reference", attestation.credentialReference());
        fields.put("consent_result", attestation.consentResult());
        fields.put("connector_route", attestation.connectorRoute());
        fields.put("delivery_result", attestation.deliveryResult());
        fields.put("boundary_outcome", attestation.boundaryOutcome().name());
        fields.put("audit_chain_id", attestation.auditChainId().value());
        fields.put("audit_sequence", attestation.auditSequence());
        fields.put("audit_head", attestation.auditHead().hex());
        fields.put("signer_key_id", attestation.signer().keyId());
        fields.put("signer_key_version", attestation.signer().keyVersion());
        fields.put("signer_algorithm", attestation.signer().algorithm());
        fields.put("status", attestation.status().name());
        fields.put("signature_base64", attestation.signature().base64());
        fields.put("saved_at", Instant.now().toString());
        return fields;
    }

    static ExchangeAttestation attestationFrom(Map<String, Object> fields) {
        return new ExchangeAttestation(
                new AttestationId(string(fields, "attestation_id")),
                new AttestationVersion(string(fields, "schema_version")),
                string(fields, "transaction_id"),
                string(fields, "integration_id"),
                stringOrNull(fields, "source_system"),
                stringOrNull(fields, "destination_system"),
                Instant.parse(string(fields, "occurred_at")),
                stringOrNull(fields, "principal_reference"),
                stringOrNull(fields, "authentication_method"),
                stringMap(fields.get("control_results")),
                stringOrNull(fields, "contract_id"),
                stringOrNull(fields, "contract_version"),
                stringOrNull(fields, "contract_result"),
                stringOrNull(fields, "egress_policy_id"),
                (int) longValue(fields, "transformation_count"),
                stringOrNull(fields, "credential_reference"),
                stringOrNull(fields, "consent_result"),
                stringOrNull(fields, "connector_route"),
                stringOrNull(fields, "delivery_result"),
                BoundaryOutcome.valueOf(string(fields, "boundary_outcome")),
                new AuditChainId(string(fields, "audit_chain_id")),
                longValue(fields, "audit_sequence"),
                new AuditHash(string(fields, "audit_head")),
                new SignerKeyReference(
                        string(fields, "signer_key_id"),
                        string(fields, "signer_key_version"),
                        string(fields, "signer_algorithm")),
                AttestationStatus.valueOf(string(fields, "status")),
                new AttestationSignature(stringOrEmpty(fields, "signature_base64")));
    }

    static Map<String, Object> observation(ObservedCrossingStore.Observation observation) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("integration_id", observation.integrationId());
        fields.put("source", observation.source());
        fields.put("destination", observation.destination());
        fields.put("destination_key", ObservedCrossingStore.destinationKey(observation.destination()));
        fields.put("outcome", observation.outcome());
        fields.put("delivery_result", observation.deliveryResult());
        fields.put("audit_chain_id", observation.auditChainId());
        fields.put("attestation_id", observation.attestationId());
        fields.put("quarantined", observation.quarantined());
        fields.put("occurred_at", observation.occurredAt().toString());
        return fields;
    }

    static Map<String, Object> outbox(OutboxRecord record) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("schema_version", record.schemaVersion());
        fields.put("instruction_id", record.instructionId());
        fields.put("integration_id", record.integrationId());
        fields.put("destination_system", record.destinationSystem());
        fields.put("status", record.status().name());
        fields.put("created_at", record.createdAt().toString());
        fields.put("publish_attempts", record.publishAttempts());
        if (record.lastAttemptAt() != null) {
            fields.put("last_attempt_at", record.lastAttemptAt().toString());
        }
        if (record.publishedAt() != null) {
            fields.put("published_at", record.publishedAt().toString());
        }
        if (record.lastFailureKind() != null) {
            fields.put("last_failure_kind", record.lastFailureKind().name());
        }
        fields.put("instruction", deliveryInstruction(record.instruction()));
        return omitNulls(fields);
    }

    static OutboxRecord outbox(Map<String, Object> fields) {
        PublishFailureKind failure = fields.get("last_failure_kind") == null
                ? null
                : PublishFailureKind.valueOf(string(fields, "last_failure_kind"));
        Instant lastAttempt =
                fields.get("last_attempt_at") == null ? null : Instant.parse(string(fields, "last_attempt_at"));
        Instant published = fields.get("published_at") == null ? null : Instant.parse(string(fields, "published_at"));
        return new OutboxRecord(
                string(fields, "instruction_id"),
                string(fields, "integration_id"),
                string(fields, "destination_system"),
                string(fields, "schema_version"),
                deliveryInstruction(map(fields.get("instruction"))),
                OutboxStatus.valueOf(string(fields, "status")),
                Instant.parse(string(fields, "created_at")),
                (int) longValue(fields, "publish_attempts"),
                lastAttempt,
                published,
                failure);
    }

    static Map<String, Object> deliveryInstruction(DeliveryInstruction instruction) {
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
        return omitNulls(fields);
    }

    static DeliveryInstruction deliveryInstruction(Map<String, Object> fields) {
        CredentialReference credential = null;
        if (fields.get("credential_reference") instanceof Map<?, ?>) {
            Map<String, Object> raw = map(fields.get("credential_reference"));
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
                stringOrNull(fields, "contract_reference"),
                stringOrNull(fields, "egress_policy_reference"),
                BoundaryOutcome.valueOf(string(fields, "boundary_outcome")),
                string(fields, "payload_json"),
                string(fields, "correlation_id"),
                Instant.parse(string(fields, "created_at")),
                credential,
                stringOrNull(fields, "consent_id"));
    }

    static ObservedCrossingStore.Observation observation(Map<String, Object> fields) {
        return new ObservedCrossingStore.Observation(
                stringOrNull(fields, "integration_id"),
                stringOrNull(fields, "source"),
                stringOrNull(fields, "destination"),
                stringOrNull(fields, "outcome"),
                stringOrNull(fields, "delivery_result"),
                stringOrNull(fields, "audit_chain_id"),
                stringOrNull(fields, "attestation_id"),
                bool(fields, "quarantined"),
                Instant.parse(string(fields, "occurred_at")));
    }

    private static Map<String, Object> decision(BoundaryDecision decision) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("transaction_id", decision.transactionId().value());
        fields.put("outcome", decision.outcome().name());
        List<Map<String, Object>> reasons = new ArrayList<>();
        for (ReasonCode reason : decision.reasonCodes()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("code", reason.code());
            row.put("description", reason.description());
            reasons.add(row);
        }
        fields.put("reason_codes", reasons);
        Map<String, Object> controls = new TreeMap<>();
        for (Map.Entry<ControlKind, ControlStatus> entry :
                decision.appliedControls().entrySet()) {
            controls.put(entry.getKey().name(), entry.getValue().name());
        }
        fields.put("applied_controls", controls);
        fields.put("decided_at", decision.decidedAt().toString());
        fields.put("policy_version", decision.policyVersion().value());
        return fields;
    }

    private static BoundaryDecision decisionFrom(Map<String, Object> fields) {
        List<ReasonCode> reasons = new ArrayList<>();
        Object rawReasons = fields.get("reason_codes");
        if (rawReasons instanceof List<?> list) {
            for (Object row : list) {
                Map<String, Object> reason = map(row);
                reasons.add(new ReasonCode(string(reason, "code"), string(reason, "description")));
            }
        }
        EnumMap<ControlKind, ControlStatus> controls = new EnumMap<>(ControlKind.class);
        Map<String, Object> rawControls = map(fields.get("applied_controls"));
        for (Map.Entry<String, Object> entry : rawControls.entrySet()) {
            controls.put(ControlKind.valueOf(entry.getKey()), ControlStatus.valueOf(String.valueOf(entry.getValue())));
        }
        return new BoundaryDecision(
                TransactionId.of(string(fields, "transaction_id")),
                BoundaryOutcome.valueOf(string(fields, "outcome")),
                reasons,
                controls,
                Instant.parse(string(fields, "decided_at")),
                new PolicyVersion(string(fields, "policy_version")));
    }

    private static Map<String, Object> deliveryResult(DeliveryResult result) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("kind", result.kind().name());
        fields.put("attempt_count", result.attemptCount());
        fields.put("provider_category", result.providerCategory().name());
        if (result.reason() != null) {
            fields.put("reason_code", result.reason().code());
            fields.put("reason_description", result.reason().description());
        }
        fields.put("safe_explanation", result.safeExplanation());
        fields.put("duration_ms", result.duration().toMillis());
        fields.put("connector_id", result.connectorId());
        fields.put("route_id", result.routeId());
        fields.put("circuit_state", result.circuitState());
        fields.put("http_status", result.httpStatus());
        if (result.credentialReference() != null) {
            Map<String, Object> credential = new LinkedHashMap<>();
            credential.put("id", result.credentialReference().id().value());
            credential.put("version", result.credentialReference().version().value());
            credential.put("status", result.credentialReference().status().name());
            fields.put("credential_reference", credential);
        }
        fields.put("consent_id", result.consentId());
        fields.put("provider_called", result.providerCalled());
        return omitNulls(fields);
    }

    private static DeliveryResult deliveryResult(Map<String, Object> fields) {
        ReasonCode reason = null;
        if (fields.get("reason_code") != null) {
            reason = new ReasonCode(string(fields, "reason_code"), stringOrEmpty(fields, "reason_description"));
        }
        CredentialReference credential = null;
        if (fields.get("credential_reference") instanceof Map<?, ?>) {
            Map<String, Object> raw = map(fields.get("credential_reference"));
            credential = new CredentialReference(
                    new CredentialId(string(raw, "id")),
                    new CredentialVersion(string(raw, "version")),
                    CredentialStatus.valueOf(string(raw, "status")));
        }
        Integer httpStatus = fields.get("http_status") == null ? null : (int) longValue(fields, "http_status");
        return new DeliveryResult(
                DeliveryResultKind.valueOf(string(fields, "kind")),
                (int) longValue(fields, "attempt_count"),
                ProviderFailureCategory.valueOf(string(fields, "provider_category")),
                reason,
                string(fields, "safe_explanation"),
                Duration.ofMillis(longValue(fields, "duration_ms")),
                stringOrNull(fields, "connector_id"),
                stringOrNull(fields, "route_id"),
                stringOrNull(fields, "circuit_state"),
                httpStatus,
                credential,
                stringOrNull(fields, "consent_id"),
                bool(fields, "provider_called"));
    }

    private static Map<String, Object> omitNulls(Map<String, Object> fields) {
        fields.entrySet().removeIf(entry -> entry.getValue() == null);
        return fields;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        if (value instanceof Map<?, ?> raw) {
            return (Map<String, Object>) raw;
        }
        throw new IllegalArgumentException("expected map");
    }

    private static Map<String, String> stringMap(Object value) {
        if (value == null) {
            return Map.of();
        }
        Map<String, String> result = new TreeMap<>();
        for (Map.Entry<String, Object> entry : map(value).entrySet()) {
            if (entry.getValue() != null) {
                result.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        return result;
    }

    private static String string(Map<String, Object> fields, String key) {
        Object value = fields.get(key);
        if (value == null) {
            throw new IllegalArgumentException("missing field " + key);
        }
        return String.valueOf(value);
    }

    private static String stringOrNull(Map<String, Object> fields, String key) {
        Object value = fields.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static String stringOrEmpty(Map<String, Object> fields, String key) {
        Object value = fields.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static long longValue(Map<String, Object> fields, String key) {
        Object value = fields.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalArgumentException("missing numeric field " + key);
    }

    private static boolean bool(Map<String, Object> fields, String key) {
        Object value = fields.get(key);
        return value instanceof Boolean flag && flag;
    }
}
