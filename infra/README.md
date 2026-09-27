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
| Production composition | LIVE VERIFIED |
| Cloud Run API Service | LIVE / PRIVATE / READY |
| Cloud Run Worker Pool | LIVE / READY / NO PUBLIC URL |
| Cloud Run Web BFF | LIVE / READY / PUBLIC TRANSPORT + MANDATORY APPLICATION AUTH |
| Secret Manager Pulumi definition | IMPLEMENTED + APPLIED |
| Secret Manager API | LIVE / ENABLED IN GCP |
| Secret containers | LIVE / PROVISIONED IN GCP |
| Secret versions | FOUR TOTAL / EXACTLY ONE ENABLED SYNTHETIC VERSION PER CONTAINER |
| Workload identities | THREE KEYLESS SERVICE ACCOUNTS / LIVE / PROVISIONED / ZERO USER-MANAGED KEYS |
| Workload IAM | LIVE / LEAST PRIVILEGE / BFF CORE INVOKER IS SERVICE-SCOPED |
| Secret Manager workload access | LIVE / RESOURCE-SCOPED AUTHORIZATION ONLY |
| Firestore persistence adapters | IMPLEMENTED + EMULATOR TESTED + WORKLOAD LIVE VERIFIED |
| Firestore Pulumi definition | IMPLEMENTED + APPLIED |
| Firestore API in GCP | LIVE / ENABLED IN GCP |
| Firestore database in GCP | LIVE / PROVISIONED IN GCP |
| Live Java SDK data path | WORKLOAD LIVE VERIFIED / DISPOSABLE RESIDUE CLEANED / EVIDENCE PRESERVED |
| Transactional outbox + Pub/Sub adapters | IMPLEMENTED + EMULATOR TESTED + WORKLOAD LIVE VERIFIED |
| Pub/Sub Pulumi | IMPLEMENTED + APPLIED |
| Pub/Sub API in GCP | LIVE / ENABLED IN GCP |
| Pub/Sub topics + pull subscriptions | LIVE / PROVISIONED IN GCP |
| Dead-letter forwarding | PLANNED / NOT CONFIGURED |
| Live Pub/Sub data path | WORKLOAD LIVE VERIFIED / ONE PUBLISH + ONE ACK / NO OBSERVED REDELIVERY |
| Cloud KMS signer adapter | IMPLEMENTED + UNIT TESTED |
| Cloud KMS Pulumi | IMPLEMENTED + APPLIED |
| Cloud KMS API / key ring / crypto key in GCP | LIVE / PROVISIONED |
| Live Cloud KMS signing | WORKLOAD LIVE VERIFIED / ES256 VERSION 1 VALID |
| Workload KMS IAM | API-ONLY, CRYPTO-KEY-SCOPED / LIVE / APPLIED |
| Phase 15I-A preview | 15 CREATE / 18 UNCHANGED / 0 UPDATE / 0 REPLACE / 0 DELETE |
| Phase 15I-B1 targeted apply | 12 CREATED / NO OTHER OPERATIONS |
| Phase 15I-B1 final preview | 3 CREATE / 30 UNCHANGED / 0 UPDATE / 0 REPLACE / 0 DELETE |
| Phase 15I-B2 Secret Manager adapter proof | LIVE VERIFIED LOCALLY / PAYLOADS NOT PULUMI-MANAGED |
| Phase 15I-C2 final preview | 33 UNCHANGED / 0 CREATE / 0 UPDATE / 0 REPLACE / 0 DELETE |
| Phase 15J final preview | 39 UNCHANGED / 0 CREATE / 0 UPDATE / 0 REPLACE / 0 DELETE |
| Firebase Hosting | LIVE / 3 RELEASES / OUTSIDE PULUMI OWNERSHIP |
| Pulumi shared backend | LIVE / GCS DIY / VERSIONED / PUBLIC ACCESS PREVENTED |
| Pulumi secrets provider | LIVE / DEDICATED GCP KMS KEY |
| GitHub WIF provider | LIVE / ACTIVE / EXACTLY TWO MANUAL MAIN-ONLY CONTEXTS |
| WIF verification identity | LIVE VERIFIED / KEYLESS / NO PROJECT OR DEPLOYMENT ROLES |
| CI preview identity | LIVE VERIFIED / KEYLESS / STATE READ + STATE-KEY DECRYPT ONLY |
| Phase 16C.4 final preview | 54 UNCHANGED / 0 CREATE / 0 UPDATE / 0 REPLACE / 0 DELETE |
| Phase 16D.1 publication architecture | IMPLEMENTED IN SOURCE / TESTED LOCALLY / PREVIEWED (5 CREATE / 2 UPDATE / 52 UNCHANGED / 0 REPLACE / 0 DELETE); NOT LIVE OR PUBLISHED |
| Current Pulumi state | 54 MANAGED RESOURCES / 55 RECORDS INCLUDING STACK ROOT / ZERO DRIFT |

Target region: **`europe-west4`**.  
Stack: **`dev`**.  
GCP project: **`boundarymesh-dev`**.  
Repository ID: **`boundarymesh`** (DOCKER). API, Worker, and Web BFF run immutable digest references. Phase 15D and intermediate Phase 15J tags remain historical and untouched.

## Current deployed image digests (Phase 15J freeze)

Current immutable Artifact Registry deployment references:

```text
europe-west4-docker.pkg.dev/boundarymesh-dev/boundarymesh/boundarymesh-api@sha256:df4ca576227b1311194af1b3c821962a228bc7222d144eb4b4fbfb41590e072d

europe-west4-docker.pkg.dev/boundarymesh-dev/boundarymesh/boundarymesh-worker@sha256:a031fd689c80faef122b2eb7a35c35c6cb04035f333c493e5d7decea1d08b76a

europe-west4-docker.pkg.dev/boundarymesh-dev/boundarymesh/boundarymesh-web-edge@sha256:90570617750bd39613482fd75d8434f1a47299ae8d35908ae0c81717374ee30a
```

The API, Worker Pool, and Web BFF are deployed on these immutable references. Phase 15J live
verified the authenticated hosted browser path and exactly one governed transaction through the
internal cloud pipeline. External connector delivery remains intentionally disabled.

Current backend: `gs://bm-pulumi-state-eu4-21cad20b325f`. Stack secrets use
`gcpkms://projects/boundarymesh-dev/locations/europe-west4/keyRings/boundarymesh-pulumi/cryptoKeys/state-secrets`.
The historical `file://~` backend and passphrase-backed state were superseded by the controlled
Phase 16C migration. The independently protected pre-migration backup remains rollback evidence;
it is not the active backend.

## Gated next steps (not provisioned)

- Pub/Sub dead-letter forwarding
- Phase 16D immutable artifact publication
- Phase 16E controlled CD
- Phase 16F live CI/CD verification and freeze
- Application-security automation after the CI/CD foundation
- Final credential-hygiene rotation and audit in its later gated phase

## Local commands

The active stack uses the GCP KMS secrets provider and does not need the historical passphrase.
The previously exposed passphrase remains accepted-risk historical credential material until the
final security-hygiene audit confirms retirement and recovery-boundary handling. Never commit,
print, or place either credential class in shell history.

```powershell
cd infra
npm ci
npm run typecheck
npm run build
npm test
pulumi login gs://bm-pulumi-state-eu4-21cad20b325f
pulumi preview
```

Do not mutate secrets, IAM, Hosting, or Cloud Run without a later explicit gate.
