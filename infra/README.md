# BoundaryMesh infrastructure

Pulumi + TypeScript workspace for Google Cloud resources.

## Phase 15I-A purpose

Phase 15I-A defined the production composition, two keyless workload identities, least-privilege
IAM, an IAM-private Cloud Run API Service, and a no-endpoint Cloud Run Worker Pool. Phase 15I-B1
applied only the two identities and ten approved IAM members. The post-apply preview proposes only
the three Cloud Run resources, with 30 resources unchanged and zero updates/replacements/deletes.

Authentication uses workstation **Application Default Credentials**. Do not set `GOOGLE_APPLICATION_CREDENTIALS`. Do not commit ADC files, tokens, or service-account JSON.

## Current state

| Item | Status |
| --- | --- |
| Pulumi TypeScript repository foundation | IMPLEMENTED + LOCALLY VALIDATED |
| GCP project | CREATED + BILLING LINKED (`boundarymesh-dev`) |
| Artifact Registry Pulumi definition | IMPLEMENTED + APPLIED |
| Artifact Registry API | LIVE / ENABLED IN GCP |
| Artifact Registry repository | LIVE / PROVISIONED IN GCP |
| API image | CORRECTED 15I-C2 IMAGE PUSHED + DIGEST VERIFIED + DEPLOYED |
| Worker image | PHASE 15I-C1 BUILT + SMOKE-TESTED + PUSHED + DIGEST VERIFIED + DEPLOYED |
| Artifact Registry round-trip | LIVE VERIFIED |
| Production composition | IMPLEMENTED + LOCALLY TESTED / NOT LIVE |
| Cloud Run API Service | LIVE / PRIVATE / READY |
| Cloud Run Worker Pool | LIVE / READY / NO PUBLIC URL |
| Secret Manager Pulumi definition | IMPLEMENTED + APPLIED |
| Secret Manager API | LIVE / ENABLED IN GCP |
| Secret containers | LIVE / PROVISIONED IN GCP |
| Secret versions | FOUR TOTAL / EXACTLY ONE ENABLED SYNTHETIC VERSION PER CONTAINER |
| Workload identities | TWO KEYLESS SERVICE ACCOUNTS / LIVE / PROVISIONED |
| Workload IAM | LIVE / TEN ADDITIVE LEAST-PRIVILEGE MEMBERS APPLIED |
| Secret Manager workload access | LIVE / RESOURCE-SCOPED AUTHORIZATION ONLY |
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
| Cloud KMS signer adapter | IMPLEMENTED + UNIT TESTED |
| Cloud KMS Pulumi | IMPLEMENTED + APPLIED |
| Cloud KMS API / key ring / crypto key in GCP | LIVE / PROVISIONED |
| Live Cloud KMS signing | LIVE VERIFIED LOCALLY (15H-C) |
| Workload KMS IAM | API-ONLY, CRYPTO-KEY-SCOPED / LIVE / APPLIED |
| Phase 15I-A preview | 15 CREATE / 18 UNCHANGED / 0 UPDATE / 0 REPLACE / 0 DELETE |
| Phase 15I-B1 targeted apply | 12 CREATED / NO OTHER OPERATIONS |
| Phase 15I-B1 final preview | 3 CREATE / 30 UNCHANGED / 0 UPDATE / 0 REPLACE / 0 DELETE |
| Phase 15I-B2 Secret Manager adapter proof | LIVE VERIFIED LOCALLY / PAYLOADS NOT PULUMI-MANAGED |
| Phase 15I-C2 final preview | 33 UNCHANGED / 0 CREATE / 0 UPDATE / 0 REPLACE / 0 DELETE |

Target region: **`europe-west4`**.  
Stack: **`dev`**.  
GCP project: **`boundarymesh-dev`**.  
Repository ID: **`boundarymesh`** (DOCKER). The API runs the corrected `phase15i-c2-fix-20260915-2345` image by immutable digest; the Worker Pool runs the verified Phase 15I-C1 digest. Phase 15D tags remain historical and untouched.

## Published image digests (2026-09-16)

Current immutable Artifact Registry deployment candidates:

```text
europe-west4-docker.pkg.dev/boundarymesh-dev/boundarymesh/boundarymesh-api@sha256:445cb21117d659fe4430cfdfb1eef956555bba5dc570a15ed43478251feb3a97

europe-west4-docker.pkg.dev/boundarymesh-dev/boundarymesh/boundarymesh-worker@sha256:5d9e0a3a442d3e3dbba8b424aa2cea79c6c97010a486cdd66eb3fd204f7071a6
sha256:62570f5c928394b74b60071ed7d34cb534522ce7f4f8c39816b012d855fa3768
```

The API and Worker Pool are deployed on these immutable references. This documents deployment
readiness only; Phase 15I-D remains required for functional workload verification.

Local backend remains `file://~`. GCS state is **PLANNED**.

## Gated next steps (not provisioned)

- Pub/Sub dead-letter forwarding
- Controlled workload end-to-end verification in Phase 15I-D
- Firebase Hosting, pending final validation
- WIF

## Local commands

The Pulumi passphrase is local secret material and must never be committed. Enter it only at
Pulumi's interactive prompt, or provide it through an approved local secret mechanism that does
not write it into this repository or shell history. Repository documentation must contain
placeholders only.

```powershell
cd infra
npm install
npm run typecheck
npm run build
npm test
pulumi preview
```

Do not add another secret version or deploy Cloud Run before a later explicit gate.
