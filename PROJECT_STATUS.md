# Project status

**Project:** BoundaryMesh (Project C)

**Tagline:** Secure every system crossing.

**Environment:** GCP project `boundarymesh-dev`, region `europe-west4` — portfolio/dev production, not enterprise production and not paid customer traffic.

**Overall:** live in the portfolio/dev GCP environment. The frozen published release is **staged, promoted, and live**. Promotion evidence is **immutable**. Rollback is **ready and not executed**. Reviewer documentation is frozen to this reality.

Current truth for reviewers is the root [README.md](README.md), this file, [CV_CLAIMS_MATRIX.md](CV_CLAIMS_MATRIX.md), and [docs/README.md](docs/README.md). Files named `docs/PHASE_*.md` are historical gate records. You do not need phase numbering to understand the product.

## Current live topology

Firebase Hosting (`boundarymesh-dev.web.app`) serves the SPA and rewrites session/API paths to the public Web BFF. The BFF is the only internet-facing application process. The core API is IAM-private. The worker is a no-ingress Cloud Run Worker Pool that pulls Pub/Sub.

| Surface | Current identity | Status |
| --- | --- | --- |
| API | `boundarymesh-api-c-36941014905` 100% | Ready |
| Web BFF | `boundarymesh-web-c-36941014905` 100% | Ready |
| Worker | `boundarymesh-worker-c-36941014905` 100% | Ready |
| Firebase Hosting | version `c9def09b61688b16` on channel `live` | FINALIZED |
| Pulumi stack `dev` | **83 managed / 83 unchanged / 0 operations** | Compute Engine API **DISABLED** |
| WIF provider `github-actions` | **7 exact** main-only `workflow_dispatch` contexts | ACTIVE |
| Frozen release source | `a876a7bfa5fa86f88e8387efb557773281daa6e6` | published, then deployed without rebuild |
| Promotion evidence | Generic package version bound to the original promotion run | PROMOTED / immutable |
| Rollback identities | API `00004-9hw`, Web `00004-k48`, Worker `00002-v6j`, Hosting `1bce16129fb1a0d4` | live-readable, **not executed** |
| Evidence recovery | `recover-promotion-evidence.yml` | live-verified **break-glass**, not the happy path |

External connector delivery remains **intentionally disabled**. Production Entra/OIDC remains **disabled**. Graph change notifications remain **not implemented**. Dead-letter forwarding remains **not configured**.

## Completeness (current)

| Item | Status |
| --- | --- |
| Local enforcement engine (identity, HMAC, contract, egress, Camel, quarantine, audit, attestation) | **COMPLETE / TESTED**; Graph calendar pull **LIVE VERIFIED LOCALLY** |
| Hosted private API + Worker + Firestore + Pub/Sub + KMS | **LIVE VERIFIED** (Phase 15J one synthetic hosted transaction) |
| Firebase Hosting + authenticated Web BFF | **LIVE VERIFIED** |
| Shared Pulumi GCS + KMS state | **LIVE VERIFIED** |
| Immutable artifact publication | **LIVE VERIFIED** (unsigned SBOM/provenance; no signing claim) |
| Controlled CD (stage / promote) | **LIVE VERIFIED**; consumes the frozen 16D release only |
| Promotion-evidence recovery | **LIVE VERIFIED** as break-glass |
| Rollback workflow | **IMPLEMENTED / TESTED / LIVE-READABLE / NOT LIVE-EXECUTED** |
| Application PR CI on GitHub | **NOT IMPLEMENTED** (local Maven/Vitest/Playwright) |
| AppSec automation (Semgrep, Gitleaks, Dependabot, ZAP) | **OPTIONAL / NOT REQUIRED** for current CV readiness |
| Project A / Project B | **FROZEN** — not modified |
| GCP service-account JSON keys for CI/CD/workloads | **NONE** (SYSTEM_MANAGED keys only) |

## ControlVector audit

| Control | Implemented | Tested | Real or synthetic |
| --- | --- | --- | --- |
| identity | yes | yes | real local JWT; Operator Entra OIDC LIVE VERIFIED LOCALLY (personal Microsoft account). Hosted operator uses Firebase Google + BFF session |
| authorization | yes | yes | real scopes including `SCOPE_Calendars.Read` on admin chain |
| authenticity | yes | yes | real HMAC |
| freshness | yes | yes | real timestamp window |
| replay | yes | yes | real nonce store |
| contract | yes | yes | real schema/registry; live Graph event `m365-calendar/v1` PASS |
| egress | yes | yes | live Graph event `m365-calendar-egress/v1` PASS, `EGRESS_REDACTION_REQUIRED` |
| credential resolvability | yes | yes | real local catalog/evaluator; Secret Manager IAM live, connector secret not read on the hosted path |
| connector safety | yes | yes | real circuit/config snapshot; hosted path used an empty endpoint |

## Six flagships

1. Identity Federation Gateway — local JWT **TESTED**; hosted Firebase operator session **LIVE VERIFIED**; Entra OIDC BFF **LIVE VERIFIED LOCALLY**.
2. Webhook & Subscription Defense — QuayForge HMAC **TESTED**; Graph subscriptions **NOT IMPLEMENTED**.
3. Contract Drift Firewall — **TESTED**.
4. Data Boundary Firewall — **TESTED**; `m365-calendar-egress/v1` **LIVE VERIFIED LOCALLY**.
5. Credential & Consent Vault — local model **TESTED**; Secret Manager containers **LIVE**; hosted Worker stopped before connector-secret retrieval.
6. Quarantine & Recovery Orchestrator — local **TESTED**; Firestore-backed terminal quarantine and ACK **WORKLOAD LIVE VERIFIED**.

## Intentionally not claimed

- Enterprise production ownership, paid traffic, or a public recruiter demo product.
- Live rollback success (the path exists; it has not been dispatched).
- Signed software supply chain / Cosign.
- OpenTelemetry or Cloud Trace.
- Complete credential hygiene (historical Pulumi passphrase exposure remains an accepted follow-up; current stack secrets use KMS).
- Project-wide “no Token Creator anywhere” (a Firebase-managed identity holds that role; CI/CD and workload SAs do not).
- Operator workstation access is keyless (workstation ADC exists).

Optional follow-ups are listed once in [docs/ROADMAP.md](docs/ROADMAP.md). They are not CV-readiness blockers.

## LIVE MICROSOFT VERIFICATION (local, 2026-09-10)

Human-controlled Operator browser completed `POST /api/v1/admin/microsoft/calendar/pull` twice in this process.

Empty-calendar pull: Graph HTTP 200, `count=0` (provider verified only).

Event pull: Graph HTTP 200, `mode=LIVE_VERIFIED`, `live=true`, `permission=Calendars.Read`, `provider_status=200`, `count=1`.

Safe recorded result for the event pull:

- path: Microsoft Graph → `MicrosoftGraphCalendarAdapter` → `m365-calendar/v1` → contract PASS → `m365-calendar-egress/v1` → engine `ALLOW_WITH_TRANSFORMATION` (`EGRESS_REDACTION_REQUIRED`) → audit → SIGNED local JCA attestation (verify VALID) → graph OBSERVED/ACTIVE
- audit chain `m365-calendar` continuity VALID: `CONNECTOR_READ` count=1, `BOUNDARY_DECISION`, `DELIVERY_RESULT` NOT_ATTEMPTED, `ATTESTATION_CREATED` SIGNED
- applicable controls PASS; authenticity/freshness/replay NOT_APPLICABLE; transformation_count=4 (subject REDACT + attendees/body/join URL DENY)
- live adapter selects only id/start/end/showAs from Graph
- Helix 365 remains SYNTHETIC DEMO
- not an enterprise Exchange Online mailbox
- Graph change notifications remain **NOT IMPLEMENTED**
- production hosted Entra/OIDC remains **disabled**

## Historical checkpoints (not current counts)

These records are true **as of the named gate**. Later gates increased WIF contexts, managed-resource counts, and deployment state.

- **15I / 15J:** private API, Worker, Hosting, BFF, and one hosted synthetic transaction. Historical Pulumi freeze: 39 unchanged. External connectors disabled.
- **16C:** GCS/KMS Pulumi backend; two WIF contexts; historical freeze **54 managed**.
- **16D:** immutable publication of source `a876a7bfa5fa86f88e8387efb557773281daa6e6`; historical freeze **59 managed**; publication did not deploy.
- **16E.6:** controlled stage + promote of that release; promotion evidence recovered without re-promotion; current freeze **83 managed / 7 WIF contexts**.

Detailed IDs live in `docs/PHASE_*.md`.
