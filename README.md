# BoundaryMesh

**Secure every system crossing.**

A successful HTTP request does **not** prove that one system should be allowed to exchange data or actions with another system.

BoundaryMesh is a **cross-system security enforcement layer**. It sits in front of system-to-system integrations and answers one question:

> Can this transaction cross this security boundary?

It is **not** an API dashboard, an iPaaS clone, an AI copilot, or a release-intelligence product.

## Product in one paragraph

An untrusted system transaction arrives. BoundaryMesh authenticates identity, authorizes the specific action, verifies message authenticity, distinguishes malicious replay from legitimate duplicate delivery, validates the integration contract, governs which fields may leave the source boundary, then **enforces** a deterministic outcome: `ALLOW`, `ALLOW_WITH_TRANSFORMATION`, `QUARANTINE`, or `REJECT`. Allowed work becomes a `DeliveryInstruction`, passes credential/consent checks, and is routed by Apache Camel with Resilience4j around the connector. Recoverable failures become quarantine items that an authorized operator can reprocess. Each terminal crossing writes a tamper-evident audit chain and a local ECDSA P-256 Cryptographic Exchange Attestation. An Integration Boundary Graph renders configured and observed crossings from application state.

## Flagship capabilities

1. **Identity Federation Gateway** — OAuth 2.0 / OIDC / JWT / JWKS, including Microsoft Entra ID
2. **Webhook & Subscription Defense** — HMAC and provider-specific Graph notification security
3. **Contract Drift Firewall** — versioned OpenAPI / AsyncAPI / JSON Schema as runtime enforcement
4. **Data Boundary Firewall** — destination-specific allow / redact / deny, no AI PII guessing
5. **Credential & Consent Vault** — local reference/version/consent model; Secret Manager containers live; hosted connector secret read not exercised
6. **Quarantine & Recovery Orchestrator** — local workflow plus Firestore-backed terminal quarantine **LIVE VERIFIED** on the hosted Worker path

Also implemented: **Tamper-Evident Audit Chain**, **Cryptographic Exchange Attestation** (local JCA; Cloud KMS signing LIVE VERIFIED on the hosted path), **Integration Boundary Graph**.

## Stack (locked)

| Layer | Choice | Locked version |
| --- | --- | --- |
| Frontend | Vue 3 + TypeScript + Pinia + Vue Router + SCSS + Vite | Vue 3.5.x, Pinia 4.x, Vue Router 5.x, Vite 8.x |
| Backend | Java + Spring Boot + Spring Security | **Java 25 LTS**, **Spring Boot 4.1.1** |
| Integration | Apache Camel | **4.22.0 LTS** (Java 17/21/25) |
| Enterprise connector | Microsoft Graph + Microsoft Entra ID | Graph Java SDK **6.67.0** |
| Cloud | Google Cloud (Cloud Run, Pub/Sub, Firestore, Secret Manager, Cloud KMS) | Region `europe-west4` |
| IaC | Pulumi + TypeScript | GCS DIY state backend |
| CI | GitHub Actions + Workload Identity Federation | No JSON keys |

Exact pins: [docs/STACK.md](docs/STACK.md).

## What this is not

Do not describe BoundaryMesh as professional security employment, paid Microsoft 365 consulting, enterprise production ownership, or years of IAM / Java experience. This is **portfolio engineering** by Adel Laghmari.

Do not modify **Project A** (AI Operations Copilot) or **Project B** (CloudOps Release Intelligence). Both are frozen.

## What the local labs prove

Local Java 25 / Spring Boot 4.1.1 / Spring Security resource server, a separate Camel delivery worker, and a Vue 3 recruiter console with a premium product header. This is the developer loop. Hosted production is separate; see **Current status**.

Phase 3 proves **local** JWT/JWKS identity federation.  
Phase 4 proves **QuayForge** HMAC webhook defense (not Graph subscriptions).  
Phase 5–7 prove contract drift, data-boundary transformation, and the composed engine.  
Phase 8 delivers approved crossings through **Apache Camel 4.22.0** and **Resilience4j 2.4.0** against WireMock providers.  
Phase 9 adds a **local credential/consent boundary** (not Secret Manager).  
Phase 10 makes quarantine a real **recovery workflow** with admin-scoped reprocess (not Firestore).  
Phase 11 adds a **SHA-256 tamper-evident audit chain** (in-memory, not immutable).  
Phase 12 signs a **Cryptographic Exchange Attestation** with local ECDSA P-256 (Cloud KMS adapter **IMPLEMENTED + UNIT TESTED**; live KMS **LIVE VERIFIED LOCALLY**).
Phase 13 renders an **Integration Boundary Graph** from live local repositories (not a blast-radius graph).  
Phase 14 adds a **premium navigation shell** plus **Microsoft Entra OIDC BFF** and a **bounded Outlook Calendar Graph connector**. Local live verification: delegated `Calendars.Read` with a personal Microsoft account; Graph HTTP 200 with one event normalized to `m365-calendar/v1` and enforced by `m365-calendar-egress/v1` (`ALLOW_WITH_TRANSFORMATION`). Helix 365 remains SYNTHETIC DEMO. Graph change notifications remain **PLANNED**.

### One-minute story

A QuayForge order arrives. Identity, authorization, authenticity, freshness, and replay **PASS**. The contract is **VALID**. The Data Boundary Firewall redacts governed fields. The engine emits **ALLOW_WITH_TRANSFORMATION**. Credential version 2 is resolvable. Consent is granted. The connector circuit is **CLOSED**. Camel routes to Lumenway or CoveCRM. The provider receives the transformed body. Result: **DELIVERED**. The audit chain verifies. The exchange attestation signature verifies. The graph edge is **ACTIVE**.

Then contract drift **QUARANTINES** the Rivertide edge, or replay **REJECTS** CoveCRM with no delivery. The graph reflects the actual outcome.

A valid JWT still does **not** automatically allow a crossing. An attestation does **not** prove business data is true.

The hosted path is Firebase Hosting → Web BFF → IAM-private API → Firestore/KMS/outbox → Pub/Sub → Worker Pool. One synthetic browser-origin transaction was live-verified through that internal path. External connector delivery remains intentionally disabled. Graph change notifications and production Entra/OIDC remain disabled. Dead-letter forwarding is not configured. Evidence records: [Phase 15J](docs/PHASE_15J_FINAL.md), [16C](docs/PHASE_16C_FINAL.md), [16D](docs/PHASE_16D_FINAL.md) (historical publication freeze; the release was later promoted).

## Navigation

Desktop: Core (Overview, Boundary Events, Connections, Architecture, Boundary Graph, Full Crossing) | Labs dropdown | Operations dropdown | Evidence dropdown | Status | Recruiter/operator control.

Labs, Operations, and Evidence are grouped so the header stays a product shell instead of a crowded admin menu.

## How to run locally

Requires **JDK 25** and **Node 22**. No secrets. `VITE_*` values are public browser configuration.

```powershell
cd backend
.\mvnw.cmd verify
.\mvnw.cmd -pl worker -am spring-boot:run
.\mvnw.cmd -pl api -am spring-boot:run
```

`scripts/start-backend.ps1` starts the worker (8081) then the API (8080).

```powershell
cd frontend
npm install
npm run typecheck
npm run lint
npm test
npm run build
npm run dev
```

Full-stack helper: `scripts/dev-local.ps1`.

```text
Frontend  http://localhost:5173
API       http://localhost:8080
Worker    http://localhost:8081
GET       /api/v1/health
GET       /api/v1/ready
POST      /api/v1/dev/delivery-lab/{scenario}
GET       /api/v1/dev/credential-lab
GET       /api/v1/quarantine
POST      /api/v1/quarantine/{id}/reprocess   (boundary.admin)
GET       /api/v1/audit/chains
GET       /api/v1/audit/chains/{integrationId}/verify
GET       /api/v1/attestations
POST      /api/v1/attestations/{id}/verify
GET       /api/v1/microsoft/connector
POST      /api/v1/dev/microsoft-lab/harness-crossing
GET       /api/v1/admin/session
```

`POST /api/v1/dev/evaluate` and `POST /api/v1/dev/crossings/evaluate` require a local bearer JWT with `boundary.evaluate`. Labs mint or sign on the Java side — do not paste tokens or HMAC secrets into the UI.

See [docs/PHASE5.md](docs/PHASE5.md), [docs/PHASE6.md](docs/PHASE6.md), and [docs/PHASE7.md](docs/PHASE7.md).

Playwright (`npm run test:e2e` in `frontend/`) starts the Java backend and the Vite app.

## Current status

BoundaryMesh is **live** in the portfolio/dev GCP project `boundarymesh-dev` (`europe-west4`). This is not enterprise production and not paid customer traffic.

**Topology:** Firebase Hosting → authenticated Web BFF → IAM-private API → Firestore / KMS / outbox → Pub/Sub → Cloud Run Worker Pool.

**Infrastructure:** Pulumi `dev` stack, **83 managed / zero drift**, GCS DIY state + KMS secrets. Compute Engine API is **disabled**. GitHub Actions deploys through Workload Identity Federation (**7 exact** main-only contexts). CI/CD and workload service accounts have **zero user-managed keys**.

**Delivery:** the frozen 16D release is **promoted**. Promotion evidence is **immutable**. Rollback is **implemented and not live-executed**. Evidence recovery is **break-glass**, not the happy path. SBOM/provenance are **published and unsigned**.

**Not live:** external connectors, production Entra/OIDC, Graph notifications, DLQ forwarding, OpenTelemetry, AppSec CI scanners.

Deeper truth: [PROJECT_STATUS.md](PROJECT_STATUS.md), [CV_CLAIMS_MATRIX.md](CV_CLAIMS_MATRIX.md), [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md), [docs/CI_CD.md](docs/CI_CD.md), [docs/README.md](docs/README.md).

## Documentation

Start with [docs/README.md](docs/README.md). `docs/PHASE_*.md` files are historical gate records.

| Document | Purpose |
| --- | --- |
| [docs/README.md](docs/README.md) | Current vs historical documentation map |
| [docs/PRODUCT.md](docs/PRODUCT.md) | Product definition, thesis, synthetic org, recruiter story |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | System design, service boundaries, coherence |
| [docs/BOUNDARY_ENFORCEMENT_ENGINE.md](docs/BOUNDARY_ENFORCEMENT_ENGINE.md) | Deterministic enforcement |
| [docs/SECURITY_MODEL.md](docs/SECURITY_MODEL.md) | Trust boundaries, public vs protected |
| [docs/IDENTITY_MODEL.md](docs/IDENTITY_MODEL.md) | AuthN vs AuthZ, Entra, machine identity |
| [docs/MICROSOFT_ENTRA_SETUP.md](docs/MICROSOFT_ENTRA_SETUP.md) | App registration, redirect URI, env names |
| [docs/M365_GRAPH_INTEGRATION.md](docs/M365_GRAPH_INTEGRATION.md) | Calendar connector, live vs harness |
| [docs/APACHE_CAMEL_ROUTES.md](docs/APACHE_CAMEL_ROUTES.md) | EIP usage after enforcement |
| [docs/WEBHOOK_SECURITY.md](docs/WEBHOOK_SECURITY.md) | HMAC vs Graph notifications |
| [docs/CONTRACT_MODEL.md](docs/CONTRACT_MODEL.md) | OpenAPI, AsyncAPI, JSON Schema |
| [docs/DATA_BOUNDARY_FIREWALL.md](docs/DATA_BOUNDARY_FIREWALL.md) | Egress allow / redact / deny |
| [docs/SECRET_MODEL.md](docs/SECRET_MODEL.md) | Credential vault |
| [docs/EXCHANGE_ATTESTATIONS.md](docs/EXCHANGE_ATTESTATIONS.md) | Signed enforcement proof |
| [docs/AUDIT_CHAIN.md](docs/AUDIT_CHAIN.md) | Tamper-evident hash chain |
| [docs/THREAT_MODEL.md](docs/THREAT_MODEL.md) | STRIDE |
| [docs/FAILURE_MODEL.md](docs/FAILURE_MODEL.md) | Fail closed / retry / quarantine |
| [docs/DATA_MODEL.md](docs/DATA_MODEL.md) | Firestore access patterns |
| [docs/API_CONTRACTS.md](docs/API_CONTRACTS.md) | Public and protected APIs |
| [docs/COST_MODEL.md](docs/COST_MODEL.md) | Portfolio cost before any provision |
| [docs/TEST_STRATEGY.md](docs/TEST_STRATEGY.md) | Test pyramid |
| [docs/ROADMAP.md](docs/ROADMAP.md) | Completed / current 16F / optional follow-ups |
| [docs/QUARANTINE.md](docs/QUARANTINE.md) | Recovery orchestrator |
| [docs/BOUNDARY_GRAPH.md](docs/BOUNDARY_GRAPH.md) | Visual system |
| [docs/NETWORK_BOUNDARIES.md](docs/NETWORK_BOUNDARIES.md) | Trust hops |
| [docs/PULUMI.md](docs/PULUMI.md) | IaC |
| [docs/GCP_ADAPTERS.md](docs/GCP_ADAPTERS.md) | GCP module boundary and port map |
| [docs/PHASE_15J_FINAL.md](docs/PHASE_15J_FINAL.md) | Final hosted production verification and cleanup freeze |
| [docs/PHASE_16C_FINAL.md](docs/PHASE_16C_FINAL.md) | Shared-state and keyless CI-preview live evidence |
| [docs/PHASE_16D_FINAL.md](docs/PHASE_16D_FINAL.md) | Historical publication freeze (release later promoted) |
| [docs/CI_CD.md](docs/CI_CD.md) | WIF, publication, stage, promote, rollback, break-glass recovery |
| [docs/PHASE0_GATE.md](docs/PHASE0_GATE.md) | Final YES/NO gate |
| [docs/PHASE1.md](docs/PHASE1.md) | Phase 1 implementation record |
| [docs/PHASE2.md](docs/PHASE2.md) | Phase 2 Vue console record |
| [docs/PHASE3.md](docs/PHASE3.md) | Phase 3 local identity gateway |
| [docs/DELIVERY_MODEL.md](docs/DELIVERY_MODEL.md) | DeliveryInstruction and DeliveryPort |
| [docs/CREDENTIAL_MODEL.md](docs/CREDENTIAL_MODEL.md) | References, rotation, consent |
| [docs/QUARANTINE_RECOVERY.md](docs/QUARANTINE_RECOVERY.md) | Local orchestrator |
| [docs/PHASE8.md](docs/PHASE8.md) | Camel worker delivery |
| [docs/PHASE9.md](docs/PHASE9.md) | Credential and consent boundary |
| [docs/PHASE10.md](docs/PHASE10.md) | Quarantine and recovery |
| [docs/PHASE4.md](docs/PHASE4.md) | Phase 4 QuayForge webhook defense |
| [docs/APPLICATION_SECURITY.md](docs/APPLICATION_SECURITY.md) | Implemented controls and explicit gaps |
| [docs/OBSERVABILITY.md](docs/OBSERVABILITY.md) | Logs and health; no OpenTelemetry claim |
| [docs/adr/README.md](docs/adr/README.md) | Architecture Decision Records |

## Phase 0 gate

**PROJECT C ARCHITECTURE READY FOR PHASE 1: YES**

See [docs/PHASE0_GATE.md](docs/PHASE0_GATE.md).
