# Infrastructure

The Google Cloud project is `boundarymesh-dev` in `europe-west4`. It is a portfolio and development project. Firebase Hosting is `boundarymesh-dev.web.app`.

## Topology

| Resource | Role |
| --- | --- |
| Firebase Hosting | Static site and rewrites to the web edge |
| `boundarymesh-web` | Public Cloud Run service |
| `boundarymesh-api` | IAM-private Cloud Run service |
| `boundarymesh-worker` | Cloud Run worker pool, no ingress |
| Firestore Native | Transactions, outbox, quarantine, audit |
| Pub/Sub | `boundarymesh-deliveries` and the worker subscription |
| Secret Manager | Named secrets. Values stay in the service |
| Cloud KMS | `exchange-attestation` and Pulumi `state-secrets` |
| Artifact Registry | Docker images and generic release packages |
| Cloud Storage | Pulumi state bucket `gs://bm-pulumi-state-eu4-21cad20b325f` |

The Compute Engine API is disabled. Pulumi may warn about that. The warning is expected. There are no VMs.

## Pulumi ownership

Stack `dev` is Pulumi TypeScript. The live count is 83 managed resources. Preview is `pulumi preview --stack dev --diff --non-interactive --suppress-permalink`. A zero-drift preview reports 83 unchanged and zero operations.

Pulumi ignores changes to image, revision, traffic, and worker `instanceSplits`. Continuous delivery owns those fields. A promoted revision is not Pulumi drift.

Firebase Hosting is outside Pulumi. Hosting updates go through the deploy workflow.

Local preview uses Application Default Credentials. Do not set `GOOGLE_APPLICATION_CREDENTIALS` and do not commit credential files.

State encryption uses Cloud KMS key `boundarymesh-pulumi/state-secrets`. The preview identity can read state and decrypt that key. It cannot write state or apply.

Saved-plan apply is not a workflow in this repository. Infrastructure apply stays a reviewed local operation.

## Runtime boundaries

API and web minimum instances are 0. The worker pool uses manual scaling of one instance, so it has an idle cost.

CD-owned parent image digests for the promoted release:

```text
boundarymesh-api@sha256:bcfd9b01a3189228f583fa3f5d1ed8797f73d214af85bea4d84caeedfa0db8d7
boundarymesh-worker@sha256:7b1841487a2d1b3751e8bc63ee5323965e76ebeae9ae581c95b672f50154bb2d
boundarymesh-web-edge@sha256:85f98f90450b7807f55649f58807f952010a6278793b808ae1f4df78a22c0b04
```

Cloud Run may resolve a linux/amd64 child digest at runtime. That resolution is not treated as drift when the parent digest still matches.

## Versions in this tree

Java 25, Spring Boot 4.1.1, Spring Security 7.1, Apache Camel 4.22.0. Vue 3.5, Vite 8, TypeScript 6, Vitest 4, Playwright 1.63. Pulumi packages in `infra/package.json` track Pulumi 3 and the Google Cloud provider 9. The CLI used for local preview may be newer than the library. Pin checks belong in the lockfiles, not in a second inventory.

## Cost

These are estimates, not an invoice.

Idle API and web services sit near the Cloud Run free tier because minimum instances are 0. Cloud KMS software keys are a small monthly charge per version. Firestore, Pub/Sub, Hosting, and Secret Manager stay inside free or low-use tiers at portfolio volume.

The worker pool is the cost exception. One always-on instance is tens of dollars if left running, not cents. Debug logging of payloads is the other trap. Production logs should stay at information level, without request bodies, tokens, or HMAC material.

There is no GKE cluster, Cloud SQL, Cloud NAT, or global HTTPS load balancer.

## Intentional limits

- Dead-letter forwarding is not configured.
- External connector endpoints are empty.
- Production Entra is disabled.
- Compute Engine stays disabled.
- Pulumi does not deploy Hosting and does not move traffic.
