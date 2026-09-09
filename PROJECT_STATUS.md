# Project status

**Project:** BoundaryMesh (Project C)  
**Tagline:** Secure every system crossing.  
**Phase:** **4 COMPLETE** (Identity Federation Gateway + Webhook & Subscription Defense, local)  
**Overall:** **PROJECT C — NOT COMPLETE**

## Completeness

| Item | Status |
| --- | --- |
| Phase 0 architecture lock | **COMPLETE** |
| Phase 1 Java/Spring foundation | **COMPLETE** |
| Phase 2 Vue recruiter console | **COMPLETE** |
| Phase 3 Identity Federation Gateway | **COMPLETE** (local JWT/JWKS; Entra PLANNED) |
| Phase 4 Webhook & Subscription Defense | **COMPLETE** (QuayForge HMAC; Graph subscriptions PLANNED) |
| Application production IAM / Entra | **NOT STARTED** |
| Apache Camel | **NOT STARTED** |
| Microsoft Graph | **NOT STARTED** |
| GCP resources | **NONE PROVISIONED** |
| Microsoft credentials | **NONE CREATED** |
| GCP service-account JSON keys | **NONE** |
| Project A | **FROZEN** — not modified |
| Project B | **FROZEN** — not modified |
| LIVE VERIFIED | **Nothing** |

## Phase 3–4 evidence

- Spring Security OAuth2 resource server with asymmetric JWT + JWKS
- AuthN (401) vs AuthZ (403 / domain FAIL) kept separate
- QuayForge HMAC-SHA256 over raw bytes, constant-time compare, ±300s freshness
- Replay nonce store ≠ event-id idempotency
- Vue Identity Lab and Webhook Defense Lab against the real Java API
- Secrets are not shipped to the browser or committed

Details: [docs/PHASE3.md](docs/PHASE3.md), [docs/PHASE4.md](docs/PHASE4.md).

## Next (do not start automatically)

Phase 5 — Contract Drift Firewall. Do not implement Camel, Entra live, Graph, Secret Manager, or GCP until that phase is requested.
