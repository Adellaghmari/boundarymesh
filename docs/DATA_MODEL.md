# Data model (Firestore)

Access patterns first. Firestore Native, regional `europe-west4`, named database `boundarymesh` (never the default database). The database uses pessimistic concurrency. This is not a SQL schema dump.

The Java SDK path is verified against `projects/boundarymesh-dev/databases/boundarymesh`. One deployed workload transaction completed end to end. Disposable outbox and delivery-idempotency records from that check were removed. Signed attestation and append-only audit evidence were kept. The delivery outbox is emulator tested and live verified. Pub/Sub topics and the pull subscription are provisioned.

## Patterns

| Question | Pattern |
| --- | --- |
| Get transaction + decision | `transactions/{id}` + `boundaryDecisions/{id}` (same id) |
| List recent events | `transactions` collection query `orderBy occurred_at desc` with fields needed for list (denormalized summary) |
| Replay nonce exists? | point get `replayNonces/{integrationId}_{nonce}` |
| Idempotent delivery? | point get `idempotency/{key}` |
| Quarantine queue | `quarantine` where `status==OPEN` orderBy `opened_at` |
| Attestation by id | `attestations/{id}` |
| Audit append | transaction on `auditChains/{id}` head + `entries/{seq}` |
| Connector status | `connectors/{id}` |
| Integration config | `integrations/{id}` |

Avoid collection-group scans for hot paths.

## Collections

### `integrations/{integrationId}`

Source, destination, inbound contract ref, egress policy ref, ACL, mode (`LIVE`|`HARNESS`), `synthetic: true`.

### `transactions/{transactionId}`

Summary: source, dest, outcome, reason_codes, occurred_at, identity_method, contract_version, `synthetic`, `payload_sha256`. **No raw Restricted body.**

Optional subcollection `stages/{stage}` for per-stage evidence.

### `boundaryDecisions/{transactionId}`

Full `EnforcementDecision` JSON (safe).

### `replayNonces/{docId}` — **IMPLEMENTED** (emulator-tested + live-verified locally)

`docId` = sanitized nonce hash (already hashed at the port). Fields: `nonce_hash`, `claimed_at`, `expires_at` (ISO-8601), `expires_at_ms`. Create-if-absent in a transaction. TTL **cloud policy is PLANNED**; the expiry field exists for later cleanup.

### `idempotency/{key}` — **IMPLEMENTED** (emulator-tested + live-verified locally)

Request/event idempotency (`IdempotencyStore`). First writer wins. Fields: `event_key`, `decision` (safe `BoundaryDecision`), `created_at`.

### `deliveryIdempotency/{deliveryInstructionId}` — **IMPLEMENTED** (emulator-tested + live-verified locally)

Delivery duplicate suppression (`DeliveryIdempotencyStore`). First writer wins. Fields: `delivery_instruction_id`, `result` (safe `DeliveryResult`), `created_at`. Not merged with request idempotency.

### `quarantine/{quarantineId}` — **IMPLEMENTED** (emulator-tested + live-verified locally)

Identity, boundary/outcome references, reason metadata, status/lifecycle, recovery attempts. No secrets or raw auth material.

### `attestations/{attestationId}` — **IMPLEMENTED** (emulator-tested + live-verified locally)

Public signature and signer reference only. No private key material. Cloud KMS signs the attestation. The private key does not leave KMS.

### `deliveryOutbox/{instructionId}` — **IMPLEMENTED** (emulator-tested + live-verified locally)

Document id is the stable `DeliveryInstruction` id. Fields: `schema_version` (`delivery-outbox/v1`), `instruction_id`, `integration_id`, `destination_system`, `status` (`PENDING`|`PUBLISHED`), `created_at`, `publish_attempts`, optional `last_attempt_at` / `published_at` / `last_failure_kind`, and nested `instruction` (approved payload + credential **reference** only).

Create-pending and status updates are Firestore transactions. Pending load is `status == PENDING` (single-field; no composite index). No secrets, tokens, or Authorization headers.

Pub/Sub publish happens **after** this document exists. A publish can succeed while mark-`PUBLISHED` fails; a later retry may emit a duplicate message. That is at-least-once. The worker deduplicates with `deliveryIdempotency/{instructionId}`.

### `observedCrossings/{destinationKey}` — **IMPLEMENTED** (emulator-tested + live-verified locally)

Latest derived observation per destination. Graph evidence, not an independently mutable source of truth.

### `auditChains/{chainId}` — **IMPLEMENTED** (emulator-tested + live-verified locally)

Head: `chain_id`, `sequence`, `head_hash`, `updated_at`.

`auditChains/{chainId}/entries/{zeroPaddedSequence}`  
Entry fields keep `occurred_at` as `Instant.toString()` so the hash input is exact.

Head read + entry create + head advance commit in one Firestore transaction.

`auditChains/{chainId}/checkpoints/{toSequence}` remains **PLANNED**.

### `connectors/{connectorId}`

Circuit state, last HTTP status (no body), credential_id, Graph subscription ids.

### `credentials/{credentialId}`

References and rotation metadata only. Secret values stay in Secret Manager.

### `contracts/{contractId}/versions/{version}`

Schema body (JSON Schema) is OK to store; not secret. Source of truth remains git `contracts/`.

### `egressPolicies/{policyId}/versions/{version}`

Rule tables.

### `securityEvents/{eventId}`

JWKS failure, audit append failure, signing failure.

### `graphSubscriptions/{subscriptionId}`

Graph subscription metadata, expiry, integration_id. No `clientState` value.

### `projectIdentity/current`

Described as a place for Git SHA, Cloud Run revisions, and stack summary. The current services do not write this document.

## Indexes

Composite: `transactions`: `synthetic` + `occurred_at`; `quarantine`: `status` + `opened_at`. Keep indexes minimal (cost).

## Why not Postgres or DynamoDB

Firestore matches the key and document access patterns above. Heavy ad-hoc analytics are not a current requirement. BigQuery export is not provisioned.

## Consistency

The transactional unit for approved delivery is the `deliveryOutbox` document (instruction snapshot + `PENDING`). Firestore and Pub/Sub are **not** one distributed transaction. Publish after persist. If publish fails, the record stays `PENDING` and is retryable. Do not publish before persist.
