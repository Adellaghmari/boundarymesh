# Roadmap

Phase 0 is this lock. **Do not execute later phases in Phase 0.** Sequencing is slightly adjusted vs the brief where dependencies require it (worker Camel before cloud adapters; Graph after vault).

| Phase | Name | Outcome |
| --- | --- | --- |
| **0** | Product / security / architecture lock | This documentation. COMPLETE when gate is YES |
| **1** | Java / Spring local foundation | Maven modules, domain types, health endpoints, in-memory adapters, no GCP |
| **2** | Vue recruiter console foundation | Vite app, routes, SCSS visual system, synthetic fixtures, SYNTHETIC DEMO banners |
| **3** | Identity Federation Gateway | **COMPLETE** local JWT/JWKS resource server + AuthN vs AuthZ. Entra / BFF OIDC remain later |
| **4** | Webhook & Subscription Defense | **COMPLETE** QuayForge HMAC, freshness, replay, idempotency. Graph subscriptions remain later |
| **5** | Contract Drift Firewall | OpenAPI 3.1, AsyncAPI, JSON Schema runtime, quarantine on drift |
| **6** | Data Boundary Firewall | versioned policies, public field-path evidence |
| **7** | Boundary Enforcement Engine | ControlVector → EnforcementDecision, decision-table tests, API wiring |
| **8** | Apache Camel routes | **COMPLETE** local worker, EIPs, Resilience4j, WireMock. Pub/Sub PLANNED |
| **9** | Credential & Consent Vault | **COMPLETE** local security model. Secret Manager adapter PLANNED |
| **10** | Quarantine & Recovery | **COMPLETE** local orchestrator. Firestore persistence PLANNED |
| **11** | Tamper-evident audit chain | **COMPLETE** local hash chain + verifier. Firestore PLANNED |
| **12** | Cryptographic Exchange Attestation | **COMPLETE** local JCA P-256. Cloud KMS PLANNED |
| **13** | Integration Boundary Graph | **COMPLETE** state-driven Vue SVG |
| **14** | M365 / Graph connector | **COMPLETE** harness + Entra architecture TESTED. Live tenant BLOCKED. Graph subscriptions PLANNED |
| **15** | GCP + Pulumi cloud foundation | **15A–15B COMPLETE** (Pulumi repo + local preview). 15C+ Artifact Registry and later provision remain |
| **16** | Pulumi / GCP infrastructure | Cloud Run ×2, IAM, KMS, secrets, hosting — **first provision** (not 15A) |
| **17** | GitHub WIF + CI/CD + AppSec | no JSON keys; Semgrep/Gitleaks/ZAP/Dependency-Check |
| **18** | Public deployment + live verification | honest badges; Graph live only if real |
| **19** | Hardening + grand final audit | rotation proof, chain tamper demo, CV matrix update, freeze |

## Adjustments vs original sketch

- Camel (8) before cloud (15): routes must exist locally.
- Graph (14) after vault (9): client secret reference must exist.
- Attestation (12) can use a `Signer` port before real KMS (16).

## Stop conditions per phase

Each phase updates `PROJECT_STATUS.md` and `CV_CLAIMS_MATRIX.md` (`IMPLEMENTED` / `TESTED`). Nothing becomes `LIVE VERIFIED` before Phase 18 evidence.

## Non-goals until a new ADR

Kubernetes, Kafka, Redis, AI, extra Graph APIs, Terraform, GKE, always-on VMs.
