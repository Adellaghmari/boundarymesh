# Pulumi architecture

Primary IaC: **Pulumi + TypeScript**. Terraform is forbidden for Project C.

## Current state

- Project/stack: `boundarymesh/dev`
- Pulumi CLI baseline: `3.262.0`
- Backend: GCS DIY `gs://bm-pulumi-state-eu4-21cad20b325f`
- Secrets provider: `gcpkms://projects/boundarymesh-dev/locations/europe-west4/keyRings/boundarymesh-pulumi/cryptoKeys/state-secrets`
- Managed resources: **83**
- Canonical preview: **83 unchanged / 0 operations / exit 0** (no refresh)
- Compute Engine API: intentionally **DISABLED**. The regions-lookup warning is a known benign limitation. It does not block deployability or authorize enabling Compute.

Pulumi owns infrastructure identity, IAM, APIs, data services, and the *existence* of Cloud Run
services and the Worker Pool. **Pulumi does not own live traffic.** Controlled CD updates these
fenced fields, which Pulumi `ignoreChanges`:

| Resource | Fenced paths |
| --- | --- |
| API / Web Cloud Run services | `template.containers[0].image`, `template.revision`, `traffics` |
| Worker Pool | `template.containers[0].image`, `template.revision`, `instanceSplits` |

That is why a promoted revision such as `boundarymesh-api-c-36941014905` can be live while a
canonical Pulumi preview still reports zero drift. Firebase Hosting releases are also outside
Pulumi. Secret Manager **payloads** are outside Pulumi; containers and IAM are inside.

The historical `file://~` backend and passphrase-backed checkpoint were migrated. They are not
the active backend. The previously exposed passphrase remains a historical hygiene follow-up;
current stack secrets use KMS.

Sections below that name Phase 15 resource counts are **historical provisioning records**.

## Preview / update / rollback

- **LIVE:** `.github/workflows/infra-preview.yml` is manual, main-only, environment
  `boundarymesh-ci-preview`, preview-only.
- **LIVE:** controlled CD in `.github/workflows/deploy-release.yml` mutates the fenced Cloud Run
  and Hosting fields, not Pulumi state.
- **NOT IMPLEMENTED:** pull-request Pulumi automation; saved-plan apply in GitHub Actions.
- Rollback of application traffic is a CD operation, not `pulumi destroy`. See [CI_CD.md](CI_CD.md).

## Resources Pulumi manages

Two Cloud Run Services, one Cloud Run Worker Pool, three keyless workload SAs, deploy and hosting
deploy SAs, Pub/Sub topic/subscription/DLQ inspect, Firestore Native database, attestation KMS,
state KMS, Secret Manager ids, Artifact Registry (images, releases, deployment evidence), shared
state bucket, WIF pool/provider, CI/publication identities, custom roles, and scoped IAM members.

No GKE. No VPC. No SQL. Compute API stays disabled.

## Layout

```text
infra/
  Pulumi.yaml
  Pulumi.dev.yaml
  src/index.ts
  src/shared-state.ts
  src/wif.ts
  src/ci-preview.ts
  src/deployment-foundation.ts
  test/
```

A separate commercial production stack is not implied by the live `dev` environment.

## Historical provisioning notes

Phase 15D–15J and 16C–16D applied Artifact Registry, Secret Manager containers, Firestore, Pub/Sub,
KMS, workloads, WIF, and publication resources in gated steps. Those gates recorded then-current
counts (for example 39, 54, or 59 unchanged). Current count is **83**. Dead-letter forwarding was
not configured then and is still not configured.

- KMS attestation key: `exchange-attestation`, `EC_SIGN_P256_SHA256`, SOFTWARE, version 1 ENABLED
- Firestore database `boundarymesh`, delete protection enabled
- Pub/Sub: `boundarymesh-deliveries` + `boundarymesh-delivery-dlq`; no `deadLetterPolicy`
