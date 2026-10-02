# BoundaryMesh infrastructure

Pulumi + TypeScript workspace for Google Cloud resources in `boundarymesh-dev` / `europe-west4`.

Authentication for local preview uses workstation **Application Default Credentials**. Do not set
`GOOGLE_APPLICATION_CREDENTIALS`. Do not commit ADC files, tokens, or service-account JSON.

Current reviewer truth: [docs/PULUMI.md](../docs/PULUMI.md), [docs/CI_CD.md](../docs/CI_CD.md),
[PROJECT_STATUS.md](../PROJECT_STATUS.md).

## Current state

| Item | Status |
| --- | --- |
| Stack | `dev` |
| Pulumi CLI | `3.262.0` |
| Backend | `gs://bm-pulumi-state-eu4-21cad20b325f` |
| Secrets provider | GCP KMS `boundarymesh-pulumi/state-secrets` |
| Managed resources | **83 / zero drift** |
| Canonical preview | `pulumi preview --stack dev --diff --non-interactive --suppress-permalink` → 83 unchanged / 0 operations / exit 0 |
| Compute Engine API | **DISABLED** (benign Pulumi regions warning) |
| WIF | ACTIVE / **7 exact** main-only contexts |
| Cloud Run API / Web / Worker | LIVE; traffic and images owned by CD, fenced from Pulumi |
| Firebase Hosting | LIVE / outside Pulumi |
| Dead-letter forwarding | NOT CONFIGURED |
| Phase 16D release | PUBLISHED then **promoted** by CD (not by Pulumi) |

Pulumi `ignoreChanges` on image, revision, traffic, and Worker `instanceSplits`. Live promoted
revisions therefore do not appear as Pulumi drift.

## Frozen published OCI parents (CD input)

Source SHA `a876a7bfa5fa86f88e8387efb557773281daa6e6`. Runtime may resolve linux/amd64 children.

```text
boundarymesh-api@sha256:bcfd9b01a3189228f583fa3f5d1ed8797f73d214af85bea4d84caeedfa0db8d7
boundarymesh-worker@sha256:7b1841487a2d1b3751e8bc63ee5323965e76ebeae9ae581c95b672f50154bb2d
boundarymesh-web-edge@sha256:85f98f90450b7807f55649f58807f952010a6278793b808ae1f4df78a22c0b04
```

See [docs/PHASE_16D_FINAL.md](../docs/PHASE_16D_FINAL.md) for the publication freeze. That document
is historical for “not deployed”; current Hosting/API/Worker run the promoted 16D release.

## Historical image tags

Phase 15D / 15I / 15J digest tags remain in Artifact Registry as historical images. They are not
the current promoted identities.

## Optional / not provisioned by this workspace

Listed in [docs/ROADMAP.md](../docs/ROADMAP.md). Not CV blockers: DLQ forwarding, AppSec CI,
tracing/alerts, historical cleanup.

## Local commands

The active stack uses the GCP KMS secrets provider. Never commit or print the historical
passphrase or uncommitted Firebase signing material.

```powershell
cd infra
npm ci
npm run typecheck
npm run build
npm test
pulumi login gs://bm-pulumi-state-eu4-21cad20b325f
pulumi preview --stack dev --diff --non-interactive --suppress-permalink
```

Do not `pulumi up`, refresh, or mutate IAM/WIF/Hosting/Cloud Run unless a later explicit gate
allows it.
