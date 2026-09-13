# Pulumi architecture

Primary IaC: **Pulumi + TypeScript**. Terraform is forbidden for Project C.

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
- `gcp.secretmanager.Secret` containers `hmac-quayforge`, `covecrm-api`, `lumenway-api`, `rivertide-api` — **LIVE / PROVISIONED IN GCP**, empty, user-managed replication in `europe-west4`

No version resources. No IAM. No secret payloads. Adapter is not workload-live.

## Phase 15D

Pulumi **manages** Artifact Registry:

- `gcp.projects.Service` `artifact-registry-api` → `artifactregistry.googleapis.com` (`disableOnDestroy: false`) — **LIVE / ENABLED IN GCP**
- `gcp.artifactregistry.Repository` `boundarymesh-images` → repo id `boundarymesh`, format `DOCKER`, location `europe-west4` — **LIVE / PROVISIONED IN GCP**

API and worker images are **PUSHED + REMOTE DIGEST VERIFIED** (see `infra/README.md`). Image publication is not a workload deployment.

## Phase 15B

`infra/` is a compilable Pulumi TypeScript project with verified **non-secret** `dev` configuration:

- `gcp:project` = `boundarymesh-dev`
- `gcp:region` = `europe-west4`
- `boundarymesh:environment` = `dev`

Local Pulumi backend (`file://~`). Phase 15B zero-resource `pulumi preview` **TESTED**. Phase 15D first controlled `pulumi up` **APPLIED** Artifact Registry only.

GCS DIY state remains **PLANNED**.

## Layout (later provision)

```text
infra/
  Pulumi.yaml
  Pulumi.dev.yaml
  Pulumi.prod.yaml
  src/index.ts          # or split: run.ts, iam.ts, data.ts, kms.ts
```

Stacks: `dev` (default), `prod` (later). Same program, config differs (project id, min instances=0).

## State

**GCS DIY backend** `gs://boundarymesh-pulumi-state` (name will include a random suffix).

- Object versioning on
- Uniform bucket-level access
- Pulumi file lock in-bucket
- State encryption: Pulumi secrets provider **GCP KMS** (`passphrase` only for local experiments)

Chicken-and-egg: the state bucket is created **once** by a documented bootstrap (gcloud) **or** a tiny bootstrap stack stored separately. After that, Pulumi owns Cloud Run, IAM, Firestore, Pub/Sub, KMS, Secret Manager **resources** (secret **values** still not in state if using SM resource with ignored value), Artifact Registry, Firebase Hosting, WIF.

Do not mix console-created Cloud Run with Pulumi (duplicate ownership). Exception: the bootstrap bucket.

## Secrets in Pulumi

Stack config secrets for project numbers only if needed. Connector HMAC **values** go to Secret Manager via a **one-time** admin API after deploy, not `pulumi config`. If Pulumi creates the secret **resource**, the first version can be a placeholder rotated immediately.

## Preview / update / rollback

- `pulumi preview` on every PR (WIF deploy SA or a plan-only SA)
- `pulumi up` gated (`workflow_dispatch` + environment + explicit flag), same spirit as Project B’s apply gate but **Google WIF**, not a copy of AWS OIDC docs
- Rollback: `pulumi history` + `pulumi up` to previous known good; Cloud Run traffic to prior revision

## Resources Pulumi manages

Two Cloud Run services, two SAs, Pub/Sub topic+sub+DLQ, Firestore database Native, KMS key ring+key, Secret Manager **ids**, Artifact Registry, IAM bindings, WIF pool/provider, Firebase Hosting (or documented split), budget alert.

No GKE. No VPC. No SQL.
