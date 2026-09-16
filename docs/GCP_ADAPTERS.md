# GCP adapter module

`backend/gcp` (`boundarymesh-gcp`) is the only home for Google Cloud application adapters.

Domain and application stay cloud-provider independent. Local in-memory adapters remain the
default. API and worker bootstrap packages depend on this module only for the explicit `prod`
composition root introduced in Phase 15I-A.

## Runtime modes

| Mode | Meaning |
| --- | --- |
| `LOCAL` | Default application path. In-memory / local adapters. |
| `TEST` | Deterministic tests. Same local adapters. |
| `GCP_EMULATOR` | Opt-in JUnit tag `gcp-emulator`. Firestore emulator adapters allowed. Not part of default `mvnw verify`. |
| `PROD` | Explicitly composes Firestore, Pub/Sub, Cloud KMS, and Secret Manager adapters in API/worker bootstrap boundaries. **DEPLOYED + READY; one internal cloud transaction was LIVE VERIFIED in 15I-D1.** |

`local` remains the API/worker Spring default. Profile `gcp-emulator` is documented only; it is not activated at runtime.

## Emulator tests

Default `backend/mvnw.cmd verify` does not need ADC, Docker, a running emulator, or live GCP. Surefire excludes the `gcp-emulator`, `gcp-live`, and `gcp-live-pubsub` groups.

Firestore adapter proofs start the Cloud Firestore emulator with firebase-tools on the host and named database ID `boundarymesh`. Emulator tests use the emulator REST API for that named database. Production construction is `GoogleFirestoreFacade` plus the official Java Firestore SDK. Run the opt-in suites with:

```text
backend/mvnw.cmd -pl gcp -am -Pgcp-emulator test
backend/mvnw.cmd -pl gcp -am -Pgcp-live test
backend/mvnw.cmd -pl gcp -am -Pgcp-live-pubsub test
```

Local emulator host placeholders remain:

- Firestore: `127.0.0.1:18085`
- Pub/Sub: `127.0.0.1:18086`

Secret Manager and Cloud KMS have no emulator path. Secret Manager uses local doubles until live verification. Cloud KMS unit tests use a fake signing client; `gcp-emulator` keeps the JCA signer.

## Port map

| Port | Local implementation today | GCP implementation | Production adapter |
| --- | --- | --- | --- |
| `AuditRepository` | `InMemoryAuditRepository` | `FirestoreAuditRepository` | **IMPLEMENTED + EMULATOR TESTED + LIVE VERIFIED LOCALLY** |
| `AttestationRepository` | `InMemoryAttestationRepository` | `FirestoreAttestationRepository` | **IMPLEMENTED + EMULATOR TESTED + LIVE VERIFIED LOCALLY** |
| `AttestationSigner` | `JcaP256AttestationCrypto` | Cloud KMS `EC_SIGN_P256_SHA256` | **IMPLEMENTED + UNIT TESTED + PROD-COMPOSED + DEPLOYED + WORKLOAD LIVE VERIFIED** |
| `QuarantineRepository` | `InMemoryQuarantineRepository` | `FirestoreQuarantineRepository` | **IMPLEMENTED + EMULATOR TESTED + LIVE VERIFIED LOCALLY** |
| `ReplayNonceStore` | `InMemoryReplayNonceStore` | `FirestoreReplayNonceStore` | **IMPLEMENTED + EMULATOR TESTED + LIVE VERIFIED LOCALLY** |
| `IdempotencyStore` | `InMemoryIdempotencyStore` | `FirestoreIdempotencyStore` | **IMPLEMENTED + EMULATOR TESTED + LIVE VERIFIED LOCALLY** |
| `DeliveryIdempotencyStore` | `InMemoryDeliveryIdempotencyStore` | `FirestoreDeliveryIdempotencyStore` | **IMPLEMENTED + EMULATOR TESTED + LIVE VERIFIED LOCALLY** |
| `SecretValueProvider` | `LocalSecretValueProvider` | `SecretManagerSecretValueProvider` | **IMPLEMENTED + PROD-COMPOSED + DEPLOYED** (D1 did not exercise a connector credential read) |
| `WebhookSecretProvider` | `LocalWebhookSecretProvider` | `SecretManagerWebhookSecretProvider` | **IMPLEMENTED + PROD-COMPOSED + DEPLOYED** (D1 did not exercise the webhook path) |
| `ObservedCrossingStore` | `InMemoryObservedCrossingStore` | `FirestoreObservedCrossingStore` | **IMPLEMENTED + EMULATOR TESTED + LIVE VERIFIED LOCALLY** |
| `OutboxRepository` | `InMemoryOutboxRepository` | `FirestoreOutboxRepository` | **IMPLEMENTED + EMULATOR TESTED + LIVE VERIFIED LOCALLY** |
| `MessagePublisherPort` | `RecordingMessagePublisher` | `GooglePubSubPublisher` | **IMPLEMENTED + EMULATOR TESTED + LIVE VERIFIED AGAINST REAL GCP** |
| Delivery pull consumer | in-process `DeliveryInstructionHandler` | `GooglePubSubPullConsumer` | **IMPLEMENTED + EMULATOR TESTED + LIVE VERIFIED AGAINST REAL GCP** |

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
| Outbox | **IMPLEMENTED + EMULATOR TESTED + LIVE VERIFIED LOCALLY** |
| Persist-before-publish | **EMULATOR VERIFIED + LIVE VERIFIED** |
| Pub/Sub publisher / pull consumer | **IMPLEMENTED + EMULATOR TESTED + LIVE VERIFIED AGAINST REAL GCP** |
| Pub/Sub resources in real GCP | **LIVE / PROVISIONED** |
| TTL policy | **PLANNED / REVIEW LATER** |

The explicit prod composition is deployed and Ready. D1 verified the workload persistence path for one
synthetic transaction; local/test profiles continue to use in-memory defaults.

## Phase 15E secret inventory

Pulumi manages the four Secret Manager containers. Phase 15I-B1 applied resource-level workload
IAM, and Phase 15I-B2 added exactly one enabled synthetic version to each container outside Pulumi.

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

The API prod profile accesses `hmac-quayforge`. The worker prod profile accesses the three
provider credentials. Workstation ADC live verification proved both adapter paths; Cloud Run
workload access is still unverified. Local providers remain the default.

## Phase 15G outbox / Pub/Sub truth

| Item | Status |
| --- | --- |
| Transactional outbox model | **IMPLEMENTED + TESTED** |
| Firestore outbox repository | **EMULATOR TESTED + LIVE VERIFIED LOCALLY** |
| Pub/Sub publisher adapter | **EMULATOR TESTED + LIVE VERIFIED AGAINST REAL GCP** |
| Pub/Sub pull consumer adapter | **EMULATOR TESTED + LIVE VERIFIED AGAINST REAL GCP** |
| Persist-before-publish | **EMULATOR VERIFIED + LIVE VERIFIED** |
| ALLOW publication | **LIVE VERIFIED** |
| ALLOW_WITH_TRANSFORMATION publication | **LIVE VERIFIED** |
| REJECT / QUARANTINE non-publication | **LIVE VERIFIED** |
| Duplicate delivery safety | **EMULATOR VERIFIED + LIVE VERIFIED** |
| Transport semantics | **AT-LEAST-ONCE** (end-to-end exactly-once **NOT CLAIMED**) |
| Worker delivery idempotency | **LIVE VERIFIED** |
| Client recreation persistence | **LIVE VERIFIED** |
| Pub/Sub API | **LIVE / ENABLED IN GCP** |
| Main topic `boundarymesh-deliveries` | **LIVE / PROVISIONED IN GCP** |
| Worker pull subscription `boundarymesh-delivery-worker` | **LIVE / PROVISIONED IN GCP** |
| DLQ topic `boundarymesh-delivery-dlq` | **LIVE / PROVISIONED IN GCP** |
| DLQ inspect subscription `boundarymesh-delivery-dlq-inspect` | **LIVE / PROVISIONED IN GCP** |
| Dead-letter forwarding | **PLANNED / NOT CONFIGURED** (no `deadLetterPolicy`, no IAM) |
| Live publisher / consumer data path | **LIVE VERIFIED LOCALLY** (not Cloud Run workload-live) |
| Live persist → publish → consume → dedupe | **LIVE VERIFIED** |
| Live verification residue | **CLEANED** |
| Cloud Run Worker Pool | **LIVE / READY**; no endpoint; Pub/Sub consumption and worker idempotency were LIVE VERIFIED in 15I-D1 |

Do **not** claim Cloud Run workload messaging is live.

## Phase 15H Cloud KMS truth

| Item | Status |
| --- | --- |
| Cloud KMS signer adapter | **IMPLEMENTED + UNIT TESTED** |
| KMS P-256 signature normalization (DER → IEEE P1363) | **IMPLEMENTED + TESTED** |
| Cloud KMS API Pulumi | **IMPLEMENTED + APPLIED** |
| KMS key ring `boundarymesh-attestation` | **LIVE / PROVISIONED IN GCP** |
| KMS asymmetric signing key `exchange-attestation` | **LIVE / PROVISIONED IN GCP** |
| Algorithm | `EC_SIGN_P256_SHA256` (attestation records `ES256`) |
| Protection level | SOFTWARE |
| Cloud KMS in real GCP | **LIVE / PROVISIONED** |
| Live Cloud KMS signing | **LIVE VERIFIED LOCALLY (15H-C)** |
| Local JCA signer | **IMPLEMENTED + VERIFIED / UNCHANGED** |
| Workload KMS IAM | **API-ONLY + CRYPTO-KEY-SCOPED / LIVE + APPLIED (15I-B1)** |
| Default `mvnw verify` | No ADC, no KMS, no real GCP |

Cloud KMS signing is **LIVE VERIFIED LOCALLY** with synthetic data. The API-only CryptoKey IAM is
live after 15I-B1; the Cloud Run API composition is deployed and Ready, but workload signing is not
yet verified. See
[Phase 15H verification](PHASE_15H_VERIFICATION.md).

## Phase 15I-A workload composition truth

- API: real Firestore repositories, transactional outbox publisher, Cloud KMS signer/verifier,
  and Secret Manager webhook provider.
- Worker: real Pub/Sub continuous pull, Firestore delivery idempotency, Camel delivery, and
  Secret Manager connector credentials. No HTTP server and no KMS permission.
- Exactly two keyless service accounts are live. The ten approved IAM grants use additive member resources.
- `roles/datastore.user` is project-scoped because Firestore IAM cannot express collection-level
  restrictions for these server SDK data operations. Pub/Sub, KMS, and Secret Manager grants are
  scoped to the individual topic, subscription, CryptoKey, or secret.
- No SecretVersion is defined in Pulumi. Phase 15I-B2 created one enabled synthetic version per
  existing integration secret and live-verified `GoogleSecretAccessor`, the webhook provider, and
  the worker provider through ADC. D1 did not exercise a deployed workload secret read. The Entra cloud
  composition remains disabled, so no Entra secret container exists.
- Phase 15I-C1 built and smoke-tested current API/worker images, pushed them to the existing
  Artifact Registry repository, and pinned the verified immutable digests in Pulumi.
- Phase 15I-C2 enabled `run.googleapis.com`, deployed the private API Service and no-endpoint
  Worker Pool, and ended with a 33-unchanged, zero-operation Pulumi preview. The API and Worker
  Pool are Ready. The private `POST /api/v1/transactions` ingress is deployed and one internal cloud transaction was LIVE VERIFIED in Phase 15I-D1. D1 did not exercise a deployed workload secret read or external connector delivery.

## Phase 15I-D0C delivery transport semantics

The worker maps application delivery disposition to transport behavior. Terminal outcomes, including a
durably persisted QUARANTINED custody state, are acknowledged after persistence succeeds. Retryable
transient outcomes remain eligible for Pub/Sub redelivery. Persistence failure does not produce a false
ACK. The D0C worker image is deployed to the Worker Pool; D1 verified one live application transaction and its terminal ACK behavior.
