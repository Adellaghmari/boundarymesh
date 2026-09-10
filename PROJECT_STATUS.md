# Project status

**Project:** BoundaryMesh (Project C)  
**Tagline:** Secure every system crossing.  
**Phase:** **14B COMPLETE** for local Microsoft Graph provider verification. Graph change notifications and GCP remain **PLANNED**.  
**Overall:** **PROJECT C — NOT COMPLETE**

## Completeness

| Item | Status |
| --- | --- |
| Phase 0 architecture lock | **COMPLETE** |
| Phase 1 Java/Spring foundation | **COMPLETE** |
| Phase 2 Vue recruiter console | **COMPLETE** |
| Phase 3 Identity Federation Gateway | **COMPLETE** (local JWT/JWKS **TESTED**; Entra OIDC Operator login **LIVE VERIFIED LOCALLY**) |
| Phase 4 Webhook & Subscription Defense | **COMPLETE** (QuayForge HMAC; Graph subscriptions **PLANNED**) |
| Phase 5 Contract Drift Firewall | **COMPLETE** |
| Phase 6 Data Boundary Firewall | **COMPLETE** (includes M365 calendar egress policy) |
| Phase 7 Engine expansion | **COMPLETE** |
| Phase 8 Apache Camel delivery | **COMPLETE** (local worker; Pub/Sub PLANNED) |
| Phase 9 Credential & Consent | **COMPLETE** (local model + delegated Calendars.Read mapping; Secret Manager PLANNED) |
| Phase 10 Quarantine & Recovery | **COMPLETE** (in-memory; Firestore PLANNED) |
| Phase 11 Tamper-Evident Audit Chain | **COMPLETE** (in-memory SHA-256 chain; Firestore PLANNED) |
| Phase 12 Cryptographic Exchange Attestation | **COMPLETE** (local JCA P-256; Cloud KMS PLANNED) |
| Phase 13 Integration Boundary Graph | **COMPLETE** (state-driven Vue SVG; includes M365 Calendar node) |
| Phase 14 premium navigation | **COMPLETE** |
| Phase 14 Entra + Graph connector | **LIVE VERIFIED LOCALLY** (Graph HTTP 200, empty calendar). Change notifications **PLANNED** |
| Application production IAM / Entra live | **LIVE VERIFIED LOCALLY** — common authority, personal Microsoft account, server-side session |
| Microsoft Graph live calendar read | **LIVE VERIFIED LOCALLY** — provider 200, `count=0`. Event payload transformation **not** verified |
| GCP resources | **NONE PROVISIONED** |
| Microsoft credentials | **NONE CREATED in Git / this session** |
| GCP service-account JSON keys | **NONE** |
| Project A | **FROZEN** — not modified |
| Project B | **FROZEN** — not modified |
| LIVE VERIFIED LOCALLY | Entra OIDC + Microsoft Graph calendar provider (empty calendar). Not GCP. Not Graph notifications |

## ControlVector audit

| Control | Implemented | Tested | Real or synthetic |
| --- | --- | --- | --- |
| identity | yes | yes | real local JWT; Operator Entra OIDC LIVE VERIFIED LOCALLY (personal Microsoft account) |
| authorization | yes | yes | real scopes including `SCOPE_Calendars.Read` on admin chain |
| authenticity | yes | yes | real HMAC |
| freshness | yes | yes | real timestamp window |
| replay | yes | yes | real nonce store |
| contract | yes | yes | real schema/registry including `m365-calendar/v1` |
| egress | yes | yes | real destination policy including `m365-calendar-egress/v1` |
| credential resolvability | yes | yes | real local catalog/evaluator |
| connector safety | yes | yes | real circuit/config snapshot |

## Six flagships

1. Identity Federation Gateway — local JWT **TESTED**; Entra OIDC BFF **LIVE VERIFIED LOCALLY**.
2. Webhook & Subscription Defense — QuayForge HMAC **TESTED**; Graph subscriptions **PLANNED**.
3. Contract Drift Firewall — **TESTED**.
4. Data Boundary Firewall — **TESTED**.
5. Credential & Consent Vault — local model **TESTED**; Microsoft delegated consent mapped; Secret Manager **PLANNED**.
6. Quarantine & Recovery Orchestrator — local **TESTED**, cloud persistence **PLANNED**.

Also: Tamper-Evident Audit Chain **TESTED**; Cryptographic Exchange Attestation **TESTED locally**; Integration Boundary Graph **TESTED locally**. Cloud KMS and Firestore **PLANNED**.

## LIVE MICROSOFT VERIFICATION (local, 2026-09-10)

Human-controlled Operator browser completed `POST /api/v1/admin/microsoft/calendar/pull`.

Safe recorded result:

- pull HTTP 200, `mode=LIVE_VERIFIED`, `live=true`, `permission=Calendars.Read`, `provider_status=200`, `count=0`
- common v2 authority, personal Microsoft account, server-side OIDC session, SPA CSRF
- live adapter: `MicrosoftGraphCalendarAdapter` (Microsoft Graph Java SDK)
- audit chain `m365-calendar`: `CONNECTOR_READ` with `provider=MICROSOFT_GRAPH`, `live=true`, `count=0`, continuity `VALID`
- no access/refresh/ID tokens, Authorization headers, calendar subjects, attendees, bodies, or join URLs in audit, attestations list, graph payload, connector GET, or recruiter UI
- Vue receives connector/session categories only; no Microsoft tokens
- zero events: no `m365-calendar/v1` payload entered the Data Boundary Firewall on this pull
- zero events: no Exchange Attestation and no Boundary Graph **OBSERVED** edge for this pull (`m365-calendar` remains **CONFIGURED**)
- Helix 365 remains SYNTHETIC DEMO
- not an enterprise Exchange Online mailbox
- Graph change notifications remain **PLANNED**
- contract harness was not used as this evidence

GET `/api/v1/microsoft/connector` still reports `LIVE_CONFIGURED` because that endpoint never calls Graph. `LIVE_VERIFIED` was the Operator pull response after Graph HTTP 2xx.

## Next (do not start automatically)

Phase 15 — GCP adapters remain planned. Do not provision Cloud Run, Firestore, Pub/Sub, Secret Manager, Cloud KMS, Artifact Registry, Firebase Hosting, WIF, or Pulumi until that phase is requested.
