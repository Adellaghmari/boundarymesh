# Pulumi architecture

Primary IaC: **Pulumi + TypeScript**. Terraform is forbidden for Project C.

## Phase 15E (current)

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
