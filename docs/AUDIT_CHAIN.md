# Tamper-evident audit chain

BoundaryMesh records a **hash-chained security/integration audit**. Verification can detect broken continuity or retrospective modification of stored entries. The local repository is still mutable memory.

## Correct language

**Correct:** tamper-evident, hash-chained, continuity-verifiable.

**Incorrect:** immutable database, blockchain, tamper-proof, cryptographically prevents all modification.

A privileged process can still delete or rewrite storage. Verification **detects** that when it is run. It does not prevent a storage administrator from changing bytes.

## Chain identity

One chain per integration (`AuditChainId` = integration slug, for example `quayforge-orders`). That avoids a single global lock. Firestore transactional append is **EMULATOR TESTED + LIVE VERIFIED**. The Phase 15I-D1 `quayforge-orders` chain was verified in the deployed Cloud Run workload and is retained as synthetic live-verification evidence.

## Domain

| Type | Role |
| --- | --- |
| `AuditChainId` | Integration-scoped chain |
| `AuditEntryId` | Entry identity (not hashed) |
| `AuditEntry` | Sequence, timestamp, event type, safe attributes, previous hash, current hash |
| `AuditHash` | SHA-256 as 64 lowercase hex characters. Genesis is 64 zeros |
| `AuditChainHead` | Latest sequence + head hash |
| `AuditVerificationResult` | `VALID`, `EMPTY`, `SEQUENCE_GAP`, `LINK_MISMATCH`, `HASH_MISMATCH`, `REORDERED`, `FOREIGN_CHAIN` |

## Events recorded

Pipeline records structured events, not every method call:

`BOUNDARY_DECISION`, `DELIVERY_INSTRUCTION_CREATED`, `DELIVERY_RESULT`, `QUARANTINE_CREATED`, `REPROCESS_STARTED`, `QUARANTINE_RESOLVED`, `ATTESTATION_CREATED`.

Safe attributes only: transaction id, destination, outcome, reason code, control statuses, contract/credential references, connector/route ids, attempt count. Never JWT, HMAC secrets, tokens, private keys, or sensitive payload fields.

## Canonicalization

Hash input is RFC 8785 JSON Canonicalization Scheme for a constrained model (objects, arrays, strings, integers, booleans, null). Floating-point numbers are rejected. Object keys are sorted lexicographically. UTF-8 bytes are hashed.

Implementation: `Rfc8785Canonicalizer` in the application layer. No third-party JCS library: Titanium JCS tracks Jackson 2; Spring Boot 4 uses Jackson 3.

Canonical map for an entry (**excludes** `id` and `current_hash`):

```text
attributes       object of string→string, keys sorted
chain_id         string
event_type       enum name
occurred_at      Instant.toString()
previous_hash    64 lowercase hex
schema_version   "audit-entry/v1"
sequence         integer
```

`current_hash = lowercase hex(SHA-256(RFC8785(canonical map)))`

Entry 1: `previous_hash = GENESIS` (64 zeros).  
Entry n: `previous_hash = entry n-1 current_hash`.

## Concurrency

`AuditAppender` delegates atomic append to `AuditRepository.appendNext`. `InMemoryAuditRepository` holds a `ReentrantLock` per chain. `FirestoreAuditRepository` commits head read, entry create, and head advancement in one Firestore transaction. Two concurrent events cannot claim the same sequence.

## Verifier

`AuditChainVerifier` walks entries in order and checks sequence continuity, previous-hash linkage, foreign-chain membership, and recalculated SHA-256. It does not merely compare a stored head hash.

## API

Read-only:

- `GET /api/v1/audit/chains`
- `GET /api/v1/audit/chains/{integrationId}` (latest 100 entries; verification uses the full in-memory chain)
- `GET /api/v1/audit/chains/{integrationId}/verify`

`GET /api/v1/dev/audit-lab/tamper-demo` (local/dev): isolated copy. Original verifies VALID. Metadata-tampered copy HASH_MISMATCH. Live repository is not mutated.

## Storage truth

Local default remains in-memory and does not survive restart. Firestore adapter is **IMPLEMENTED + EMULATOR TESTED + LIVE VERIFIED**. Database is **LIVE / PROVISIONED**. D1 audit entries are append-only evidence and were preserved after cleanup.
