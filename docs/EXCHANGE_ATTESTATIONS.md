# Cryptographic Exchange Attestation

A **Cryptographic Exchange Attestation** is a signed summary of one terminal crossing. It proves:

- which canonical attestation bytes were signed
- which local key identity/version signed them
- that those bytes have not been changed without signature verification failing

It does **not** prove that source business data is true, that an external business action occurred beyond BoundaryMesh’s own delivery evidence, or that the infrastructure was uncompromised.

Do not rename this to Trust Receipt, Truth Receipt, Assurance Packet, or Decision Evidence Packet.

## Lifecycle

One final attestation is created at a stable terminal point after the boundary decision and delivery/quarantine outcome are known. The signed document is not mutated later. Signing failure does **not** rewrite the crossing as REJECT after delivery; the attestation is recorded as `UNSIGNED`.

## Domain

| Type | Role |
| --- | --- |
| `ExchangeAttestation` | Immutable payload + status + signature |
| `AttestationId` | Identifier |
| `AttestationVersion` | `attestation/v1` |
| `AttestationSignature` | Base64 IEEE P1363 ECDSA bytes, or empty |
| `SignerKeyReference` | `keyId`, `keyVersion`, `algorithm` — never a private key |
| `AttestationStatus` | `SIGNED` / `UNSIGNED` |
| `SignatureVerificationStatus` | `VALID` / `INVALID` / `UNSIGNED` / `UNVERIFIABLE` |

## Canonical bytes

RFC 8785 of the payload **excluding** `status` and `signature`. Property order and whitespace do not change the bytes. A semantic field change does.

Signed with JCA `SHA256withECDSAinP1363Format` (ECDSA P-256 + SHA-256). Algorithm recorded as `ES256`. Signature encoding: standard Base64 of IEEE P1363 (`r||s`, 64 bytes). Hash encoding elsewhere: lowercase hex.

Production Cloud KMS uses `EC_SIGN_P256_SHA256` over a locally computed SHA-256 digest. Provider signatures are ASN.1 DER. `CloudKmsAttestationSigner` normalizes DER → IEEE P1363 before constructing `AttestationSignature`. The application-level format does not change.

Phase 15H-C **LIVE VERIFIED LOCALLY** the real SDK/ADC path against `exchange-attestation` version 1. Phase 15I-D1 additionally verified the deployed API workload produced a `SIGNED` ES256 attestation through the Cloud KMS signer; the adapter validates response integrity and verifies the normalized signature with the retrieved NIST P-256 public key before returning a present signature. Failures produce UNSIGNED while preserving the crossing outcome. See [live proof and opt-in commands](PHASE_15H_VERIFICATION.md).

## Ports

`AttestationSigner` / `AttestationVerifier` are cloud-independent. Local adapter: `JcaP256AttestationCrypto` (default for local, test, and `gcp-emulator`). Production adapter: `CloudKmsAttestationSigner` in `backend/gcp` (**IMPLEMENTED + UNIT TESTED + DEPLOYED + WORKLOAD LIVE VERIFIED**). The application module does not depend on a GCP SDK. Private key material never leaves Cloud KMS. Verification uses the KMS public key locally.

## Local keys

Key id: `local-jca-p256`. Key version: ephemeral UUID generated at process start. **Signatures will not verify across restart.** No production key is committed.

If the signer throws or returns no signature, status is `UNSIGNED`. A fake signature is never generated.

## Audit linkage

Each attestation stores `audit_chain_id`, `audit_sequence`, and `audit_head` (SHA-256 hex of the chain head at signing time). Audit verification checks event continuity. Attestation verification checks the signed artifact. The signature does not make every audit entry permanently immutable.

## API

- `GET /api/v1/attestations` (latest 50)
- `GET /api/v1/attestations/{id}`
- `GET` / `POST /api/v1/attestations/{id}/verify` (Java is authoritative)
- `GET /api/v1/dev/attestation-lab/tamper-demo` — isolated destination tamper → `INVALID`

No public signing endpoint.

## Storage truth

In-memory repository is the local default. Firestore attestation evidence is persisted by the production composition and the D1 signed attestation was intentionally preserved. Private keys are not stored in the repository. Cloud KMS private key material never leaves KMS.
