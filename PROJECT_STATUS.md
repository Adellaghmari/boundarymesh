# Project status

**Project:** BoundaryMesh (Project C)  
**Tagline:** Secure every system crossing.  
**Phase:** **15F COMPLETE** — named Firestore Native database `boundarymesh` **LIVE / PROVISIONED** in `europe-west4`. Production Java SDK path **LIVE VERIFIED LOCALLY** against real GCP, then run-scoped documents **CLEANED**. Artifact Registry and Secret Manager unchanged. Cloud Run workload persistence is **not** live.  
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
| Phase 10 Quarantine & Recovery | **COMPLETE** (in-memory default; Firestore adapter **IMPLEMENTED + EMULATOR TESTED + LIVE VERIFIED LOCALLY**) |
| Phase 11 Tamper-Evident Audit Chain | **COMPLETE** (in-memory default; Firestore transactional append **IMPLEMENTED + EMULATOR TESTED + LIVE VERIFIED LOCALLY**) |
| Phase 12 Cryptographic Exchange Attestation | **COMPLETE** (local JCA P-256 **LIVE VERIFIED LOCALLY** for one Graph event; Cloud KMS PLANNED) |
| Phase 13 Integration Boundary Graph | **COMPLETE** (`m365-calendar` OBSERVED/ACTIVE from the live event; Helix 365 SYNTHETIC) |
| Phase 14 premium navigation | **COMPLETE** |
| Phase 14 Entra + Graph connector | **LIVE VERIFIED LOCALLY** (Graph HTTP 200, `count=1`, `ALLOW_WITH_TRANSFORMATION`). Change notifications **PLANNED** |
| Phase 15A Pulumi TypeScript foundation | **IMPLEMENTED + LOCALLY VALIDATED** — `infra/` compiles; zero application resources |
| Phase 15B GCP project / provider validation | **IMPLEMENTED + LOCALLY VALIDATED** — `boundarymesh-dev`, `europe-west4`, ADC, local Pulumi backend, zero-resource preview |
| Phase 15C GCP adapter module + emulator foundation | **IMPLEMENTED + TESTED** — `backend/gcp` |
| Phase 15D Artifact Registry + container images | **COMPLETE** — registry live; images pushed + remote digest verified. Workloads not deployed |
| Phase 15E Secret Manager | **COMPLETE** — API LIVE; four empty containers LIVE. No versions. Adapter not workload-live |
| Phase 15F-A Firestore persistence + preview | **COMPLETE** — adapters in `backend/gcp`; emulator suite passed; preview approved |
| Phase 15F-B Firestore infrastructure apply | **COMPLETE** — `firestore.googleapis.com` LIVE; database `boundarymesh` LIVE / PROVISIONED |
| Phase 15F-C live Java SDK data path | **COMPLETE** — `GoogleFirestoreFacade` + official Java SDK + ADC against `projects/boundarymesh-dev/databases/boundarymesh`. Residue **CLEANED**. Not Cloud Run workload-live |
| Application production IAM / Entra live | **LIVE VERIFIED LOCALLY** — common authority, personal Microsoft account, server-side session |
| Microsoft Graph live calendar read | **LIVE VERIFIED LOCALLY** — provider 200, one event normalized and enforced |
| GCP project | **CREATED + BILLING LINKED** (`boundarymesh-dev`) |
| GCP resources | Artifact Registry + Secret Manager (empty) + Firestore API + named database `boundarymesh` (no run-scoped residue). No workloads |
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
6. Quarantine & Recovery Orchestrator — local **TESTED**; Firestore persistence **LIVE VERIFIED LOCALLY**. Not Cloud Run workload-live.

Also: Tamper-Evident Audit Chain **LIVE VERIFIED LOCALLY** for the Graph event; Cryptographic Exchange Attestation **LIVE VERIFIED LOCALLY** (local JCA; Cloud KMS **PLANNED**); Integration Boundary Graph **LIVE VERIFIED LOCALLY** for `m365-calendar` OBSERVED. Firestore adapters **IMPLEMENTED + EMULATOR TESTED + LIVE VERIFIED LOCALLY**. Firestore database **LIVE / PROVISIONED**. Live verification residue **CLEANED**. Cloud Run workload persistence is **not** live.

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

Phase 15G delivery outbox (Firestore persist-then-publish foundation). Do not add secret versions, Secret Manager IAM, Pub/Sub topics, Cloud KMS, Cloud Run, Firebase Hosting, or WIF. Do not start automatically.
