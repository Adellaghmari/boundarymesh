# Phase 15J-C3A.3 — structured web-session failure diagnostics

Status: **IMPLEMENTED / TESTED / DEPLOYMENT READY.** The diagnostic BFF image is built and pushed, but it is not deployed. The live BFF remains on its Phase 15J-C2 image; Firebase Hosting remains live and no browser retry or transaction occurred in this gate.

## Error contract

The web edge now returns a small, token-free JSON error contract for security failures:

- Spring Security CSRF failures return `403` with `CSRF_REJECTED`.
- A verified Firebase identity outside the server-side operator allowlist returns `403` with `OPERATOR_NOT_AUTHORIZED`.
- A non-allowlisted route returns `403` with `ROUTE_NOT_ALLOWED`.
- A non-CSRF access denial remains `ACCESS_DENIED`.

The handlers expose only `error_code`, set `Cache-Control: no-store`, and never return tokens, cookies, UID values, allowlist contents, exception names, or stack traces. Existing identity/session codes remain distinct, including `INVALID_IDENTITY`, `RECENT_SIGN_IN_REQUIRED`, and `SESSION_INVALID`.

## Browser behavior

The hosted console displays the operator-allowlist message only for `OPERATOR_NOT_AUTHORIZED`. It displays a neutral secure-session message for `CSRF_REJECTED` and a separate neutral rejection message for an unknown `403`. The UI no longer labels every forbidden login response as an operator mismatch.

## Verification and deployment readiness

- Maven reactor: 379 tests passed with zero failures, errors, or skips; focused BFF security coverage includes the structured CSRF, operator, route, and generic-denial cases.
- Frontend typecheck, 63 unit tests, and cloud build passed. Infrastructure typecheck, four tests, and build passed.
- The immutable BFF image tag `phase15j-c3a3-20260922t005534z` resolves to `sha256:d235f2c92e99fb9d86e5f2bd08f533e06308e7736e781c3afeeba917ed9e2f33` in Artifact Registry.
- Manual Pulumi preview reports only one update: `boundarymesh-web` moves to that immutable image. It reports 38 unchanged, with zero creates, replacements, or deletes.

The previous hosted login failure remains **unclassified**. Its only proven root cause is that the browser UI mislabeled the unstructured `403` as an operator allowlist failure. A controlled C3A.4 BFF-only deployment and one later browser retry can now identify the real state through the new safe code, without weakening authentication or authorization.

The previously exposed short-lived CSRF bootstrap value is acknowledged as **EXPOSED IN LOCAL TOOL OUTPUT / NOT COMMITTED / NOT STORED / SHORT-LIVED SESSION MATERIAL**. It is not reproduced here.
