# Pulumi architecture

Primary IaC: **Pulumi + TypeScript**. Terraform is forbidden for Project C.

## Current state (Phase 16C.4 live verified)

- Project/stack: `boundarymesh/dev`
- Pulumi CLI baseline: `3.262.0`
- Backend: GCS DIY backend `gs://bm-pulumi-state-eu4-21cad20b325f`
- Secrets provider: `gcpkms://projects/boundarymesh-dev/locations/europe-west4/keyRings/boundarymesh-pulumi/cryptoKeys/state-secrets`
- State: **54 managed resources / 55 records including one `pulumi:pulumi:Stack` root**
- Final accepted preview: **54 unchanged / zero operations / exit 0**

The state bucket uses uniform bucket-level access, public-access prevention, versioning, protected
Pulumi ownership, and bounded noncurrent-version retention. The state KMS key is separate from the
attestation-signing key. Compute Engine API remains intentionally **DISABLED**; Pulumi's resulting
regions lookup warning is accepted and non-fatal.

The historical developer-machine `file://~` backend and passphrase-backed checkpoint were migrated
under a controlled source-freeze and recovery procedure. They are not the active backend or secrets
provider. The previously exposed passphrase remains an accepted historical security-hygiene item;
this migration does not erase that history.

The Phase 15 sections below are historical provisioning records. Their resource counts and gate
statements describe those phases, not the current 54-resource stack.

## Phase 15H-B (applied)

Pulumi **manages** Cloud KMS attestation signing resources. Applied in Phase 15H-B.

- `gcp.projects.Service` `kms-api` → `cloudkms.googleapis.com` (`disableOnDestroy: false`) — **LIVE / ENABLED IN GCP**
- `gcp.kms.KeyRing` `attestation-key-ring` → `boundarymesh-attestation` in `europe-west4` — **LIVE / PROVISIONED IN GCP**
- `gcp.kms.CryptoKey` `attestation-signing-key` → `exchange-attestation`, purpose `ASYMMETRIC_SIGN`, algorithm `EC_SIGN_P256_SHA256`, protection `SOFTWARE` — **LIVE / PROVISIONED IN GCP**

Phase 15I-B1 provisioned the keyless API and worker service accounts. Only the API has CryptoKey-scoped signer and public-key-viewer IAM; the worker has no KMS role. No imported key material or scheduled version cycling exists. Version `1` is ENABLED. Cloud KMS signing was **LIVE VERIFIED LOCALLY (15H-C)** and later **WORKLOAD LIVE VERIFIED (15J)**.

`phase15HScope = "KMS_PREVIEW"` is a historical implementation-scope marker introduced in 15H-A, like the earlier Firestore/Pub/Sub scope constants. It is not a measurement of live infrastructure state. The literal and stack output are intentionally unchanged; a source comment clarifies its meaning. Provisioning truth comes from the 15H-B apply checkpoint and read-only GCP metadata. Phase 15H-C runs previews only, never `pulumi up`, including for informational output changes.

## Phase 15G-B (applied)

Pulumi **manages** Pub/Sub delivery topology. Applied in Phase 15G-B.

- `gcp.projects.Service` `pubsub-api` → `pubsub.googleapis.com` (`disableOnDestroy: false`) — **LIVE / ENABLED IN GCP**
- `gcp.pubsub.Topic` `delivery-topic` → `boundarymesh-deliveries` — **LIVE / PROVISIONED IN GCP**
- `gcp.pubsub.Subscription` `delivery-worker-subscription` → pull `boundarymesh-delivery-worker` (ack 60s, retry 10s–600s, no push) — **LIVE / PROVISIONED IN GCP**
- `gcp.pubsub.Topic` `delivery-dlq-topic` → `boundarymesh-delivery-dlq` — **LIVE / PROVISIONED IN GCP**
- `gcp.pubsub.Subscription` `delivery-dlq-inspect-subscription` → `boundarymesh-delivery-dlq-inspect` — **LIVE / PROVISIONED IN GCP**

No `deadLetterPolicy`. No IAM. Dead-letter forwarding is **PLANNED / NOT CONFIGURED**. Live persist → publish → consume was **LIVE VERIFIED** in Phase 15G-C against these resources. Residue **CLEANED**.

Existing Artifact Registry, Secret Manager, and Firestore resources stay unchanged.

## Phase 15F

Pulumi **manages** one named Firestore Native database and the Firestore API.

- `gcp.projects.Service` `firestore-api` → `firestore.googleapis.com` (`disableOnDestroy: false`) — **LIVE / ENABLED IN GCP**
- `gcp.firestore.Database` `boundarymesh-database` → database ID `boundarymesh`, `europe-west4`, `FIRESTORE_NATIVE`, `STANDARD`, App Engine `DISABLED`, concurrency `PESSIMISTIC`, delete protection **ENABLED** — **LIVE / PROVISIONED IN GCP**

No indexes, PITR, backup schedules, or TTL policies. Phase 15F-C live-verified the Java SDK path and cleaned run-scoped documents. Residue is **zero**. Artifact Registry and Secret Manager remain **LIVE / UNCHANGED**. Cloud Run workload persistence is **not** live.

## Phase 15E

Pulumi **manages** Secret Manager containers in addition to Artifact Registry:

- `gcp.projects.Service` `secret-manager-api` → `secretmanager.googleapis.com` (`disableOnDestroy: false`) — **LIVE / ENABLED IN GCP**
- `gcp.secretmanager.Secret` containers `hmac-quayforge`, `covecrm-api`, `lumenway-api`, `rivertide-api` — **LIVE / PROVISIONED IN GCP**, user-managed replication in `europe-west4`; each has one enabled synthetic version created outside Pulumi in 15I-B2

No version resources. No IAM. No secret payloads. Adapter is not workload-live.

## Phase 15D

Pulumi **manages** Artifact Registry:

- `gcp.projects.Service` `artifact-registry-api` → `artifactregistry.googleapis.com` (`disableOnDestroy: false`) — **LIVE / ENABLED IN GCP**
- `gcp.artifactregistry.Repository` `boundarymesh-images` → repo id `boundarymesh`, format `DOCKER`, location `europe-west4` — **LIVE / PROVISIONED IN GCP**

API and worker images are **PUSHED + REMOTE DIGEST VERIFIED** (see `infra/README.md`). Image publication is not a workload deployment.

Phase 15I-C1 added fresh images built from the current Firestore/Pub/Sub/KMS/Secret Manager
production composition. Phase 15I-C2 deployed the private API Service and no-endpoint Worker Pool
on remote-verified immutable digests. The Phase 15D images remain historical; workload functional
verification remains Phase 15I-D.

## Phase 15B

`infra/` is a compilable Pulumi TypeScript project with verified **non-secret** `dev` configuration:

- `gcp:project` = `boundarymesh-dev`
- `gcp:region` = `europe-west4`
- `boundarymesh:environment` = `dev`

At this historical gate Pulumi used the local `file://~` backend. Phase 15B zero-resource `pulumi preview` was **TESTED**, and Phase 15D's first controlled `pulumi up` applied Artifact Registry only. Phase 16C later migrated the active stack to the GCS/KMS model documented above.

## Layout

```text
infra/
  Pulumi.yaml
  Pulumi.dev.yaml
  src/index.ts
  src/shared-state.ts
  src/wif.ts
  src/ci-preview.ts
  test/
```

Current stack: `dev`. A production stack remains future work and is not implied by the live dev environment.

## State ownership

The active GCS DIY backend is `gs://bm-pulumi-state-eu4-21cad20b325f`. Pulumi owns the protected
bucket, its dedicated KMS key ring/key, and all 54 managed resources in one `dev` stack. Firebase
Hosting releases remain intentionally outside Pulumi ownership. Secret Manager payloads also remain
outside Pulumi state; Pulumi manages the containers and IAM boundaries only.

The bucket is the shared state and lock authority for local and hosted previews. The CI preview
identity is limited to listing/getting its objects and decrypting with the dedicated state key. It
cannot write/delete state or encrypt with KMS.

## Secrets in Pulumi

Stack config secrets for project numbers only if needed. Connector and HMAC **values** go to
Secret Manager through a controlled interactive administrator procedure, never `pulumi config`,
source, chat, logs, or placeholder SecretVersions.

## Preview / update / rollback

- **LIVE:** `.github/workflows/infra-preview.yml` runs only by `workflow_dispatch` from protected `main` in environment `boundarymesh-ci-preview`.
- **LIVE:** `boundarymesh-ci-preview` authenticates keylessly and can read/decrypt only the shared stack state. Run `36337930890` returned 54 unchanged / zero operations.
- **NOT IMPLEMENTED:** pull-request automation, artifact publication, and infrastructure/application deployment.
- **PLANNED:** Phase 16D immutable publication, Phase 16E controlled CD with explicit approval, then Phase 16F live verification and freeze.
- Rollback remains a reviewed operation using versioned state and known-good immutable workload digests. CI preview has no rollback or mutation permission.

## Resources Pulumi manages

Two Cloud Run Services, one Cloud Run Worker Pool, three keyless workload SAs, Pub/Sub
topic/subscription/DLQ, Firestore Native database, attestation KMS key ring/key, Secret Manager
**ids**, Artifact Registry, shared-state bucket and KMS resources, WIF pool/provider, two keyless CI
identities, and scoped IAM members/roles. Firebase Hosting releases are live but outside Pulumi.

No GKE. No VPC. No SQL.
