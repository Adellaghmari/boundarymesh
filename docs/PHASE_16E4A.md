# Phase 16E.4a — controlled deployment workflow source

Status: **IMPLEMENTED / TESTED / OPEN PR REQUIRED / NOT MERGED / NOT EXECUTED.**

The dormant `.github/workflows/deploy-release.yml` consumes only the immutable Phase 16D release
from source `a876a7bfa5fa86f88e8387efb557773281daa6e6`, publication run `36492495801`, and release-manifest
SHA-256 `ecdbaf88710ff73fd694c5c4e13c0a8ce300fdacf2a3761064b7511bd627300d`. The workflow has no
application build or republish path. Every operation downloads the published manifest, checks its
exact byte hash, validates its schema and four-subject content, and compares it with the reviewed
release lock.

## Human control and authority split

The workflow is `workflow_dispatch` only on `refs/heads/main`. One dispatch selects exactly one of
`stage`, `promote`, or `rollback`. Promotion requires an explicit stage run ID. Rollback requires
explicit stage and promotion run IDs. The workflow never dispatches another operation and never
automatically promotes or rolls back.

Runtime jobs use environment `boundarymesh-deploy` and service account
`boundarymesh-deploy@boundarymesh-dev.iam.gserviceaccount.com`. Hosting jobs use environment
`boundarymesh-hosting-deploy` and service account
`boundarymesh-hosting-deploy@boundarymesh-dev.iam.gserviceaccount.com`. Each job obtains its own WIF
credentials; credentials never cross job or authority boundaries. Only non-secret, canonical JSON
operation results cross jobs.

## Stage, promote, and rollback

Runtime stage reads live Cloud Run v2 resources and IAM, captures ETags and production allocation,
then uses `allowMissing=false` and exact update masks to create digest-pinned candidates. API and Web
keep their existing production revisions at 100 percent while tagged candidates remain at zero.
The Worker Pool keeps its current instance split at 100 percent while its candidate remains at zero.
Service accounts, environment, resources, scaling, concurrency, timeouts, probes, ingress,
security-relevant configuration, and IAM are normalized and compared before and after. Candidate
health checks are non-destructive.

Frontend stage verifies the published archive and sidecar, rejects absolute/traversal/link entries,
extracts only `frontend-dist/`, and materializes the exact release-source `firebase.json` after its
SHA-256 check. The fixed site `boundarymesh-dev` is deployed only to deterministic preview channel
`bm-<run-id>`. Root, deep-link, asset-cache, auth-rewrite, and leakage checks run against the preview;
the live release must remain unchanged.

Promotion first rejects stale API, Web, Worker, or Hosting baselines recorded at stage. It promotes
API, Web, and Worker allocation in that order, then releases the exact tested Hosting version.
Rollback is a separate dispatch and restores exact retained identities in Frontend, Worker, Web,
and API order. Partial failure stops without hidden compensating mutation. A promotion failure
records the safely observed runtime/Hosting state as immutable `PROMOTION_FAILED` evidence; that
explicit run reference remains eligible for a later human rollback after stale-state validation.

## Evidence and excess-permission containment

`contracts/deployment-evidence/v1.schema.json` defines canonical stage, promotion, and rollback
records. Evidence is external-SHA-256-bound and uses immutable Generic versions derived from the
frozen release SHA and exact workflow run ID. Only the runtime deploy identity writes to
`boundarymesh-deployments/boundarymesh-deployment-evidence`; version collision is fatal.

The Hosting identity's predefined platform role includes site lifecycle capabilities that the
workflow does not need. Source fences prohibit `firebase init`, site creation/deletion, project
switching, arbitrary project/site input, and unexpected sites. The project and site are fixed in the
release lock and read back around each operation.

Phase 16E.4a changes source only. It does not register or dispatch the workflow, create deployment
evidence, mutate Cloud Run or Firebase Hosting, rebuild or publish artifacts, or deploy the frozen
release.
