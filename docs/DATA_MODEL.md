# Data model (Firestore)

Access patterns first. Firestore Native, regional `europe-west4`, optimistic concurrency. This is **not** a SQL schema dump.

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

### `replayNonces/{docId}`

`docId = integrationId + '_' + nonce`. TTL. `transactionId`, `seenAt`.

### `idempotency/{key}`

`key` = hash of `integration + destination + idempotency_key`. Delivery result, `created_at`, TTL ≥ 7d.

### `quarantine/{quarantineId}`

Reason, stage, retry_eligible, reprocess_of, connector_id, opened_at, status.

### `attestations/{attestationId}`

See [EXCHANGE_ATTESTATIONS.md](EXCHANGE_ATTESTATIONS.md). `transaction_id` index.

### `auditChains/{chainId}`

Head sequence + head hash.

`auditChains/{chainId}/entries/{sequence}`  
`auditChains/{chainId}/checkpoints/{toSequence}`

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
