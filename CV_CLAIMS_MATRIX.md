# CV claims matrix

**Rule:** promote only what is actually supported. Nothing is LIVE VERIFIED in Phase 1.

Statuses: `PLANNED` → `IMPLEMENTED` → `TESTED` → `LIVE VERIFIED`

This is portfolio work, not professional employment.

| Capability | Status | Notes |
| --- | --- | --- |
| Product thesis / Velora Interchange | PLANNED | Domain enums exist; product not shipped |
| Vue 3 + TypeScript + Pinia + Vue Router + SCSS + Vite | PLANNED | |
| Integration Boundary Graph | PLANNED | |
| Java 25 | **TESTED** | Temurin 25.0.4.1; `--release 25`; Enforcer |
| Maven | **TESTED** | Wrapper 3.9.11; `mvnw.cmd verify` |
| Spring Boot 4.1.1 | **TESTED** | Context load + HTTP tests |
| Spring Security (local filter chain) | **IMPLEMENTED** | Local/test permit health/ready/dev. **Not IAM.** |
| Spring Security OAuth2 resource server | PLANNED | Dependency ≠ implementation |
| Spring Security OIDC BFF | PLANNED | |
| Identity Federation Gateway | PLANNED | |
| AuthN vs AuthZ evidence | PLANNED | Control kinds modeled only |
| Microsoft Entra ID (admin) | PLANNED | No app registration |
| Entra client-credentials JWT | PLANNED | |
| JWKS validation / key rotation tests | PLANNED | |
| Webhook HMAC-SHA256 | PLANNED | |
| Replay vs duplicate distinction | PLANNED | Reason code exists; no verifier |
| Graph notification validation (harness) | PLANNED | |
| Graph subscription lifecycle | PLANNED | |
| Microsoft Graph Calendar connector | PLANNED | |
| Microsoft Graph Java SDK | PLANNED | |
| Calendars.Read least privilege | PLANNED | |
| Apache Camel 4.22 routes | PLANNED | No Camel dependency |
| EIP: router, translator, DLC, idempotent consumer, CB, wire tap | PLANNED | |
| OpenAPI 3.1 | **IMPLEMENTED** | springdoc `/v3/api-docs`. **Not runtime contract enforcement.** |
| AsyncAPI delivery instructions | PLANNED | |
| JSON Schema runtime validation | PLANNED | |
| Contract Drift Firewall | PLANNED | |
| Data Boundary Firewall | PLANNED | |
| Boundary Enforcement Engine foundation | **TESTED** | Synthetic ControlVector |
| Outcomes ALLOW / TRANSFORM / QUARANTINE / REJECT | **TESTED** | |
| JUnit 5 | **TESTED** | 38 tests |
| ArchUnit | **TESTED** | Domain/application must not depend on Spring/cloud |
| Spotless | **TESTED** | Bound to `verify` |
| Spring Actuator health | **TESTED** | `/api/v1/health` and `/api/v1/ready` |
| Credential & Consent Vault | PLANNED | |
| Google Secret Manager | PLANNED | No GCP project |
| Secret rotation | PLANNED | |
| Quarantine & Recovery | PLANNED | Outcome exists; no orchestrator |
| Resilience4j outbound | PLANNED | |
| Pub/Sub delivery decoupling | PLANNED | |
| Firestore Native access patterns | PLANNED | |
| Cloud Run API + worker | PLANNED | |
| Cloud KMS EC_SIGN_P256_SHA256 | PLANNED | |
| Cryptographic Exchange Attestation | PLANNED | |
| RFC 8785 canonicalization | PLANNED | |
| Attestation signature verification | PLANNED | |
| Tamper-evident audit chain | PLANNED | |
| Pulumi TypeScript GCP | PLANNED | |
| GitHub Actions + GCP WIF | PLANNED | |
| Semgrep | PLANNED | |
| Gitleaks | PLANNED | |
| OWASP Dependency-Check | PLANNED | |
| OWASP ZAP | PLANNED | |
| Dependabot | PLANNED | |
| WireMock | PLANNED | |
| Pact | PLANNED | |
| Testcontainers emulators | PLANNED | |
| Camel tests | PLANNED | |
| Vitest / Playwright | PLANNED | |
| OpenTelemetry / Micrometer | PLANNED | Actuator health only |
| Cloud Logging / Monitoring / Trace | PLANNED | |
| Firebase Hosting | PLANNED | |
| Public recruiter demo | PLANNED | |
| Protected admin mutations | PLANNED | |
| Self-dogfood Git SHA / revision | PLANNED | |
| LIVE Graph tenant call | PLANNED | Explicitly not claimed |

No row may move to LIVE VERIFIED without dated evidence in PROJECT_STATUS.md.
