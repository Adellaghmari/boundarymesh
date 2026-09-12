# BoundaryMesh infrastructure

Pulumi + TypeScript workspace for future Google Cloud resources.

## Phase 15A purpose

Phase 15A is **repository foundation only**. It establishes a compilable Pulumi TypeScript project so later phases can add resources incrementally.

This directory currently defines **zero cloud resources**.

`npm run typecheck` and `npm test` do not require GCP credentials, Pulumi login, or network access to Google.

**Do not run** `pulumi preview`, `pulumi up`, `pulumi destroy`, or `gcloud` for Phase 15A. Those commands were not used to validate this phase.

## Current state

| Item | Status |
| --- | --- |
| Pulumi TypeScript repository foundation | IMPLEMENTED + LOCALLY VALIDATED |
| GCP provider / authentication | PLANNED (Phase 15B) |
| Cloud resources in `src/` | none |

Target region: **`europe-west4`**.  
Documented stack: **`dev`**.  
GCP project identifier: placeholder `REPLACE_IN_PHASE_15B` until Phase 15B.

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

Never commit secrets, tokens, Microsoft credentials, or service-account material. Stack YAML in this phase is non-secret placeholders only. Connector and Entra values stay out of `infra/`.

## Local commands (Phase 15A)

```powershell
cd infra
npm install
npm run typecheck
npm test
```
