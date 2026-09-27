# Project status

**Project:** BoundaryMesh (Project C)  
**Tagline:** Secure every system crossing.  
**Phase:** **16C CLOSED / PASS / SOURCE-FROZEN** — shared Pulumi state, keyless verification, least-privilege hosted preview, negative isolation proofs, and authoritative documentation are reconciled.
**Overall:** **PROJECT C — PHASE 16C FOUNDATION LIVE VERIFIED AND SOURCE-FROZEN; PHASE 16D/16E/16F AND FINAL SECURITY HYGIENE REMAIN**

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
| Phase 15I-C1 production images | **COMPLETE / HISTORICAL** — the initial API/Worker deployment candidates were built, smoke-tested, pushed, digest-verified, and pinned; later Phase 15I/15J revisions superseded those candidates and are live |
| Phase 15I-C2 Cloud Run deployment | **COMPLETE** — Run API enabled; private API Service and no-endpoint Worker Pool deployed and Ready on immutable images. One internal Firestore/Pub/Sub/KMS workload transaction was LIVE VERIFIED in Phase 15I-D1 |
| Phase 15J-C2 web edge runtime deployment | **COMPLETE / HISTORICAL GATE** — private core API updated to its reviewed immutable image; `boundarymesh-web` BFF became live with a dedicated keyless identity, service-level core Invoker access, and BFF-only public transport. At that gate Hosting still had zero releases and authenticated browser verification was deferred; C3A later completed both. |
| Phase 15J-C3A.1 Hosting cache-policy fix | **COMPLETE** — first Hosting release exposed stale SPA-shell caching at `/` and deep links. A second Hosting-only release applies `no-cache` to every SPA shell request while preserving immutable cache headers for hashed assets. Browser sign-in, authenticated BFF-to-core reads, and browser transactions remain unexecuted. |
| Phase 15J-C3A.4 hosted session diagnostic | **BLOCKED** — three real logins returned 200, each followed by `GET /session/me` returning 401 / `SESSION_INVALID`. Firebase Hosting forwards only the reserved `__session` cookie to Cloud Run, while the live BFF issues `__Host-bm_session`. No transaction was submitted. |
| Phase 15J-C3A.5 session cookie correction | **PASS / HISTORICAL IMPLEMENTATION GATE** — `__session` was implemented consistently for login, reads, and logout with existing cookie security and auth checks. At that gate the immutable image was pushed and previewed but not deployed; C3A.8 later live-verified the hosted session and browser-to-private-core read. |
| Phase 15J-C3A.8 hosted authentication and private-core read | **LIVE VERIFIED / PASS** — Hosting release 3 serves the corrected SPA; Google sign-in, `POST /session/login` 200, `GET /session/me` 200, `__session`, Firebase verification, operator authorization, CSRF, route allowlisting, and keyless BFF invocation of the private core all passed. The retained attestation summary returned 200 through `boundarymesh-web-00004-k48`; BFF duration was 17.284 seconds and core duration 16.561 seconds under the corrected bounded 30-second request timeout. No transaction or application mutation occurred. Final Pulumi preview: 39 unchanged, zero operations. |
| Phase 15J-C3B hosted transaction and cleanup | **LIVE VERIFIED / PASS** — exactly one browser-origin synthetic transaction returned `ALLOW` / `ACCEPTED_FOR_ASYNCHRONOUS_DELIVERY`, produced a SIGNED/VALID KMS-backed attestation, a VALID eight-entry audit chain, one published outbox instruction, one Worker receipt, durable terminal `QUARANTINED` state, one ACK, and zero NACK/redelivery/DLQ. Exact disposable outbox/idempotency records were removed; immutable/shared evidence remains valid. [Final evidence](docs/PHASE_15J_FINAL.md) |
| Phase 16C shared Pulumi state | **LIVE VERIFIED / PASS** — stack `boundarymesh/dev` uses `gs://bm-pulumi-state-eu4-21cad20b325f`; secrets use the dedicated `state-secrets` GCP KMS key in `europe-west4`. Current state is 54 managed resources / 55 records including one stack root. |
| Phase 16C WIF verification identity | **LIVE VERIFIED / PASS** — `boundarymesh-wif-verify` is keyless, environment-scoped to `boundarymesh-dev`, has no project/deployment role and no Token Creator role, and is admitted only by the exact manual main-branch verification workflow context. |
| Phase 16C CI preview identity | **LIVE VERIFIED / PASS** — `boundarymesh-ci-preview` is keyless and environment-scoped to `boundarymesh-ci-preview`; it can only list/get objects in the state bucket and decrypt with the dedicated state KMS key. It has no state write/delete, KMS encrypt, project reader, deployment, application-data, or Token Creator permission. |
| Phase 16C.4 hosted preview and isolation | **CLOSED / PASS / LIVE VERIFIED** — GitHub run `36337930890` at `f19498bd06d484e3295d98de63cdd2211ada2999` authenticated keylessly and returned 54 unchanged / zero operations. Cross-SA impersonation, wrong workflow/environment, and missing-environment contexts were rejected at the intended WIF/IAM boundaries. |
| Phase 16C.5 documentation freeze | **PASS / SOURCE-FROZEN** — roadmap, state, CI/CD, infrastructure, and CV claims are reconciled. The gate made no workflow dispatch, cloud mutation, Pulumi state mutation, publication, or deployment. |
| Phase 16D / 16E / 16F | **PLANNED / NOT IMPLEMENTED** — immutable artifact publication, controlled CD, then live verification and CI/CD freeze, in that order. |
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
| GCP resources | **54 Pulumi-managed resources / 55 state records including stack root / ZERO DRIFT** plus three Firebase Hosting releases outside Pulumi. Private API, Worker Pool, authenticated Web BFF, Firestore, Pub/Sub, KMS, Secret Manager, shared state, WIF, and least-privilege CI preview IAM are live |
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
5. Credential & Consent Vault — local model **TESTED**; Microsoft delegated consent mapped; Secret Manager infrastructure and adapters **LIVE VERIFIED**, while C3B correctly stopped before connector-secret retrieval.
6. Quarantine & Recovery Orchestrator — local **TESTED**; Firestore-backed terminal quarantine persistence and ACK ordering **WORKLOAD LIVE VERIFIED**.

Also: the Tamper-Evident Audit Chain, KMS-backed Cryptographic Exchange Attestation, Firestore persistence, transactional outbox, Pub/Sub publish/consume, Worker idempotency, terminal quarantine, and persist-before-ACK behavior are **WORKLOAD LIVE VERIFIED**. Integration Boundary Graph remains **LIVE VERIFIED LOCALLY** for the Microsoft event. Disposable Phase 15J run residue is cleaned while signed/audit/shared evidence is preserved.

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

## Phase 15I-D2 freeze truth (historical at the Phase 15I gate)

- Cloud Run API and Worker Pool: **LIVE VERIFIED** and Ready.
- Private `POST /api/v1/transactions`: **LIVE VERIFIED** through the internal cloud pipeline.
- Firestore, Cloud KMS signing, Exchange Attestation, audit continuity, transactional outbox, Pub/Sub publish/consume, worker idempotency, and terminal ACK semantics: **LIVE VERIFIED**.
- External connector delivery: **NOT LIVE**; connector configuration remains intentionally fail-closed.
- Deployed workload Secret Manager reads: **NOT EXERCISED BY D1**; infrastructure and IAM remain live.
- Firebase Hosting was still planned at this historical gate and became live in Phase 15J. WIF, Graph notifications, and production Entra/OIDC remain planned or disabled.
- Pulumi passphrase: **FUNCTIONAL but previously exposed in chat; rotation remains required before the final project security freeze**.

## Phase 15I-D0C worker disposition truth

- Worker terminal disposition semantics are CORRECTED + TESTED + DEPLOYED.
- A terminal QUARANTINED result is durably recorded by the existing idempotency/quarantine persistence path before the transport ACK is returned.
- Persistence failure or an unexpected handler exception does not ACK; it follows the existing retry/NACK path.
- Retryable provider categories (RATE_LIMITED, TEMPORARY_FAILURE, TIMEOUT, CIRCUIT_OPEN) remain NACK/redelivery dispositions.
- Pub/Sub remains at-least-once; existing application idempotency and BoundaryMesh-owned quarantine recovery remain in force.
- The corrected Worker Pool revision is Ready on the D0C immutable digest. D1 verified one live transaction and D2 preserved the audit/attestation evidence.
- Pub/Sub deadLetterPolicy remains unconfigured. Live quarantine ACK behavior was verified in D1.

## Phase 15J final freeze truth

- Phase 15J-C3A and Phase 15J-C3B: **LIVE VERIFIED / PASS**.
- Exactly one hosted browser transaction is correlated end to end and preserved in [the final Phase 15J evidence](docs/PHASE_15J_FINAL.md).
- Historical Phase 15J final Pulumi state: **39 unchanged / zero operations / exit 0**. Phase 16C subsequently expanded the managed set to 54 resources.
- External connector delivery remains intentionally disabled; `provider_called=false`, attempts `0`.
- The project is not fully security-frozen: the historically exposed Pulumi passphrase and locally exposed, uncommitted Firebase password-hash signing material remain accepted follow-ups.

## Phase 16C final freeze truth

- Phase 16C.4 is **CLOSED / PASS / LIVE VERIFIED**; Phase 16C.5 reconciles and source-freezes its documentation.
- Current Pulumi backend: `gs://bm-pulumi-state-eu4-21cad20b325f`; current secrets provider: `gcpkms://projects/boundarymesh-dev/locations/europe-west4/keyRings/boundarymesh-pulumi/cryptoKeys/state-secrets`.
- Current stack: **54 managed resources / 55 records including one stack root / zero drift**.
- The WIF provider admits exactly two manual, private-repository, main-branch contexts: `wif-verify.yml` with `boundarymesh-dev`, and `infra-preview.yml` with `boundarymesh-ci-preview`.
- The verification and preview service accounts are separate, keyless, environment-scoped, have no direct project roles, and have no Token Creator role. The preview identity can only read state objects and decrypt the dedicated state key.
- Positive preview and bounded negative trust tests are live verified. They do not prove artifact publication or deployment permissions.
- Compute Engine API remains intentionally **DISABLED**. Its regions warning during Pulumi preview is accepted and non-fatal; it does not authorize enabling the API.
- Immutable publication, controlled CD, and live CI/CD freeze remain Phase 16D, 16E, and 16F. Final credential/security hygiene remains unresolved and must not be described as complete.
