# Cryptographic Exchange Attestation

A **Cryptographic Exchange Attestation** is a machine-verifiable artifact that proves:

- what BoundaryMesh verified
- which rules were enforced
- what action occurred
- that the attestation bytes have not been modified without detection after signing

It is **not** a Truth Receipt. It does **not** prove that business data is true. It does **not** prove that Microsoft 365 “really happened” beyond what was verified.

## Fields (schema version `attestation.v1`)

| Field | Purpose |
| --- | --- |
| `attestation_id` | ULID |
| `attestation_schema_version` | `1` |
| `transaction_id` | crossing id |
| `integration_id` | |
| `source_system` | e.g. `HELIX_365` |
| `destination_system` | e.g. `COVECRM` |
| `occurred_at` | RFC 3339 UTC |
| `identity_subject_ref` | opaque reference (hashed oid/appid), not a token |
| `authentication_method` | enum |
| `authentication_result` | enum |
| `authorization_result` | enum |
| `authenticity_result` | enum |
| `replay_result` | enum |
| `contract_id` | |
| `contract_version` | |
| `contract_result` | enum |
| `egress_policy_version` | |
| `redaction_summary` | `{ redacted_count, denied_count, accepted_count }` — **no values** |
| `credential_ref` | resource name without version secret |
| `credential_version` | integer metadata |
| `delivery_result` | `PENDING` / `SUCCESS` / `FAILED` / `NOT_ATTEMPTED` |
| `boundary_outcome` | ALLOW / ALLOW_WITH_TRANSFORMATION / QUARANTINE / REJECT |
| `reason_codes` | string array, sorted |
| `applied_rules` | string array, sorted |
| `audit_chain_ref` | chain id + sequence |
| `payload_sha256` | hex of canonical **transformed** payload or `null` if none |
| `kms_key` | `projects/.../cryptoKeys/...` |
| `kms_key_version` | |
| `signature_algorithm` | `EC_SIGN_P256_SHA256` |
| `signature_b64` | over canonical bytes **excluding** this field and `unsigned_status` |
| `unsigned_status` | `SIGNED` / `UNSIGNED` / `VERIFY_FAILED` |

## Canonical serialization

**RFC 8785 JSON Canonicalization Scheme (JCS)** over a JSON object that:

1. Includes all fields above except `signature_b64`.
2. Uses UTF-8.
3. Has sorted reason/rule arrays already at the object model (deterministic input).
4. Does **not** include raw payloads, tokens, or secrets.

Signing `ObjectMapper.writeValueAsString` without JCS is **forbidden**.

Byte sequence:

```text
canonical_bytes = JCS(attestation_without_signature)
digest          = SHA-256(canonical_bytes)   // KMS EC_SIGN_P256_SHA256 signs a digest as required by the API
signature       = Cloud KMS AsymmetricSign
```

Use the Google Cloud KMS Java client / Tink-compatible path. Private key **never** leaves KMS. Do not implement ECDSA by hand.

## Verification

Public API `POST /api/v1/attestations/{id}/verify` (public, rate-limited):

1. Load attestation document.
2. Fetch KMS **public** key for `kms_key_version` (cacheable).
3. Recompute JCS bytes.
4. Verify ECDSA P-256 SHA-256.
5. Return `{ valid: true|false, checked_at, key_version }` — no key material.

If `unsigned_status != SIGNED`, verification returns `valid: false` with reason `UNSIGNED`.

## KMS layout

| Resource | Value |
| --- | --- |
| Location | `europe-west4` |
| Key ring | `boundarymesh` |
| Key | `exchange-attestation` |
| Purpose | `ASYMMETRIC_SIGN` |
| Algorithm | `EC_SIGN_P256_SHA256` |
| Protection | `SOFTWARE` (portfolio cost; HSM not required) |
| Rotation | new version; attestations record the version used |

Key rotation does not rewrite old attestations. Verify uses the **recorded** version’s public key. Destroyed versions: verification reports `KEY_VERSION_DESTROYED` (detective).

## Signing failure

If KMS is down after a decision:

- Persist decision and audit entry.
- Attestation stored with `unsigned_status=UNSIGNED`, `reason_codes` include `ATTESTATION_SIGNING_FAILED`.
- Do **not** silently claim a signature.
- Delivery of ALLOW may still proceed (enforcement already happened). UI shows attestation unsigned.

Fail **closed** on claiming validity. Fail **open** on delivery only if the engine already ALLOWED — documented explicitly so interviews stay honest.

## What we will never claim

- “This attestation proves the calendar meeting occurred in the real world.”
- “This is a blockchain timestamp.”
- “Business amounts are factually correct.”
