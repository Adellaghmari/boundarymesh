# Project status

**Project:** BoundaryMesh (Project C)  
**Tagline:** Secure every system crossing.  
**Phase:** **2 COMPLETE** (Vue recruiter console foundation)  
**Overall:** **PROJECT C — NOT COMPLETE**

## Completeness

| Item | Status |
| --- | --- |
| Phase 0 architecture lock | **COMPLETE** |
| Phase 1 Java/Spring foundation | **COMPLETE** |
| Phase 2 Vue recruiter console | **COMPLETE** |
| Application production IAM / Entra / JWT | **NOT STARTED** |
| Apache Camel | **NOT STARTED** |
| Microsoft Graph | **NOT STARTED** |
| GCP resources | **NONE PROVISIONED** |
| Microsoft credentials | **NONE CREATED** |
| GCP service-account JSON keys | **NONE** |
| Project A | **FROZEN** — not modified |
| Project B | **FROZEN** — not modified |
| LIVE VERIFIED | **Nothing** |

## Phase 2 evidence

- Official `create-vue` scaffold in `frontend/`
- Vue 3 + TypeScript + Pinia + Vue Router + SCSS + Vite
- Typed API client for health, ready, and synthetic evaluate
- Session-only boundary events
- Vitest + Vue Test Utils + MSW
- Playwright smoke against the real local Java backend
- `frontend` typecheck, lint, unit tests, production build
- `backend/mvnw.cmd verify` still required as regression

Details: [docs/PHASE2.md](docs/PHASE2.md).

## Next (do not start automatically)

Phase 3 — Identity Federation Gateway. Do not implement OAuth2/OIDC, JWT/JWKS, Entra, Graph, HMAC, Camel, or GCP until that phase is requested.
