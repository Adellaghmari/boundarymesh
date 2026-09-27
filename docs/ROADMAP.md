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
| **15** | GCP + Pulumi cloud foundation and hosted verification | **COMPLETE / LIVE VERIFIED.** Phase 15I deployed and verified the private core/Worker path. Phase 15J deployed Firebase Hosting and the secure Web BFF, then verified exactly one authenticated browser-origin transaction through KMS evidence, audit, outbox, Pub/Sub, durable Worker quarantine, and ACK. Historical Phase 15J freeze: 39 unchanged / zero operations |
| **16C** | Shared state and keyless CI preview | **CLOSED / PASS / LIVE VERIFIED.** Pulumi uses the protected GCS DIY backend and dedicated GCP KMS secrets provider. GitHub WIF admits exactly the manual main-only verification and infrastructure-preview contexts. Separate keyless identities proved positive preview and negative isolation paths. Current Pulumi state: 54 managed resources / 55 records including the stack root; 54 unchanged / zero operations |
| **16C.5** | Documentation and source-of-truth freeze | **PASS / SOURCE-FROZEN.** Repository truth reconciled with the Phase 16C live state; documentation-only, with no new cloud capability |
| **16D.1** | Immutable artifact publication architecture | **IMPLEMENTED IN SOURCE / TESTED LOCALLY / PREVIEWED.** Dedicated keyless publisher, repository-scoped publication, immutable container bases/tags, deterministic frontend archive, release-manifest contract, and dormant workflow template. Preview: 5 creates, 2 updates, 52 unchanged, no replace/delete. Nothing is live or published |
| **16D.2+** | Protected publication environment and live publication | **PLANNED / NOT IMPLEMENTED.** Freeze reviewed source, establish the GitHub environment before activating the workflow, apply/verify publisher infrastructure, then perform one controlled immutable publication |
| **16E** | Controlled CD | **PLANNED / NOT IMPLEMENTED.** Explicitly approved, least-privilege deployment using immutable artifacts |
| **16F** | Live CI/CD verification and freeze | **PLANNED / NOT IMPLEMENTED.** Prove publication/deployment boundaries, rollback, and zero-key operation before declaring CI/CD complete |
| **17** | Application security automation | **PLANNED.** Semgrep, Gitleaks, Dependency-Check, Dependabot, and appropriately gated ZAP |
| **18** | Additional public hardening | **PLANNED.** Honest badges and optional cost-reviewed edge controls; Graph live only if real |
| **19** | Hardening + grand final audit | rotation proof, chain tamper demo, CV matrix update, freeze |

## Adjustments vs original sketch

- Camel (8) before cloud (15): routes must exist locally.
- Graph (14) after vault (9): client secret reference must exist.
- Attestation (12) uses a cloud-neutral signer port. Real KMS was provisioned in 15H-B and live-verified in 15H-C; local JCA remains the default.

## Stop conditions per phase

Each phase updates `PROJECT_STATUS.md` and `CV_CLAIMS_MATRIX.md`. A claim becomes `LIVE VERIFIED` only when dated evidence proves the real path; Phase 15J and Phase 16C.4 have such evidence. Phase 16 remains in progress until 16D, 16E, and 16F complete.

## Non-goals until a new ADR

Kubernetes, Kafka, Redis, AI, extra Graph APIs, Terraform, GKE, always-on VMs.
