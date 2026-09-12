# Project status

**Project:** BoundaryMesh (Project C)  
**Tagline:** Secure every system crossing.  
**Phase:** **15A COMPLETE** — Pulumi TypeScript repository foundation (zero resources). GCP provider/auth and all GCP services remain **PLANNED**.  
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
| Phase 6 Data Boundary Firewall | **COMPLETE** (`m365-calendar-egress/v1` **LIVE VERIFIED LOCALLY** on one real Graph event) |
| Phase 7 Engine expansion | **COMPLETE** |
| Phase 8 Apache Camel delivery | **COMPLETE** (local worker; Pub/Sub PLANNED) |
| Phase 9 Credential & Consent | **COMPLETE** (local model + delegated Calendars.Read mapping; Secret Manager PLANNED) |
| Phase 10 Quarantine & Recovery | **COMPLETE** (in-memory; Firestore PLANNED) |
| Phase 11 Tamper-Evident Audit Chain | **COMPLETE** (in-memory SHA-256 chain; Firestore PLANNED) |
| Phase 12 Cryptographic Exchange Attestation | **COMPLETE** (local JCA P-256 **LIVE VERIFIED LOCALLY** for one Graph event; Cloud KMS PLANNED) |
| Phase 13 Integration Boundary Graph | **COMPLETE** (`m365-calendar` OBSERVED/ACTIVE from the live event; Helix 365 SYNTHETIC) |
| Phase 14 premium navigation | **COMPLETE** |
| Phase 14 Entra + Graph connector | **LIVE VERIFIED LOCALLY** (Graph HTTP 200, `count=1`, `ALLOW_WITH_TRANSFORMATION`). Change notifications **PLANNED** |
| Phase 15A Pulumi TypeScript foundation | **IMPLEMENTED + LOCALLY VALIDATED** — `infra/` compiles; zero resources; no `pulumi` / `gcloud` |
| Application production IAM / Entra live | **LIVE VERIFIED LOCALLY** — common authority, personal Microsoft account, server-side session |
| Microsoft Graph live calendar read | **LIVE VERIFIED LOCALLY** — provider 200, one event normalized and enforced |
| GCP resources | **NONE PROVISIONED** |
| Microsoft credentials | **NONE CREATED in Git / this session** |
| GCP service-account JSON keys | **NONE** |
| Project A | **FROZEN** — not modified |
| Project B | **FROZEN** — not modified |
| LIVE VERIFIED LOCALLY | Entra OIDC + Graph calendar event crossing (normalization, egress, decision, audit, attestation, OBSERVED graph). Not GCP. Not Graph notifications |

## ControlVector audit

| Control | Implemented | Tested | Real or synthetic |
| --- | --- | --- | --- |
| identity | yes | yes | real local JWT; Operator Entra OIDC LIVE VERIFIED LOCALLY (personal Microsoft account) |
| authorization | yes | yes | real scopes including `SCOPE_Calendars.Read` on admin chain |
| authenticity | yes | yes | real HMAC |
| freshness | yes | yes | real timestamp window |
| replay | yes | yes | real nonce store |
| contract | yes | yes | real schema/registry; live Graph event `m365-calendar/v1` PASS |
| egress | yes | yes | live Graph event `m365-calendar-egress/v1` PASS, `EGRESS_REDACTION_REQUIRED` |
| credential resolvability | yes | yes | real local catalog/evaluator |
| connector safety | yes | yes | real circuit/config snapshot |

## Six flagships

1. Identity Federation Gateway — local JWT **TESTED**; Entra OIDC BFF **LIVE VERIFIED LOCALLY**.
2. Webhook & Subscription Defense — QuayForge HMAC **TESTED**; Graph subscriptions **PLANNED**.
3. Contract Drift Firewall — **TESTED**.
4. Data Boundary Firewall — **TESTED**; `m365-calendar-egress/v1` **LIVE VERIFIED LOCALLY**.
5. Credential & Consent Vault — local model **TESTED**; Microsoft delegated consent mapped; Secret Manager **PLANNED**.
6. Quarantine & Recovery Orchestrator — local **TESTED**, cloud persistence **PLANNED**.

Also: Tamper-Evident Audit Chain **LIVE VERIFIED LOCALLY** for the Graph event; Cryptographic Exchange Attestation **LIVE VERIFIED LOCALLY** (local JCA; Cloud KMS **PLANNED**); Integration Boundary Graph **LIVE VERIFIED LOCALLY** for `m365-calendar` OBSERVED. Firestore **PLANNED**.

## LIVE MICROSOFT VERIFICATION (local, 2026-09-10)

Human-controlled Operator browser completed `POST /api/v1/admin/microsoft/calendar/pull` twice in this process.

Empty-calendar pull: Graph HTTP 200, `count=0` (provider verified only).

Event pull: Graph HTTP 200, `mode=LIVE_VERIFIED`, `live=true`, `permission=Calendars.Read`, `provider_status=200`, `count=1`.

Safe recorded result for the event pull:

- path: Microsoft Graph → `MicrosoftGraphCalendarAdapter` → `m365-calendar/v1` → contract PASS → `m365-calendar-egress/v1` → engine `ALLOW_WITH_TRANSFORMATION` (`EGRESS_REDACTION_REQUIRED`) → audit → SIGNED local JCA attestation (verify VALID) → graph OBSERVED/ACTIVE
- audit chain `m365-calendar` continuity VALID: `CONNECTOR_READ` count=1, `BOUNDARY_DECISION`, `DELIVERY_RESULT` NOT_ATTEMPTED, `ATTESTATION_CREATED` SIGNED
- applicable controls PASS; authenticity/freshness/replay NOT_APPLICABLE; transformation_count=4 (subject REDACT + attendees/body/join URL DENY)
- live adapter selects only id/start/end/showAs from Graph
- no access/refresh/ID tokens, Authorization headers, calendar subjects, attendees, bodies, or join URLs in audit, attestation, graph, connector GET, recruiter UI, localStorage, or sessionStorage
- Vue receives connector/session categories only
- Helix 365 remains SYNTHETIC DEMO
- not an enterprise Exchange Online mailbox
- Graph change notifications remain **PLANNED**
- contract harness was not used as this evidence

GET `/api/v1/microsoft/connector` still reports `LIVE_CONFIGURED` because that endpoint never calls Graph. `LIVE_VERIFIED` was the Operator pull response after Graph HTTP 2xx.

## Next (do not start automatically)

Phase 15B — GCP project / provider validation. Do not provision Artifact Registry, Cloud Run, Firestore, Pub/Sub, Secret Manager, Cloud KMS, Firebase Hosting, or WIF until those subphases are requested. Do not run `pulumi up`.
