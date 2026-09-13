# Data model (Firestore)

Access patterns first. Firestore Native, regional `europe-west4`, named database `boundarymesh` (never the default database). The Phase 15F-A Pulumi database uses **PESSIMISTIC** concurrency. This is **not** a SQL schema dump.

Phase 15F-C live-verified the official Java SDK path against `projects/boundarymesh-dev/databases/boundarymesh`, then deleted the run-scoped documents. Residue is **CLEANED**. The delivery outbox remains Phase 15G. Cloud Run workload persistence is **not** live.

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

See [EXCHANGE_ATTESTATIONS.md](EXCHANGE_ATTESTATIONS.md). Public signature + signer **reference** only. No private key material.

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

References and rotation metadata only. [SECRET_MODEL.md](SECRET_MODEL.md).

### `contracts/{contractId}/versions/{version}`

Schema body (JSON Schema) is OK to store; not secret. Source of truth remains git `contracts/`.

### `egressPolicies/{policyId}/versions/{version}`

Rule tables.

### `securityEvents/{eventId}`

JWKS failure, audit append failure, signing failure.

### `graphSubscriptions/{subscriptionId}`

Graph subscription metadata, expiry, integration_id. No `clientState` value.

### `projectIdentity/current`

Git SHA, Cloud Run revisions, Pulumi stack, AppSec summary — **live project data**, labeled separately from synthetic transactions.

## Indexes

Composite: `transactions`: `synthetic` + `occurred_at`; `quarantine`: `status` + `opened_at`. Keep indexes minimal (cost).

## Why not Postgres or DynamoDB

Project A already demonstrates PostgreSQL. Project B already demonstrates DynamoDB. Firestore matches serverless GCP and the key/value + document access patterns above. If a later phase proves query patterns that Firestore cannot serve (heavy ad-hoc analytics), the fallback is **BigQuery export**, not a second OLTP database. BigQuery is **OPTIONAL** and not in Phase 0 provision.

## Consistency

Decision + transaction + audit entry: Firestore transaction where possible. Pub/Sub publish **after** commit (outbox field `publish_state` on the decision: `PENDING` → worker/API publisher). If publish fails, a sweeper republishes. Do not publish before persist (would deliver without evidence).
