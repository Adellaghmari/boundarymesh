# Tamper-evident audit chain

Each relevant security/integration event is appended to a **hash chain**. When the chain is verified, **broken continuity or tampering of stored entries can be detected**.

## Correct vs incorrect claims

**Correct:** verification detects missing sequence, mutated payload, or broken `previous_entry_hash` links.

**Incorrect:** “the database cannot be changed,” “immutable,” “blockchain,” “tamper-proof.”

Firestore remains a mutable store. This is a **detective** control, not a prevention of a privileged administrator.

## Entry fields

```text
chain_id            e.g. integration or global `boundarymesh.v1`
sequence            monotonic integer per chain
timestamp           RFC 3339
event_type          INGEST | DECISION | DELIVERY | QUARANTINE | REPROCESS | ADMIN ...
transaction_id      optional
previous_entry_hash hex SHA-256 of previous canonical entry (64 zeros for genesis)
entry_hash          SHA-256 of this entry’s canonical bytes (excluding entry_hash itself)
metadata            operational fields only (ids, reason codes, hashes)
```

Canonical bytes: RFC 8785 JCS of the entry without `entry_hash`.

`entry_hash = SHA-256( JCS(entry_without_hash) )`

`previous_entry_hash` of item n = `entry_hash` of item n-1.

## What is hashed

Include: sequence, timestamp, event_type, ids, reason codes, decision outcome, payload_sha256, attestation_id.

Exclude: raw payloads, tokens, secrets, emails, PAN-like fields.

## Write protocol

1. Read latest sequence (Firestore transaction).
2. Build entry with that `previous_entry_hash`.
3. Compute `entry_hash`.
4. Create `auditChains/{chainId}/entries/{sequence}` with precondition “does not exist.”
5. Update chain head.

Contention: one chain per **integration** plus one `platform` chain for admin actions. Avoid a single global hotspot.

## Verification API

`GET /api/v1/audit-chains/{chainId}/verify` (public read):

Walk entries in order. Recompute hashes. Report first break:

- `VALID`
- `GAP` (missing sequence)
- `HASH_MISMATCH`
- `LINK_MISMATCH`

Do not “repair” automatically.

## Signed checkpoints (KMS)

Every **N=100** entries, or daily, write `checkpoints/{seq}`:

- `from_sequence`, `to_sequence`
- `head_hash`
- KMS signature over JCS({chain_id, from, to, head_hash})

Checkpoints make long-range verification cheaper and detect silent truncation of the tail if an attacker deletes recent entries **and** the checkpoint still points at the old head (operator compares). An attacker who also deletes checkpoints is a privileged-storage threat; detection then depends on exported logs / object versioning — **not claimed as solved**.

## Failure

If append fails after a decision: security event `AUDIT_CHAIN_APPEND_FAILED`, fail **open** for the already-computed REJECT/ALLOW response only if the decision document exists; otherwise fail closed. Never claim chain validity if append failed.

## Tests

- valid chain
- mutated metadata → HASH_MISMATCH
- swapped previous hash → LINK_MISMATCH
- deleted middle entry → GAP
- checkpoint verifies
- public API does not leak Restricted metadata
