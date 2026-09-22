# Phase 15J-C3A.5 — Firebase Hosting session cookie compatibility

Status: **PASS / DEPLOYMENT READY / NOT DEPLOYED.** Phase 15J-C3A.4 remains blocked at hosted session verification. Firebase Hosting is live with three releases; no Hosting redeploy, browser sign-in retry, or BoundaryMesh transaction occurred in C3A.5.

## Proven failure and topology

In three hosted attempts, `POST /session/login` returned 200 and the immediate `GET /session/me` returned 401 / `SESSION_INVALID`. The production path is browser → Firebase Hosting same-origin `/session/**` rewrite → `boundarymesh-web`. Firebase Hosting forwards only a cookie named `__session` to a rewritten Cloud Run application. The live BFF image issues `__Host-bm_session`, which cannot carry the session through that rewrite.

## Correction and security invariants

`SessionController.SESSION_COOKIE` is the single authoritative name for login issuance, session reads, proxy authorization, and logout clearing. It is now `__session`. There is no fallback to the old cookie. The cookie remains Secure, HttpOnly, SameSite=Lax, Path `/`, host-only with no explicit Domain, and limited to one hour. Logout revokes the Firebase session and clears the same cookie with `Max-Age=0`.

Firebase ID-token validation, Firebase session verification with revocation checking, recent-sign-in and verified-email requirements, UID allowlisting, CSRF protection, route allowlisting, and keyless BFF-to-private-core authentication are unchanged. The core API remains IAM-private. No cookie value or credential is logged or bundled for the browser.

Focused security tests cover the reserved cookie on login, successful and failed session reads, rejection of the old cookie name, operator authorization, CSRF, and matching logout attributes. The full Maven reactor and infrastructure tests pass. The immutable BFF image `phase15j-c3a5-20260922t114138z` was built and pushed as `sha256:190a4860fd57939d5ef3020c00f5f01e3b6fb5a8ca74e5ae442e853882ae25cf`; Pulumi pins that digest. The reviewed preview reports one `boundarymesh-web` image update, 38 unchanged resources, zero creates, replacements, or deletes, and exit code 0. The live BFF still runs the prior C3A.3 digest until a separate approved deployment. The hosted session, authenticated BFF-to-core read, and browser transaction remain **not live verified**.
