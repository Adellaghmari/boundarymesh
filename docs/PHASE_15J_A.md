# Phase 15J-A — frontend cloud architecture and readiness

Status: **PASS as an architecture/readiness gate.** No frontend hosting or browser cloud transaction is live. This document records the reviewed Phase 15I baseline and the design to implement in 15J-B; it is not a deployment record.

## Verified starting point

- Phase 15I HEAD: `e520acbc78a103d56cf84c98df42d4c7fe12984d`; working tree clean before 15J-A.
- `boundarymesh-api` is a Ready, IAM-private Cloud Run Service in `europe-west4`; `boundarymesh-worker` is a Ready Worker Pool without a URL. Their deployed images are immutable digests and use separate keyless service accounts.
- One synthetic transaction verified the internal Firestore → KMS → audit/attestation → outbox → Pub/Sub → worker → durable terminal quarantine → ACK path. External connector delivery was not exercised.
- Firebase Hosting is not deployed. `firebasehosting.googleapis.com` is disabled. No `firebase.json` or `.firebaserc` exists. The existing `firebaserules.googleapis.com` service alone does not prove Hosting setup. A read-only Firebase Management API request returned 403, so project linkage needs confirmation before any Hosting apply.

## Current browser truth

The Vue 3/TypeScript/Vite SPA has a substantial local console, but its production transaction route is unused. The client calls local `/api/v1/dev/**` lab routes and uses session-memory events. Audit, attestation, graph, and quarantine UI components exist, but the production Spring profile denies their current read routes. The shipped UI cannot yet display the real D1-style cloud transaction or its eventual worker disposition. The current frontend unit tests use MSW, and Playwright exercises the local Java profile.

15J-A corrected only obsolete static copy that said GCP, Firestore, Pub/Sub, Secret Manager, or Cloud KMS did not exist. The copy now distinguishes verified private cloud workloads from this unhosted, locally connected browser console. No backend, Pulumi, IAM, Firebase, or other cloud resource was changed.

## Chosen trust boundaries

```text
Browser (Firebase-hosted SPA)
  → same-origin /api and /session rewrites
  → public, thin Cloud Run web BFF (application-authenticated)
  → Google-signed service-to-service ID token from its keyless identity
  → IAM-private boundarymesh-api
  → existing application pipeline and worker
```

The **core API stays IAM-private** and receives no `allUsers` or `allAuthenticatedUsers` binding. Firebase Hosting serves only static assets and sends selected same-origin dynamic paths to the BFF. Hosting must not be treated as an IAM authenticator for the private core API. The BFF is the intentionally internet-facing edge, with its own authentication, route allowlist, request limits, and CSRF defense. It does not evaluate boundaries or access Firestore, Pub/Sub, KMS, or integration secrets.

Use Firebase Authentication for the 15J browser operator identity rather than altering the frozen Microsoft/Entra/Graph integration. The browser signs in with **in-memory Auth persistence only**, exchanges its short-lived end-user ID token with the BFF over HTTPS, and immediately clears client-side Auth state. The BFF verifies the token, recent authentication, and a server-side operator allowlist before issuing a short-lived `Secure`, `HttpOnly`, `SameSite=Lax` session cookie. It verifies the session and an anti-CSRF token on mutations. End-user tokens are never put in local/session storage, Vite configuration, static assets, or logs. A Firebase web API key/project ID, if needed for sign-in, are public configuration, not privileged credentials. The BFF's Google service identity and Cloud Run invocation token remain server-side. The required Firebase session-cookie permission must be reviewed as a minimal custom role rather than granting Firebase Authentication Admin.

The BFF gets a dedicated keyless workload service account and only service-level `roles/run.invoker` on `boundarymesh-api`, plus the minimum Firebase Auth session permission actually needed. The browser receives no service-account credential and no Cloud Run invocation token. The existing API and worker service accounts and IAM remain unchanged. The BFF strips incoming identity/authorization forwarding headers and proxies only approved product paths with bounded bodies. The core API's current `cloud-run-iam` evidence describes the BFF workload identity; it must not be represented as proof of the human operator's identity without a separately validated provenance design.

Alternatives rejected for this gate: requiring each browser user to have project IAM and a Cloud Run invocation token is not a product access model; static Hosting alone cannot hold a workload identity; the existing local Entra BFF is inside the private API and production Entra remains disabled, so it cannot bootstrap browser access without a new edge or an explicitly separate Entra gate.

## 15J-B implementation sequence

1. Confirm `boundarymesh-dev` Firebase project linkage and Authentication availability through an authorized read-only check. Resolve the current 403 without adopting unrelated service-agent resources. Choose the smallest operator allowlist and Google/Firebase sign-in configuration; no anonymous or open self-registration for transaction submission.
2. Add the thin web BFF module and its local fake-backed tests: Firebase Auth token-to-session exchange, session validation, authorization, CSRF, strict proxy allowlist, request-size limits, no credential/header leakage, and keyless service-to-service invocation with the core API URL as audience. Keep domain enforcement in the existing API.
3. Add a production frontend mode using same-origin `/api`. Integrate `POST /api/v1/transactions` with the existing request/response contract, `202` asynchronous semantics, loading/error states, and a real transaction detail flow. Hide or clearly separate local `/api/v1/dev/**` labs from the hosted production navigation; do not make broken lab buttons appear live.
4. Add the smallest safe production read path for worker delivery state, likely a validated `GET /api/v1/deliveries/{instructionId}` over the existing delivery-idempotency port. The submission response already returns `instruction_id`, `attestation_id`, and `audit_chain_id`; reuse existing attestation/audit read controllers where appropriate, correct their hard-coded local banners, and allow only selected safe `GET` routes in `ProdFoundationSecurityConfig`. The frontend currently calls `POST /api/v1/attestations/{id}/verify`, although a safe `GET` variant exists; use that variant for hosted readback. The existing quarantine list does not represent a worker terminal quarantine document and must not be used to claim it does. No broad production route wildcard or mutation route is needed.
5. Add `firebase.json` and `.firebaserc` only after ownership is confirmed. Serve `frontend/dist`; rewrite `/session/**` and allowlisted `/api/**` to the BFF in `europe-west4` before the catch-all SPA rewrite to `/index.html`. Use no-store on dynamic responses, no-cache on `index.html`, immutable caching on hashed assets, and reviewed CSP/security headers. Keep `VITE_API_BASE_URL` empty for same-origin production traffic; only public safe values enter `VITE_*`.
6. Define only the needed Hosting/API enablement, BFF Cloud Run service, dedicated keyless service account, API service-level Invoker member, and narrowly scoped Firebase Auth session permission in infrastructure. Preview first. Do not guess a resource count before provider and Firebase ownership inspection. Existing 33 resources must show no update/replacement/deletion except an explicitly reviewed core API image update for the safe read routes.
7. Run frontend typecheck/unit/production build/local Playwright, backend Maven/ArchUnit and read/security tests, BFF auth/CSRF/proxy tests, and Firebase Hosting emulator/deep-link tests. Build/push immutable API and BFF images only after tests and image hygiene checks pass. Deploy in dependency order: identity/IAM and BFF, updated private API read routes, then Hosting. Verify unauthorized and non-allowlisted calls fail before any valid browser transaction.

15J-B should stop after secure deployment and readiness verification. A later 15J-C should execute one browser-origin synthetic transaction, verify the returned decision and readback of signed attestation/audit and terminal worker quarantine, confirm no external connector traffic, inventory its application residue, and perform a separate cleanup/freeze gate. Rollback is by the previous immutable API/BFF revisions and previous Hosting release; do not change the worker or weaken API IAM.

## Non-negotiable truth

Firebase Hosting: **NOT DEPLOYED**. Browser production E2E: **NOT LIVE VERIFIED**. Production Entra/OIDC and external connector delivery: **NOT LIVE**. Existing private cloud transaction path: **LIVE VERIFIED**. The known exposed Pulumi passphrase is an accepted follow-up and was not touched in 15J-A.
