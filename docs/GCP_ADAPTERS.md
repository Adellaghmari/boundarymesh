# GCP adapter module

`backend/gcp` (`boundarymesh-gcp`) is the only home for Google Cloud application adapters.

Domain and application stay cloud-provider independent. Local in-memory adapters remain the default. API and worker do **not** depend on this module yet.

## Runtime modes

| Mode | Meaning |
| --- | --- |
| `LOCAL` | Default application path. In-memory / local adapters. |
| `TEST` | Deterministic tests. Same local adapters. |
| `GCP_EMULATOR` | Opt-in JUnit tag `gcp-emulator`. Firestore emulator adapters allowed. Not part of default `mvnw verify`. |
| `PROD` | May select Secret Manager and Firestore production adapters when a later phase wires `backend/gcp` under an explicit cloud profile. Live Java SDK path is **LIVE VERIFIED LOCALLY** via opt-in `gcp-live`; not the local API/worker default. |

`local` remains the API/worker Spring default. Profile `gcp-emulator` is documented only; it is not activated at runtime.

## Emulator tests

Default `backend/mvnw.cmd verify` does not need ADC, Docker, a running emulator, or live GCP. Surefire excludes the `gcp-emulator` and `gcp-live` groups.

Firestore adapter proofs start the Cloud Firestore emulator with firebase-tools on the host and named database ID `boundarymesh`. Emulator tests use the emulator REST API for that named database. Production construction is `GoogleFirestoreFacade` plus the official Java Firestore SDK. Run the opt-in suites with:

```text
backend/mvnw.cmd -pl gcp -am -Pgcp-emulator test
backend/mvnw.cmd -pl gcp -am -Pgcp-live test
```

Local emulator host placeholders remain:

- Firestore: `127.0.0.1:18085`
- Pub/Sub: `127.0.0.1:18086`

Secret Manager and Cloud KMS have no emulator path; Secret Manager uses local doubles until live verification.

## Port map

| Port | Local implementation today | GCP implementation | Production adapter |
| --- | --- | --- | --- |
| `AuditRepository` | `InMemoryAuditRepository` | `FirestoreAuditRepository` | **IMPLEMENTED + EMULATOR TESTED + LIVE VERIFIED LOCALLY** |
| `AttestationRepository` | `InMemoryAttestationRepository` | `FirestoreAttestationRepository` | **IMPLEMENTED + EMULATOR TESTED + LIVE VERIFIED LOCALLY** |
| `AttestationSigner` | `JcaP256AttestationCrypto` | Cloud KMS `EC_SIGN_P256_SHA256` | PLANNED |
| `QuarantineRepository` | `InMemoryQuarantineRepository` | `FirestoreQuarantineRepository` | **IMPLEMENTED + EMULATOR TESTED + LIVE VERIFIED LOCALLY** |
| `ReplayNonceStore` | `InMemoryReplayNonceStore` | `FirestoreReplayNonceStore` | **IMPLEMENTED + EMULATOR TESTED + LIVE VERIFIED LOCALLY** |
| `IdempotencyStore` | `InMemoryIdempotencyStore` | `FirestoreIdempotencyStore` | **IMPLEMENTED + EMULATOR TESTED + LIVE VERIFIED LOCALLY** |
| `DeliveryIdempotencyStore` | `InMemoryDeliveryIdempotencyStore` | `FirestoreDeliveryIdempotencyStore` | **IMPLEMENTED + EMULATOR TESTED + LIVE VERIFIED LOCALLY** |
| `SecretValueProvider` | `LocalSecretValueProvider` | `SecretManagerSecretValueProvider` | **IMPLEMENTED** (not default-wired) |
| `WebhookSecretProvider` | `LocalWebhookSecretProvider` | `SecretManagerWebhookSecretProvider` | **IMPLEMENTED** (not default-wired) |
| `ObservedCrossingStore` | `InMemoryObservedCrossingStore` | `FirestoreObservedCrossingStore` | **IMPLEMENTED + EMULATOR TESTED + LIVE VERIFIED LOCALLY** |
| Delivery outbox | ABSENT (not a port yet) | Firestore outbox then Pub/Sub publish | PLANNED FOR PHASE 15G |

## Phase 15F Firestore truth

| Item | Status |
| --- | --- |
| Firestore persistence architecture | **IMPLEMENTED + TESTED** |
| Firestore production repository adapters | **IMPLEMENTED + EMULATOR TESTED + LIVE VERIFIED LOCALLY** |
| `GoogleFirestoreFacade` | **LIVE VERIFIED AGAINST REAL GCP FIRESTORE** |
| Firestore transactional audit append | **EMULATOR TESTED + LIVE VERIFIED** |
| Firestore replay/idempotency atomicity | **EMULATOR TESTED + LIVE VERIFIED** |
| Quarantine / attestation / observed crossing | **LIVE VERIFIED** |
| Restart / client recreation persistence | **LIVE VERIFIED** |
| Firestore database Pulumi definition | **IMPLEMENTED + APPLIED** |
| Firestore API in GCP | **LIVE / ENABLED IN GCP** |
| Firestore database in GCP | **LIVE / PROVISIONED IN GCP** |
| Firestore database ID | `boundarymesh` |
| Firestore location | `europe-west4` |
| Firestore database type | **FIRESTORE_NATIVE / STANDARD** |
| Live verification residue | **CLEANED / ZERO RUN-SCOPED DOCUMENTS REMAIN** |
| Outbox | **PLANNED FOR PHASE 15G** |
| TTL policy | **PLANNED / REVIEW LATER** |

Do **not** claim Cloud Run workload persistence is live. API/worker still use in-memory defaults.

## Phase 15E secret inventory

Pulumi **manages** empty Secret Manager **containers**. They are LIVE in GCP. No versions. No IAM.

| Secret ID | Category | Why |
| --- | --- | --- |
| `hmac-quayforge` | A / later Cloud Run env | QuayForge webhook HMAC behind `WebhookSecretProvider` |
| `covecrm-api` | A | Worker `SecretValueProvider` credential `covecrm-api` |
| `lumenway-api` | A | Worker `SecretValueProvider` credential `lumenway-api` |
| `rivertide-api` | A | Worker `SecretValueProvider` credential `rivertide-api` |

Not created in 15E:

| Secret | Category | Why deferred |
| --- | --- | --- |
| Entra client secret | B / 15I | Stays `BOUNDARYMESH_ENTRA_CLIENT_SECRET`. Cloud Run env-from-secret later |
| Graph `clientState` | C | Graph subscriptions remain PLANNED |

API and worker do **not** depend on `backend/gcp` in this phase. Local providers remain the default.

Do not add KMS or Cloud Run here.
