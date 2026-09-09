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

## What exists now (Phase 1)

Local Java 25 / Spring Boot 4.1.1 backend with a testable Boundary Enforcement Engine. Synthetic `POST /api/v1/dev/evaluate` only. **Not** Entra, Graph, Camel, GCP, or Vue.

## How to run (Phase 1)

Requires **JDK 25**. No secrets.

```powershell
cd backend
.\mvnw.cmd verify
.\mvnw.cmd -pl api -am spring-boot:run
```

```text
GET  http://localhost:8080/api/v1/health
GET  http://localhost:8080/api/v1/ready
POST http://localhost:8080/api/v1/dev/evaluate
```

The evaluate body must include `"synthetic": true` and all nine controls. See [docs/PHASE1.md](docs/PHASE1.md).

## Current status

**PROJECT C — NOT COMPLETE.** Phase 0 COMPLETE. Phase 1 COMPLETE (local foundation). Nothing is LIVE VERIFIED. No GCP, no Microsoft credentials.

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
| [docs/adr/README.md](docs/adr/README.md) | Architecture Decision Records |

## Phase 0 gate

**PROJECT C ARCHITECTURE READY FOR PHASE 1: YES**

See [docs/PHASE0_GATE.md](docs/PHASE0_GATE.md).
