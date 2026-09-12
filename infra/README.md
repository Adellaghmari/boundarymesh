# BoundaryMesh infrastructure

Pulumi + TypeScript workspace for future Google Cloud resources.

## Phase 15B purpose

Phase 15B validates **non-secret GCP project/provider configuration** and local Pulumi tooling. It does not provision application infrastructure.

This directory still defines **zero cloud resources**.

`pulumi preview` is allowed. **`pulumi up` is forbidden.**

Authentication uses workstation **Application Default Credentials**. Do not set `GOOGLE_APPLICATION_CREDENTIALS`. Do not commit ADC files, tokens, or service-account JSON.

## Current state

| Item | Status |
| --- | --- |
| Pulumi TypeScript repository foundation | IMPLEMENTED + LOCALLY VALIDATED |
| GCP project | CREATED + BILLING LINKED (`boundarymesh-dev`) |
| gcloud local authentication | CONFIGURED + VERIFIED |
| Application Default Credentials | CONFIGURED + VERIFIED LOCALLY |
| Pulumi local backend | CONFIGURED + VERIFIED LOCALLY |
| Pulumi `dev` stack configuration | IMPLEMENTED + LOCALLY VALIDATED |
| GCP project configuration | IMPLEMENTED + LOCALLY VALIDATED (`gcp:project`) |
| GCP region configuration | IMPLEMENTED + LOCALLY VALIDATED (`europe-west4`) |
| Zero-resource Pulumi preview | TESTED |
| Cloud resources in `src/` | none |

Target region: **`europe-west4`**.  
Stack: **`dev`**.  
GCP project: **`boundarymesh-dev`**.

GCS Pulumi state remains **PLANNED**. Phase 15B uses a **local** Pulumi backend only.

## Future planned services (not implemented)

- Artifact Registry
- Secret Manager
- Firestore
- Pub/Sub
- Cloud KMS
- Cloud Run
- Firebase Hosting, pending final validation

Workload Identity Federation for GitHub Actions is **PLANNED**. Do not add long-lived GCP service-account JSON keys. Ever.

## Secrets

Never commit secrets, tokens, Microsoft credentials, ADC JSON, or service-account material. Stack YAML contains only non-secret project/region/environment values.

## Local commands (Phase 15B)

```powershell
cd infra
npm install
npm run typecheck
npm run build
npm test
$env:PULUMI_CONFIG_PASSPHRASE = '<LOCAL_PULUMI_PASSPHRASE>'
pulumi preview
```

`<LOCAL_PULUMI_PASSPHRASE>` is a **local Pulumi stack unlock string** for the empty `dev` stack. It is not a GCP, Microsoft, or application secret. Do not reuse it as a cloud credential.

A first preview may list `pulumi:pulumi:Stack` as create. That is the local stack envelope. It is not a GCP resource. Do not run `pulumi up` to materialize it in Phase 15B.

Do not run `pulumi up` or `pulumi destroy`.
