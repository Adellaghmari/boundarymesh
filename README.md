# BoundaryMesh

**Secure every system crossing.**

A successful HTTP request does **not** prove that one system should be allowed to exchange data or actions with another system.

BoundaryMesh is a **cross-system security enforcement layer**. It sits in front of system-to-system integrations and answers one question:

> Can this transaction cross this security boundary?

It is **not** an API dashboard, an iPaaS clone, an AI copilot, or a release-intelligence product.

## Product in one paragraph

An untrusted system transaction arrives. BoundaryMesh authenticates identity, authorizes the specific action, verifies message authenticity, distinguishes malicious replay from legitimate duplicate delivery, validates the integration contract, governs which fields may leave the source boundary, then **enforces** a deterministic outcome: `ALLOW`, `ALLOW_WITH_TRANSFORMATION`, `QUARANTINE`, or `REJECT`. Allowed work is delivered asynchronously through Apache Camel connector routes. Every enforcement produces a **Cryptographic Exchange Attestation** and a tamper-evident audit chain entry.

## Flagship capabilities

1. **Identity Federation Gateway** — OAuth 2.0 / OIDC / JWT / JWKS, including Microsoft Entra ID
2. **Webhook & Subscription Defense** — HMAC and provider-specific Graph notification security
3. **Contract Drift Firewall** — versioned OpenAPI / AsyncAPI / JSON Schema as runtime enforcement
4. **Data Boundary Firewall** — destination-specific allow / redact / deny, no AI PII guessing
5. **Credential & Consent Vault** — Secret Manager references, never plaintext secrets in data stores
6. **Quarantine & Recovery Orchestrator** — bounded retry, circuit breaker, safe reprocess

Core systems: **Boundary Enforcement Engine**, **Cryptographic Exchange Attestation**, **Integration Boundary Graph**.

## Stack (locked)

| Layer | Choice | Locked version |
| --- | --- | --- |
| Frontend | Vue 3 + TypeScript + Pinia + Vue Router + SCSS + Vite | Vue 3.5.x, Pinia 4.x, Vue Router 5.x, Vite 8.x |
| Backend | Java + Spring Boot + Spring Security | **Java 25 LTS**, **Spring Boot 4.1.1** |
| Integration | Apache Camel | **4.22.0 LTS** (Java 17/21/25) |
| Enterprise connector | Microsoft Graph + Microsoft Entra ID | Graph Java SDK 6.66.x |
| Cloud | Google Cloud (Cloud Run, Pub/Sub, Firestore, Secret Manager, Cloud KMS) | Region `europe-west4` |
| IaC | Pulumi + TypeScript | GCS DIY state backend |
| CI | GitHub Actions + Workload Identity Federation | No JSON keys |

Exact pins: [docs/STACK.md](docs/STACK.md).

## What this is not

Do not describe BoundaryMesh as professional security employment, paid Microsoft 365 consulting, enterprise production ownership, or years of IAM / Java experience. This is **portfolio engineering** by Adel Laghmari.

Do not modify **Project A** (AI Operations Copilot) or **Project B** (CloudOps Release Intelligence). Both are frozen.

## What exists now (Phases 0–7)

Local Java 25 / Spring Boot 4.1.1 / Spring Security resource server with a testable Boundary Enforcement Engine, plus a Vue 3 recruiter console.

Phase 3 proves **local** JWT/JWKS identity federation (not Entra, not interactive OIDC login).  
Phase 4 proves **QuayForge** HMAC webhook defense, freshness, replay, and duplicate delivery (not Graph subscriptions).  
Phase 5 proves **Contract Drift Firewall**: versioned OpenAPI 3.1 / AsyncAPI / JSON Schema artifacts and runtime payload validation.  
Phase 6 proves **Data Boundary Firewall**: destination-specific ALLOW / REDACT / DENY with a real transformed outbound payload.  
Phase 7 composes those controls in the Boundary Enforcement Engine. All four outcomes are produced by implemented mechanisms.

A valid JWT, scope, HMAC, freshness window, and unused nonce still do **not** automatically allow a crossing.

Synthetic evaluate and composed crossing are JWT-protected (`boundary.evaluate`). Labs call the real Java backend. Boundary events are browser-session memory. **Not** Entra, Graph, Camel, GCP, or a public deploy.

Still **PLANNED:** Entra, Microsoft Graph, Apache Camel, GCP, Firestore, Pub/Sub, Secret Manager, KMS, Pulumi, Credential & Consent Vault, Quarantine & Recovery Orchestrator beyond the QUARANTINE outcome.

## How to run (Phases 3–7)

Requires **JDK 25** and **Node 22**. No secrets. `VITE_*` values are public browser configuration.

```powershell
cd backend
.\mvnw.cmd verify
.\mvnw.cmd -pl api -am spring-boot:run
```

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
GET       http://localhost:8080/api/v1/health
GET       http://localhost:8080/api/v1/ready
GET       http://localhost:8080/api/v1/dev/jwks
POST      http://localhost:8080/api/v1/dev/tokens
POST      http://localhost:8080/api/v1/identity/evaluate
POST      http://localhost:8080/api/v1/webhooks/quayforge
POST      http://localhost:8080/api/v1/dev/contract-lab/{scenario}
POST      http://localhost:8080/api/v1/dev/egress-lab/{destination}/{scenario}
POST      http://localhost:8080/api/v1/dev/crossings/evaluate
```

`POST /api/v1/dev/evaluate` and `POST /api/v1/dev/crossings/evaluate` require a local bearer JWT with `boundary.evaluate`. Labs mint or sign on the Java side — do not paste tokens or HMAC secrets into the UI.

See [docs/PHASE5.md](docs/PHASE5.md), [docs/PHASE6.md](docs/PHASE6.md), and [docs/PHASE7.md](docs/PHASE7.md).

Playwright (`npm run test:e2e` in `frontend/`) starts the Java backend and the Vite app.

## Current status

**PROJECT C — NOT COMPLETE.** Phase 0–7 COMPLETE locally. Identity #1 is local only (Entra future). Webhook #2 is generic HMAC (Graph subscriptions future). Contract Drift #3 TESTED. Data Boundary #4 TESTED. Credential Vault #5 PLANNED. Quarantine Orchestrator #6 PLANNED beyond the QUARANTINE outcome. Nothing is LIVE VERIFIED. No GCP, no Microsoft credentials.

See [PROJECT_STATUS.md](PROJECT_STATUS.md) and [CV_CLAIMS_MATRIX.md](CV_CLAIMS_MATRIX.md).

## Documentation

| Document | Purpose |
| --- | --- |
| [docs/PRODUCT.md](docs/PRODUCT.md) | Product definition, thesis, synthetic org, recruiter story |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | System design, service boundaries, coherence |
| [docs/BOUNDARY_ENFORCEMENT_ENGINE.md](docs/BOUNDARY_ENFORCEMENT_ENGINE.md) | Deterministic enforcement |
| [docs/SECURITY_MODEL.md](docs/SECURITY_MODEL.md) | Trust boundaries, public vs protected |
| [docs/IDENTITY_MODEL.md](docs/IDENTITY_MODEL.md) | AuthN vs AuthZ, Entra, machine identity |
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
| [docs/ROADMAP.md](docs/ROADMAP.md) | Phases 1–19 |
| [docs/QUARANTINE.md](docs/QUARANTINE.md) | Recovery orchestrator |
| [docs/BOUNDARY_GRAPH.md](docs/BOUNDARY_GRAPH.md) | Visual system |
| [docs/NETWORK_BOUNDARIES.md](docs/NETWORK_BOUNDARIES.md) | Trust hops |
| [docs/PULUMI.md](docs/PULUMI.md) | IaC |
| [docs/CI_CD.md](docs/CI_CD.md) | GitHub WIF |
| [docs/PHASE0_GATE.md](docs/PHASE0_GATE.md) | Final YES/NO gate |
| [docs/PHASE1.md](docs/PHASE1.md) | Phase 1 implementation record |
| [docs/PHASE2.md](docs/PHASE2.md) | Phase 2 Vue console record |
| [docs/PHASE3.md](docs/PHASE3.md) | Phase 3 local identity gateway |
| [docs/PHASE4.md](docs/PHASE4.md) | Phase 4 QuayForge webhook defense |
| [docs/adr/README.md](docs/adr/README.md) | Architecture Decision Records |

## Phase 0 gate

**PROJECT C ARCHITECTURE READY FOR PHASE 1: YES**

See [docs/PHASE0_GATE.md](docs/PHASE0_GATE.md).
