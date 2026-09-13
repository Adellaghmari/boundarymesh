# GCP adapter module

Phase 15C introduces `backend/gcp` (`boundarymesh-gcp`) as the only home for future Google Cloud application adapters.

Domain and application stay cloud-provider independent. Local in-memory adapters remain the default. API and worker do **not** depend on this module yet.

## Runtime modes

| Mode | Meaning |
| --- | --- |
| `LOCAL` | Default application path. In-memory / local adapters. |
| `TEST` | Deterministic tests. Same local adapters. |
| `GCP_EMULATOR` | Reserved. Opt-in JUnit tag `gcp-emulator`. Not part of default `mvnw verify`. |
| `PROD` | May select Secret Manager adapters when a later phase wires `backend/gcp` under an explicit cloud profile. Not active in 15E. |

`local` remains the API/worker Spring default. Profile `gcp-emulator` is documented only; it is not activated.

## Emulator tests

Default `backend/mvnw.cmd verify` does not need ADC, Docker, or a running emulator. Surefire excludes the `gcp-emulator` group.

Later Firestore and Pub/Sub adapter tests should use local emulator endpoints:

- Firestore: `127.0.0.1:18085`
- Pub/Sub: `127.0.0.1:18086`

Testcontainers may be added in 15F/15G if an official emulator image is justified. Secret Manager and Cloud KMS have no emulator path in this phase; use local doubles until live verification.

## Port map

| Port | Local implementation today | Planned GCP implementation | Production adapter |
| --- | --- | --- | --- |
| `AuditRepository` | `InMemoryAuditRepository` | Firestore Native | PLANNED |
| `AttestationRepository` | `InMemoryAttestationRepository` | Firestore Native | PLANNED |
| `AttestationSigner` | `JcaP256AttestationCrypto` | Cloud KMS `EC_SIGN_P256_SHA256` | PLANNED |
| `QuarantineRepository` | `InMemoryQuarantineRepository` | Firestore Native | PLANNED |
| `ReplayNonceStore` | `InMemoryReplayNonceStore` | Firestore Native TTL | PLANNED |
| `IdempotencyStore` | `InMemoryIdempotencyStore` | Firestore Native | PLANNED |
| `DeliveryIdempotencyStore` | `InMemoryDeliveryIdempotencyStore` | Firestore Native | PLANNED |
| `SecretValueProvider` | `LocalSecretValueProvider` | `SecretManagerSecretValueProvider` | **IMPLEMENTED** (not default-wired) |
| `WebhookSecretProvider` | `LocalWebhookSecretProvider` | `SecretManagerWebhookSecretProvider` | **IMPLEMENTED** (not default-wired) |
| `ObservedCrossingStore` | In-process store | Firestore Native | PLANNED |
| Delivery outbox | ABSENT (not a port yet) | Firestore outbox then Pub/Sub publish | PLANNED |

## Next

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

Phase 15F+ may add Firestore/Pub/Sub adapters. Do not add KMS or Cloud Run here.
