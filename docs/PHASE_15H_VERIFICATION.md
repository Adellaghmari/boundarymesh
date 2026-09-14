# Phase 15H verification

Phase 15H-A/B/C is **PASS**. KMS infrastructure is **LIVE / PROVISIONED**;
the production signer is **LIVE VERIFIED LOCALLY**, not default-wired or Cloud Run workload-live.
Base commit: `28e2c8688b3a09636dd0b4fc6e3f53f8f4b94e70`.

## Live proof (2026-09-14)

Run: `livekms-20260914012902-3d19066b`, completed at 01:29:03 UTC.

Target:
`projects/boundarymesh-dev/locations/europe-west4/keyRings/boundarymesh-attestation/cryptoKeys/exchange-attestation/cryptoKeyVersions/1`.
Before and after signing: ENABLED, `EC_SIGN_P256_SHA256`, SOFTWARE. Key purpose:
`ASYMMETRIC_SIGN`. Exactly one key/version in the inspected ring/key; no rotation or import jobs.

`CloudKmsLiveTest` uses `CloudKmsAttestationSigner` and `GoogleKmsSigningClient`, backed by
the official `KeyManagementServiceClient` and workstation ADC against real GCP. A transparent
test recorder delegates signing/public-key calls to that production client and retains only
the digest and returned signature in memory. It does not mock transport or sign locally.
The recreated signer uses the production `connect` method directly.

The application `AttestationFactory` calls the existing `AttestationCanonicalizer` /
`Rfc8785Canonicalizer`. The synthetic attestation has contract `synthetic-kms-contract/v1`,
egress policy `policy`, outcome ALLOW, a run-scoped audit chain, sequence 1, and a synthetic
audit hash. It produces SIGNED with ES256 and the actual KMS key/version reference.

Two real signing operations succeeded: the factory operation and another sign after closing
and recreating the production client. The original signature also verifies with the recreated
client and the same retrieved public key. No process-local private signing key is used.

The first actual response was ASN.1 DER. The test independently reads both positive INTEGERs,
compares their numeric values and magnitude bytes to the 32-byte P1363 coordinates, checks
zero left padding and complete input consumption, and round-trips to identical DER. The
BoundaryMesh signature decodes from standard Base64 to exactly 64 bytes (`r || s`). A separate
JCA DER verifier validates the original provider response using the retrieved public key.
Deterministic unit cases cover mandatory DER sign padding and short-coordinate left padding;
these edge cases are not assumed to occur in every random live signature.

The recorded digest equals SHA-256 of the existing canonical bytes. The SDK adapter checks
the provider's digest-checksum acknowledgement, response key-version name, and signature CRC32C.
Public-key retrieval validates name, algorithm and PEM CRC32C; parsing checks the full NIST P-256
parameters. The public key and signature remain in memory, with no complete PEM or signature dump.
Integrity checks follow [Google's KMS data integrity guidance](https://cloud.google.com/kms/docs/data-integrity-guidelines).

`AttestationService.verify` returned VALID. Altering only the canonical bytes in memory made
verification fail. The adapter verifies before returning a present signature, so malformed,
corrupted, or cryptographically invalid provider responses cannot mark an attestation SIGNED.

## Opt-in execution and regressions

From `backend`:

```powershell
.\mvnw.cmd verify
.\mvnw.cmd -o -pl gcp -am -Pgcp-live-kms test
```

The second command requires deliberate authorization for real KMS operations. The default
Surefire configuration excludes `gcp-live-kms`; the explicit profile selects only that tag
and excludes the Firestore, Pub/Sub and emulator live suites. Target coordinates and live
algorithm/state/protection are asserted before signing. The harness refuses an explicit
`GOOGLE_APPLICATION_CREDENTIALS` file and uses the workstation ADC convention.

Normal verification was executed as `.\mvnw.cmd -o verify` with
`GOOGLE_APPLICATION_CREDENTIALS` deliberately pointing to a nonexistent file. It passed:
**302 tests**, zero failures/errors/skips, all six reactor modules BUILD SUCCESS. No live
KMS suite or other live GCP suite ran. Offline Maven avoids external dependency downloads;
HTTP test doubles still use localhost. The normal command has no ADC or live GCP requirement.

- KMS unit tests: **21 passed**, including SDK response integrity, malformed DER, P-256
  parsing, controlled failures and the preserved local JCA path.
- Live KMS: **1 end-to-end test passed**, zero failures/errors/skips; two successful real signs.
- ArchUnit: **16 rules passed** across domain, application, GCP, API and worker. Google SDK
  types remain in `backend/gcp`; the KMS SDK remains in its `kms` package.
- Infra: TypeScript typecheck/build passed; **2 tests passed**.
- Shared Firestore/Pub/Sub abstractions were not changed, so no additional emulator or live
  Pub/Sub regression was needed. Existing normal GCP unit tests passed.

Controlled unit doubles cover permission denied, unavailable, deadline/timeout, generic
provider exception, empty/malformed DER, structurally valid but invalid signatures, and a
wrong-curve public key. They produce UNSIGNED without rewriting the outcome, transaction,
contract or audit reference. These are **not live failure tests**: IAM and the key were not changed.

## Infrastructure boundary and cleanup

Pre-live checks confirmed the active gcloud project `boundarymesh-dev`, an active workstation
account, ADC availability, unset `GOOGLE_APPLICATION_CREDENTIALS`, Pulumi backend `file://~`,
and stack `dev`. ADC/token contents were not printed. The documented local Pulumi passphrase
was supplied to the preview process; the initial missing-environment error caused no mutation.

Both pre-live and final `pulumi preview --non-interactive --diff` succeeded: **18 unchanged**,
**0 creates, 0 GCP updates, 0 replacements, 0 deletes**. No informational stack output changed.
The provider warned that Compute Engine API is disabled when looking up regions; it was not enabled.
No `pulumi up` was run in 15H-C. The three KMS resources were applied in **15H-B**, not 15H-C.

Read-only before/after metadata was identical for Artifact Registry, the four Secret Manager
containers, Pub/Sub topics/subscriptions, enabled APIs, project IAM, key/key-ring IAM, service
accounts and import jobs. Each secret still has zero versions. Firestore configuration, UID,
and update time were unchanged; its rolling `earliestVersionTime` and etag advanced naturally.
KMS IAM policies contain no bindings. Existing Google-managed service-agent bindings in the
project policy remain unchanged; these are not BoundaryMesh workload IAM or service accounts.
The existing Firebase Rules service agent/API associated with the foundation was not changed;
no Firebase application/hosting work was performed.

Cloud Run API remains disabled. A read-only service-list request returned SERVICE_DISABLED,
so live service enumeration was unavailable; no Cloud Run resource exists in the Pulumi program
or checkpoint and no deployment was performed. Service-account and WIF-pool lists were empty.
Microsoft/Entra/Graph configuration, production code and live data were untouched.

Firestore persistence was deliberately omitted: Phase 15F already proved
`FirestoreAttestationRepository`, and repeating it would add no KMS-specific integration proof.
There were **zero cloud document writes and zero application-data cleanup operations**.
Clients were closed. KMS service audit records may naturally record the allowed operations.

Production code contains only signing/public-key operations and public-key parsing; it does
not load, export, serialize, store, log or persist a private key. The KMS private key remains
inside KMS. Source/diff/log security review found no real credential, ADC JSON, secret payload,
service-account key, Docker credential, private key or customer data. Private-key/token-like
test strings are visibly synthetic fixtures. Build reports/logs remain ignored build artifacts.

## Takeover discrepancy resolutions

1. `THREAT_MODEL.md` now distinguishes default/local/test without KMS, explicitly authorized
   opt-in live verification, and future production composition after Phase 15I workload IAM.
2. Roadmap/status/claims docs distinguish KMS provisioning in 15H-B and live signing in 15H-C.
   Workload IAM remains PLANNED FOR 15I; Cloud Run workload signing is not claimed.
3. `phase15HScope = "KMS_PREVIEW"` is retained as the historical 15H-A implementation-scope
   marker, consistent with preceding phase markers. A source comment and `PULUMI.md` explain
   that it does not report deployment status. No output value, state, or GCP resource was altered.

Phase 15I is **not started**. Recommended next scope: review least-privilege workload identities,
KMS sign/public-key permissions and explicit production composition before any IAM or deployment
operation. A separate authorization is required. No Git push is part of Phase 15H completion.
