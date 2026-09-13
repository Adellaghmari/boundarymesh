# BoundaryMesh infrastructure

Pulumi + TypeScript workspace for Google Cloud resources.

## Phase 15F-B purpose

The named Firestore Native database and Firestore API are **LIVE / PROVISIONED**. Phase 15F-C live-verified the Java SDK path and cleaned run-scoped documents. Phase 15G-B **applied** Pub/Sub API + delivery topic + pull subscription + DLQ topic/inspect subscription. Dead-letter forwarding is **not** configured. Do not add indexes, versions, IAM, `deadLetterPolicy`, KMS, or Cloud Run here.

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
| Secret Manager Pulumi definition | IMPLEMENTED + APPLIED |
| Secret Manager API | LIVE / ENABLED IN GCP |
| Secret containers | LIVE / PROVISIONED IN GCP / EMPTY |
| Secret versions | PLANNED |
| Workload IAM | PLANNED |
| Firestore persistence adapters | IMPLEMENTED + EMULATOR TESTED + LIVE VERIFIED LOCALLY |
| Firestore Pulumi definition | IMPLEMENTED + APPLIED |
| Firestore API in GCP | LIVE / ENABLED IN GCP |
| Firestore database in GCP | LIVE / PROVISIONED IN GCP |
| Live Java SDK data path | LIVE VERIFIED LOCALLY / RESIDUE CLEANED |
| Transactional outbox + Pub/Sub adapters | IMPLEMENTED + EMULATOR TESTED + LIVE VERIFIED LOCALLY |
| Pub/Sub Pulumi | IMPLEMENTED + APPLIED |
| Pub/Sub API in GCP | LIVE / ENABLED IN GCP |
| Pub/Sub topics + pull subscriptions | LIVE / PROVISIONED IN GCP |
| Dead-letter forwarding | PLANNED / NOT CONFIGURED |
| Live Pub/Sub data path | LIVE VERIFIED LOCALLY / RESIDUE CLEANED |

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

- Secret versions and Secret Manager IAM
- Pub/Sub dead-letter forwarding and live messaging verification
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

Do not run `pulumi destroy` or add versions/IAM until explicitly approved.
