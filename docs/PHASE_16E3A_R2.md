# Phase 16E.3a-R2 — split-authority deployment foundation

Status: **SOURCE IMPLEMENTED / NOT LIVE APPLIED.** This source defines two distinct keyless
deployment identities. It does not create a deployment workflow, apply infrastructure, publish an
artifact, or deploy a workload.

## Authority split

`boundarymesh-deploy` is limited to existing-resource Cloud Run Service and Worker Pool updates,
runtime service-account `actAs`, private API health invocation, frozen release reads, and writes to
the deployment-evidence repository. `boundarymesh-hosting-deploy` can only read the frozen Generic
release package and administer Firebase Hosting.

Firebase Hosting does not support custom roles for Hosting control. The Hosting-only identity
therefore receives project-scoped `roles/firebasehosting.admin`. That predefined role includes
`firebasehosting.sites.create` and `firebasehosting.sites.delete`. These operations are
**BOUNDARYMESH-WORKFLOW-FORBIDDEN**. The accepted containment model is a separate service account,
separate `boundarymesh-hosting-deploy` GitHub environment, WIF only, zero keys, private immutable
repository/owner claims, `refs/heads/main`, exact `deploy-release.yml` workflow identity, and
`workflow_dispatch`. The future workflow must reject site creation, site deletion, `firebase init`,
project switching, and arbitrary Firebase project or site inputs. It must read and re-verify the
fixed `boundarymesh-dev` site identity before and after every Hosting operation.

## Cloud Run permission contract

The custom `boundarymeshControlledRunDeployer` role contains only:

- `run.services.get`, `run.services.update`, and `run.services.getIamPolicy` for existing API/Web
  read, update, and IAM-invariant verification;
- `run.revisions.get` and `run.revisions.list` for Service and Worker Pool revision verification;
- `run.workerpools.get`, `run.workerpools.update`, and `run.workerpools.getIamPolicy` for the existing
  Worker Pool and its IAM invariant;
- `run.operations.get` to observe the long-running result returned by Cloud Run v2 PATCH.

The role is bound separately to `boundarymesh-api`, `boundarymesh-web`, and `boundarymesh-worker`.
It has no create, delete, Job, source-upload, SSH, `setIamPolicy`, project-IAM, or API-management
permission. Artifact Registry Reader and runtime service-account User are separate resource-scoped
grants. API Invoker is granted only on the private API.

## Release ownership fence

Controlled CD owns only these Pulumi provider paths:

- API: `template.containers[0].image`, `template.revision`, `traffics`;
- Web: `template.containers[0].image`, `template.revision`, `traffics`;
- Worker: `template.containers[0].image`, `template.revision`, `instanceSplits`.

Pulumi retains service accounts, environment, probes, scaling, concurrency, CPU/memory, ingress,
IAM, and every other workload field. After an external deployment, operators must perform a
controlled targeted state reconciliation before any later Pulumi update that could touch these
resources; `ignoreChanges` can otherwise resend stale state during an unrelated update. Phase
16E.3a-R2 itself performs no refresh and no apply.

## Evidence boundary

`boundarymesh-deployments` is a protected regional Generic Artifact Registry repository for the
future `boundarymesh-deployment-evidence` package. It is separate from the immutable application
release repositories. Only the runtime deploy identity receives repository-scoped Writer; the
Hosting identity receives no evidence writer authority.

The frozen release remains source commit
`a876a7bfa5fa86f88e8387efb557773281daa6e6` with manifest SHA-256
`ecdbaf88710ff73fd694c5c4e13c0a8ce300fdacf2a3761064b7511bd627300d`. This foundation does not
rebuild, retag, republish, or deploy it.
