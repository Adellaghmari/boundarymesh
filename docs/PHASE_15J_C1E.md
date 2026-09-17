# Phase 15J-C1E — immutable deployment preparation

Status: **PRE-DEPLOYMENT READY.** No Cloud Run service, IAM resource, or Firebase Hosting release was applied by this phase.

## Prepared artifacts

- The private `boundarymesh-api` image was built from the current 15J source and pushed to Artifact Registry at immutable digest `sha256:df4ca576227b1311194af1b3c821962a228bc7222d144eb4b4fbfb41590e072d`.
- The separate `boundarymesh-web` BFF image was built from the current 15J source and pushed at immutable digest `sha256:70b4af9b62b5568aaf324088bcd9c74250ccc52da05e63c67bbb416ad69dcabb`.
- The Worker Pool image remains pinned to its Phase 15I digest and is unchanged.
- The public Firebase Web App configuration is compiled only into the cloud SPA build. It contains public browser configuration, never an Admin credential or browser token.
- The verified Firebase operator UID is configured only as server-side Pulumi configuration for `BOUNDARYMESH_WEB_OPERATOR_UIDS`; an empty list remains deny-all.

## Deployment boundary

The reviewed preview proposes six new Pulumi resources for the BFF identity, narrow core-API invoker member, Firebase session custom role/member, BFF service, and BFF-only public invoker. It updates `boundarymesh-api` in place only to its new immutable image. The core API remains IAM-private and receives neither `allUsers` nor `allAuthenticatedUsers`.

Firebase project association, Firebase Auth, Google sign-in, the Web App, and the default Hosting site are Firebase-managed bootstrap state. The Hosting site exists but has **zero releases**. Firebase Hosting, the BFF, and the prepared core API revision are **not deployed**. Browser cloud E2E is **not live verified**.
