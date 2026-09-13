# BoundaryMesh infrastructure

Pulumi + TypeScript workspace for Google Cloud resources.

## Phase 15D purpose

Define the **minimum** Artifact Registry needed to store API and worker images.

The first controlled apply has provisioned Artifact Registry only. Do not add other GCP resources without a new explicit apply gate.

Authentication uses workstation **Application Default Credentials**. Do not set `GOOGLE_APPLICATION_CREDENTIALS`. Do not commit ADC files, tokens, or service-account JSON.

## Current state

| Item | Status |
| --- | --- |
| Pulumi TypeScript repository foundation | IMPLEMENTED + LOCALLY VALIDATED |
| GCP project | CREATED + BILLING LINKED (`boundarymesh-dev`) |
| Artifact Registry Pulumi definition | IMPLEMENTED + APPLIED |
| Artifact Registry API | LIVE / ENABLED IN GCP |
| Artifact Registry repository | LIVE / PROVISIONED IN GCP |
| API image | PUSHED + REMOTE DIGEST VERIFIED |
| Worker image | PUSHED + REMOTE DIGEST VERIFIED |
| Artifact Registry round-trip | LIVE VERIFIED |
| Cloud Run deployment | PLANNED |

Target region: **`europe-west4`**.  
Stack: **`dev`**.  
GCP project: **`boundarymesh-dev`**.  
Repository ID: **`boundarymesh`** (DOCKER). Published image names: `boundarymesh-api`, `boundarymesh-worker` (tag `phase15d`).

## Published image digests (2026-09-13)

Immutable Artifact Registry digests. Tag `phase15d` is a pointer; the digest is the artifact.

```text
europe-west4-docker.pkg.dev/boundarymesh-dev/boundarymesh/boundarymesh-api:phase15d
sha256:3a933e2bc260be6511d835353dbc3a3cde10e3e836f1b318122e7e94cee4d705

europe-west4-docker.pkg.dev/boundarymesh-dev/boundarymesh/boundarymesh-worker:phase15d
sha256:62570f5c928394b74b60071ed7d34cb534522ce7f4f8c39816b012d855fa3768
```

These images are stored, not deployed. Cloud Run remains **PLANNED**.

Local backend remains `file://~`. GCS state is **PLANNED**.

## Future planned services (not provisioned)

- Secret Manager
- Firestore
- Pub/Sub
- Cloud KMS
- Cloud Run
- Firebase Hosting, pending final validation
- WIF

## Local commands

```powershell
cd infra
npm install
npm run typecheck
npm run build
npm test
$env:PULUMI_CONFIG_PASSPHRASE = '<LOCAL_PULUMI_PASSPHRASE>'
pulumi preview
```

Do not run `pulumi destroy` or add resources until explicitly approved. Image publication is not a Cloud Run deploy.
