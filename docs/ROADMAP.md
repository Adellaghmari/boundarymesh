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
| **10** | Quarantine & Recovery | **COMPLETE** local orchestrator. Firestore adapter **IMPLEMENTED + EMULATOR TESTED + LIVE VERIFIED LOCALLY** |
| **11** | Tamper-evident audit chain | **COMPLETE** local hash chain + verifier. Firestore transactional append **IMPLEMENTED + EMULATOR TESTED + LIVE VERIFIED LOCALLY** |
| **12** | Cryptographic Exchange Attestation | **COMPLETE** local JCA P-256. Cloud KMS adapter **IMPLEMENTED + UNIT TESTED**; live KMS LIVE VERIFIED LOCALLY |
| **13** | Integration Boundary Graph | **COMPLETE** state-driven Vue SVG |
| **14** | M365 / Graph connector | **COMPLETE** harness + Entra architecture TESTED. Live tenant BLOCKED. Graph subscriptions PLANNED |
| **15** | GCP + Pulumi cloud foundation and hosted verification | **COMPLETE / LIVE VERIFIED.** Phase 15I deployed and verified the private core/Worker path. Phase 15J deployed Firebase Hosting and the secure Web BFF, then verified exactly one authenticated browser-origin transaction through KMS evidence, audit, outbox, Pub/Sub, durable Worker quarantine, and ACK. Final Pulumi state: 39 unchanged / zero operations |
| **16** | Remaining Pulumi / GCP infrastructure | Reconcile remaining infrastructure roadmap items and state-backend ownership. Firebase Hosting is already LIVE outside Pulumi; WIF remains Phase 17 |
| **17** | GitHub WIF + CI/CD + AppSec | no JSON keys; Semgrep/Gitleaks/ZAP/Dependency-Check |
| **18** | Public deployment + live verification | honest badges; Graph live only if real |
| **19** | Hardening + grand final audit | rotation proof, chain tamper demo, CV matrix update, freeze |

## Adjustments vs original sketch

- Camel (8) before cloud (15): routes must exist locally.
- Graph (14) after vault (9): client secret reference must exist.
- Attestation (12) uses a cloud-neutral signer port. Real KMS was provisioned in 15H-B and live-verified in 15H-C; local JCA remains the default.

## Stop conditions per phase

Each phase updates `PROJECT_STATUS.md` and `CV_CLAIMS_MATRIX.md`. A claim becomes `LIVE VERIFIED` only when a dated phase evidence record proves the real deployed path; Phase 15J is such a record.

## Non-goals until a new ADR

Kubernetes, Kafka, Redis, AI, extra Graph APIs, Terraform, GKE, always-on VMs.
