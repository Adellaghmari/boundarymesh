# Phase 15J-C2 — controlled web-edge runtime deployment

Status: **LIVE RUNTIME DEPLOYED / VERIFIED.** The reviewed Pulumi apply created six web-edge resources and updated the private core API in place. The final Pulumi preview reported **39 unchanged** and zero operations.

## Live services and trust boundaries

- `boundarymesh-api` is Ready on its reviewed immutable Phase 15J image and continues to run as `boundarymesh-api@boundarymesh-dev.iam.gserviceaccount.com`.
- The core API remains IAM-private. It has no `allUsers` or `allAuthenticatedUsers` binding.
- `boundarymesh-web` is Ready on its reviewed immutable BFF image and runs as `boundarymesh-web@boundarymesh-dev.iam.gserviceaccount.com`.
- `allUsers` has `roles/run.invoker` on `boundarymesh-web` only. That is a public **transport** edge, not public application access.
- The BFF workload identity has `roles/run.invoker` on `boundarymesh-api` at service scope only. No project-wide Cloud Run Invoker grant was created.
- The custom `boundarymeshWebSession` role is limited to Firebase session creation, lookup, and update; it is bound only to the BFF workload identity.

## Verified fail-closed behavior

Live safe requests to the BFF produced the intended results without submitting a BoundaryMesh transaction:

- unauthenticated allowlisted read: `401`;
- non-allowlisted route: `403`;
- `/api/v1/dev/**`: `403`;
- mutation without a valid session and CSRF proof: `403`.

The API, Worker, and Web service accounts have zero user-managed keys. The Worker Pool, Firestore, Pub/Sub, Cloud KMS, Secret Manager, Microsoft/Entra state, and Compute Engine API state were not changed by C2. Compute Engine remains disabled.

## Deferred evidence

Firebase Hosting has a site but **zero releases** and is **not deployed**. Therefore these claims remain intentionally unverified:

- authenticated Firebase browser session;
- authenticated BFF-to-private-core application path;
- hosted browser transaction and browser cloud E2E.

Those checks belong to Phase 15J-C3 after an explicitly approved Hosting deployment. No transaction was submitted during C2.
