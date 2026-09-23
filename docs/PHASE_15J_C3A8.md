# Phase 15J-C3A — hosted authentication and private-core verification

Status at this historical gate: **LIVE VERIFIED / PASS.** No hosted browser transaction was submitted during C3A. Phase 15J-C3B later completed successfully; see [PHASE_15J_FINAL.md](PHASE_15J_FINAL.md) for the authoritative final freeze.

## Corrected gates

C3A preserved the history of four defects found through controlled live verification:

- The first Hosting release cached `/` and SPA fallbacks for one hour. C3A.1 applied `Cache-Control: no-cache` to every HTML shell while retaining `public, max-age=31536000, immutable` for hashed assets.
- An unstructured login `403` was incorrectly rendered as an operator allowlist failure. C3A.3 introduced distinct safe codes including `CSRF_REJECTED`, `OPERATOR_NOT_AUTHORIZED`, `ROUTE_NOT_ALLOWED`, and `ACCESS_DENIED`, with matching frontend handling.
- Firebase Hosting did not forward the original `__Host-bm_session` cookie. C3A.5 changed the single authoritative session cookie to Firebase Hosting's reserved `__session` name while retaining Secure, HttpOnly, SameSite=Lax, Path `/`, no Domain, a one-hour lifetime, revocation checking, and matching logout clearing.
- A valid private-core read took approximately 16 seconds while the BFF request timeout was 12 seconds. C3A.7 raised only that bounded per-request timeout to 30 seconds; the five-second connection timeout, no-retry behavior, no-redirect policy, response bound, keyless identity, and fail-closed handling remain unchanged.

## Live authentication and session evidence

The hosted Firebase Google flow returned HTTP 200 from `POST /session/login`, followed by HTTP 200 from `GET /session/me`. Firebase ID-token validation, verified-email and recent-sign-in requirements, the server-side operator UID allowlist, Firebase session verification with revocation checking, framework and application CSRF, and route allowlisting all passed. Firebase Hosting transported the Secure HttpOnly `__session` cookie successfully.

## Keyless private-core evidence

`boundarymesh-web-00004-k48`, pinned to `sha256:90570617750bd39613482fd75d8434f1a47299ae8d35908ae0c81717374ee30a`, handled one authenticated read of retained evidence:

`GET /api/v1/attestations/67e2f339-b122-4fc9-86f7-a7fb6f0b2cce/summary`

The BFF returned HTTP 200 in 17.284 seconds. `boundarymesh-api-00004-9hw` returned HTTP 200 in 16.561 seconds, within the new 30-second bound. The response described the existing signed ES256 attestation for `quayforge-orders`; the read created or changed no evidence.

The browser called only the Firebase Hosting same-origin path. The BFF workload identity `boundarymesh-web@boundarymesh-dev.iam.gserviceaccount.com` used the Google-signed ID-token path and service-level `roles/run.invoker` to invoke the private core. The core has no `allUsers`, no `allAuthenticatedUsers`, and no project-wide Invoker grant. The browser received no service-account credential, Cloud Run token, or Authorization header. API, Worker, and Web service accounts have zero user-managed keys.

## No-mutation and infrastructure truth

No C3A request called `POST /api/v1/transactions`. The successful read created no transaction, outbox event, application Pub/Sub message, worker processing, idempotency record, attestation, transaction audit-chain entry, or quarantine state. API and Worker images remained unchanged. Firestore, Pub/Sub, KMS, Secret Manager, Firebase Auth, IAM, and Microsoft/Entra remained unchanged. Firebase Hosting has three releases, and Compute Engine API remains disabled.

The final C3A Pulumi preview reported 39 unchanged resources, zero creates, updates, replacements, or deletes, and exit code 0. At this gate Phase 15J remained in progress; C3B later completed the exactly one controlled hosted browser transaction recorded in [PHASE_15J_FINAL.md](PHASE_15J_FINAL.md).

The Pulumi passphrase remains functional but was previously exposed in chat. Rotation remains recommended before the final whole-project security freeze; C3A does not claim complete credential hygiene.
