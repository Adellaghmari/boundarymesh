# Project status

**Project:** BoundaryMesh (Project C)  
**Tagline:** Secure every system crossing.  
**Phase:** **15J-C2 PASS** — the private Cloud Run API was updated to its reviewed immutable image and the separate public-transport web edge was deployed. The web edge is application-authenticated, fail-closed, and invokes the private core API with its dedicated keyless workload identity. Firebase Hosting has zero releases, so authenticated browser E2E remains deferred to Phase 15J-C3. Final Pulumi preview: 39 unchanged, zero operations.
**Overall:** **PROJECT C — PHASE 15I FUNCTIONALLY COMPLETE; PHASE 15J-C2 RUNTIME DEPLOYMENT VERIFIED**

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
| Phase 8 Apache Camel delivery | **COMPLETE** (local worker HTTP default; Pub/Sub adapters LIVE VERIFIED; deployed D1 publish/consume LIVE VERIFIED) |
| Phase 9 Credential & Consent | **COMPLETE** (local model + delegated Calendars.Read mapping; Secret Manager PLANNED) |
| Phase 10 Quarantine & Recovery | **COMPLETE** (in-memory default; Firestore adapter **IMPLEMENTED + EMULATOR TESTED + LIVE VERIFIED LOCALLY**) |
| Phase 11 Tamper-Evident Audit Chain | **COMPLETE** (in-memory default; Firestore transactional append **IMPLEMENTED + EMULATOR TESTED + LIVE VERIFIED LOCALLY**) |
| Phase 12 Cryptographic Exchange Attestation | **COMPLETE** (local JCA P-256 **LIVE VERIFIED LOCALLY** for one Graph event; Cloud KMS signing **LIVE VERIFIED LOCALLY**) |
| Phase 13 Integration Boundary Graph | **COMPLETE** (`m365-calendar` OBSERVED/ACTIVE from the live event; Helix 365 SYNTHETIC) |
| Phase 14 premium navigation | **COMPLETE** |
| Phase 14 Entra + Graph connector | **LIVE VERIFIED LOCALLY** (Graph HTTP 200, `count=1`, `ALLOW_WITH_TRANSFORMATION`). Change notifications **PLANNED** |
| Phase 15A Pulumi TypeScript foundation | **IMPLEMENTED + LOCALLY VALIDATED** — `infra/` compiles; zero application resources |
| Phase 15B GCP project / provider validation | **IMPLEMENTED + LOCALLY VALIDATED** — `boundarymesh-dev`, `europe-west4`, ADC, local Pulumi backend, zero-resource preview |
| Phase 15C GCP adapter module + emulator foundation | **IMPLEMENTED + TESTED** — `backend/gcp` |
| Phase 15D Artifact Registry + container images | **COMPLETE / HISTORICAL** — registry and original images remain live and untouched |
| Phase 15I-C1 production images | **COMPLETE** — current API/worker images built, smoke-tested, pushed, remote-digest verified, and pinned in Pulumi; workloads not deployed |
| Phase 15I-C2 Cloud Run deployment | **COMPLETE** — Run API enabled; private API Service and no-endpoint Worker Pool deployed and Ready on immutable images. One internal Firestore/Pub/Sub/KMS workload transaction was LIVE VERIFIED in Phase 15I-D1 |
| Phase 15J-C2 web edge runtime deployment | **COMPLETE** — private core API updated to its reviewed immutable image; `boundarymesh-web` BFF is LIVE with a dedicated keyless identity, service-level core Invoker access, and BFF-only public transport. Unauthenticated, non-allowlisted, dev-route, and mutation-without-session/CSRF requests fail closed. Firebase Hosting has zero releases; authenticated browser session and browser-to-core application path remain deferred to 15J-C3. |
| Phase 15E Secret Manager | **COMPLETE** — API and four containers LIVE; Phase 15I-B2 added one enabled synthetic version per container. Adapter live-verified locally; D1 did not exercise a deployed secret read |
| Phase 15F-A Firestore persistence + preview | **COMPLETE** — adapters in `backend/gcp`; emulator suite passed; preview approved |
| Phase 15F-B Firestore infrastructure apply | **COMPLETE** — `firestore.googleapis.com` LIVE; database `boundarymesh` LIVE / PROVISIONED |
| Phase 15F-C live Java SDK data path | **COMPLETE** — `GoogleFirestoreFacade` + official Java SDK + ADC against `projects/boundarymesh-dev/databases/boundarymesh`. D1 workload persistence LIVE VERIFIED; disposable residue CLEANED |
| Phase 15G-A transactional outbox + Pub/Sub preview | **COMPLETE** — adapters + combined emulator **IMPLEMENTED + EMULATOR TESTED**. Pulumi preview approved |
| Phase 15G-B Pub/Sub infrastructure apply | **COMPLETE** — `pubsub.googleapis.com` LIVE; topics `boundarymesh-deliveries` / `boundarymesh-delivery-dlq` LIVE; pull subscriptions LIVE. No IAM. No `deadLetterPolicy` |
| Phase 15G-C live Pub/Sub data path | **COMPLETE** — `GooglePubSubPublisher` + `GooglePubSubPullConsumer` + official Java SDK + ADC against real topic/subscription. D1 workload persistence LIVE VERIFIED; disposable residue CLEANED |
| Phase 15H-A Cloud KMS signer + preview | **COMPLETE** — `CloudKmsAttestationSigner` **IMPLEMENTED + UNIT TESTED**. Pulumi KMS API/key ring/crypto key **IMPLEMENTED + PREVIEWED**. No IAM |
| Phase 15H-B Cloud KMS infrastructure apply | **COMPLETE** — `cloudkms.googleapis.com` LIVE; key ring `boundarymesh-attestation` LIVE; crypto key `exchange-attestation` LIVE (`ASYMMETRIC_SIGN` / `EC_SIGN_P256_SHA256` / SOFTWARE). No workload IAM. No live sign in 15H-B |
| Phase 15H-C live KMS attestation | **COMPLETE** — official Java SDK + ADC, two real version 1 signs, DER → P1363, public-key verify VALID, altered bytes INVALID, client recreation VALID. Synthetic only; zero cloud document writes. [Evidence](docs/PHASE_15H_VERIFICATION.md) |
| Phase 15I-A workload preview | **IMPLEMENTED + LOCALLY TESTED + PREVIEW-ONLY** — prod composition, two keyless SAs, additive least-privilege IAM, Cloud Run Service + Worker Pool. No apply/deploy |
| Phase 15I-B1 workload identity/IAM apply | **COMPLETE** — two keyless SAs and ten approved additive IAM members LIVE; no keys, secret versions, Run API, Service, or Worker Pool |
| Phase 15I-B2 synthetic secret readiness | **COMPLETE** — exactly one enabled version in each existing integration secret; official Java SDK adapters LIVE VERIFIED LOCALLY; no payload exposed or Pulumi-managed |
| Application production IAM / Entra live | **LIVE VERIFIED LOCALLY** — common authority, personal Microsoft account, server-side session |
| Microsoft Graph live calendar read | **LIVE VERIFIED LOCALLY** — provider 200, one event normalized and enforced |
| GCP project | **CREATED + BILLING LINKED** (`boundarymesh-dev`) |
| GCP resources | Existing 18 resources unchanged. Phase 15I-B1 added 12 managed identity/IAM resources; Phase 15I-C2 added the Run API, private API Service, and Worker Pool. Live verification residue **CLEANED** |
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
6. Quarantine & Recovery Orchestrator — local **TESTED**; Firestore persistence LIVE VERIFIED in D1.

Also: Tamper-Evident Audit Chain **LIVE VERIFIED LOCALLY** for the Graph event; Cryptographic Exchange Attestation **LIVE VERIFIED LOCALLY** (local JCA; Cloud KMS signing **LIVE VERIFIED LOCALLY**); Integration Boundary Graph **LIVE VERIFIED LOCALLY** for `m365-calendar` OBSERVED. Firestore adapters **IMPLEMENTED + EMULATOR TESTED + LIVE VERIFIED LOCALLY**. Firestore database **LIVE / PROVISIONED**. Live verification residue **CLEANED**. Cloud Run workload persistence is **not** live.

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

## Phase 15I-D2 freeze truth

- Cloud Run API and Worker Pool: **LIVE VERIFIED** and Ready.
- Private `POST /api/v1/transactions`: **LIVE VERIFIED** through the internal cloud pipeline.
- Firestore, Cloud KMS signing, Exchange Attestation, audit continuity, transactional outbox, Pub/Sub publish/consume, worker idempotency, and terminal ACK semantics: **LIVE VERIFIED**.
- External connector delivery: **NOT LIVE**; connector configuration remains intentionally fail-closed.
- Deployed workload Secret Manager reads: **NOT EXERCISED BY D1**; infrastructure and IAM remain live.
- Firebase Hosting, WIF, Graph notifications, and production Entra/OIDC: **PLANNED / DISABLED**.
- Pulumi passphrase: **FUNCTIONAL but previously exposed in chat; rotation remains required before the final project security freeze**.

## Phase 15I-D0C worker disposition truth

- Worker terminal disposition semantics are CORRECTED + TESTED + DEPLOYED.
- A terminal QUARANTINED result is durably recorded by the existing idempotency/quarantine persistence path before the transport ACK is returned.
- Persistence failure or an unexpected handler exception does not ACK; it follows the existing retry/NACK path.
- Retryable provider categories (RATE_LIMITED, TEMPORARY_FAILURE, TIMEOUT, CIRCUIT_OPEN) remain NACK/redelivery dispositions.
- Pub/Sub remains at-least-once; existing application idempotency and BoundaryMesh-owned quarantine recovery remain in force.
- The corrected Worker Pool revision is Ready on the D0C immutable digest. D1 verified one live transaction and D2 preserved the audit/attestation evidence.
- Pub/Sub deadLetterPolicy remains unconfigured. Live quarantine ACK behavior was verified in D1.
