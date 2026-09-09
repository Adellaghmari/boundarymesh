# CV claims matrix

**Rule:** promote only what is actually supported. Nothing is LIVE VERIFIED in Phase 7.

Statuses: `PLANNED` → `IMPLEMENTED` → `TESTED` → `LIVE VERIFIED`

This is portfolio work, not professional employment.

| Capability | Status | Notes |
| --- | --- | --- |
| Product thesis / Velora Interchange | **IMPLEMENTED** | Console uses Velora systems; SYNTHETIC DEMO labeled |
| Vue 3 | **TESTED** | create-vue + recruiter console + Vitest |
| TypeScript | **TESTED** | `vue-tsc --build` |
| Pinia | **TESTED** | runtime + events stores |
| Vue Router | **TESTED** | typed routes + 404 |
| SCSS | **TESTED** | token system, no Tailwind |
| Vite | **TESTED** | dev proxy + production build |
| Vitest | **TESTED** | unit/component tests |
| Vue Test Utils | **TESTED** | outcome, control vector, routes |
| Playwright | **TESTED** | local smoke vs real Java backend |
| MSW | **TESTED** | frontend API unit tests only; not WireMock |
| Integration Boundary Graph | PLANNED | |
| Java 25 | **TESTED** | Temurin 25.0.4.1; `--release 25`; Enforcer |
| Maven | **TESTED** | Wrapper 3.9.11; `mvnw.cmd verify` |
| Spring Boot 4.1.1 | **TESTED** | Context load + HTTP tests |
| Spring Security (local filter chain) | **TESTED** | Resource-server chain, headers, CORS, 401/403 JSON |
| Spring Security OAuth2 resource server | **TESTED** | Local JWT decoder + JWKS. Not Entra |
| Spring Security OIDC BFF | PLANNED | No Authorization Code login |
| Identity Federation Gateway | **TESTED** | Local issuer. Entra remains PLANNED |
| AuthN vs AuthZ evidence | **TESTED** | HTTP 401/403 and domain identity/authorization controls |
| JWT validation (iss/aud/exp/nbf/sig) | **TESTED** | Asymmetric RS256 |
| JWKS validation / key rotation tests | **TESTED** | In-process JWKS |
| Scope/role authorization | **TESTED** | `boundary.read` / `boundary.evaluate` / `boundary.admin` |
| Microsoft Entra ID (admin) | PLANNED | No app registration |
| Entra client-credentials JWT | PLANNED | |
| Webhook HMAC-SHA256 | **TESTED** | QuayForge raw-body contract |
| Constant-time signature comparison | **TESTED** | `MessageDigest.isEqual` |
| Timestamp freshness | **TESTED** | ±300s via `Clock` |
| Replay defense | **TESTED** | Atomic in-memory nonce store |
| Idempotency foundation | **TESTED** | Separate from replay; `duplicate_delivery` |
| Replay vs duplicate distinction | **TESTED** | Same nonce ≠ same event_id |
| Graph notification validation (harness) | PLANNED | |
| Graph subscription lifecycle | PLANNED | |
| Microsoft Graph Calendar connector | PLANNED | |
| Microsoft Graph Java SDK | PLANNED | |
| Calendars.Read least privilege | PLANNED | |
| Apache Camel 4.22 routes | PLANNED | No Camel dependency |
| EIP: router, translator, DLC, idempotent consumer, CB, wire tap | PLANNED | |
| OpenAPI 3.1 | **TESTED** | Partner artifact `contracts/openapi/` plus springdoc. Runtime uses referenced JSON Schema |
| AsyncAPI delivery instructions | **TESTED** | Validated AsyncAPI 3 artifact. Pub/Sub runtime still PLANNED |
| JSON Schema runtime validation | **TESTED** | networknt 3.0.6, dialect 2020-12 |
| versioned integration contracts | **TESTED** | QuayForge v1/v2 supported, v3 unsupported |
| Contract Drift Firewall | **TESTED** | Drift ≠ malformed. Affects BoundaryOutcome |
| destination-specific data egress policy | **TESTED** | CoveCRM / Rivertide / Lumenway |
| deterministic redaction | **TESTED** | `[REDACTED]` outbound copy |
| data minimization | **TESTED** | Implicit deny + destination allow-lists |
| Data Boundary Firewall | **TESTED** | Real transformation, not UI masking |
| Boundary Enforcement Engine foundation | **TESTED** | Synthetic ControlVector plus real contract/egress |
| expanded Boundary Enforcement Engine | **TESTED** | Seven real controls composed |
| integrated multi-control boundary crossing | **TESTED** | `POST /api/v1/dev/crossings/evaluate` |
| Outcomes ALLOW / TRANSFORM / QUARANTINE / REJECT | **TESTED** | |
| JUnit 5 | **TESTED** | Identity + webhook + engine suites |
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
| Frontend API integration | **TESTED** | health, ready, JWT evaluate, webhook lab |
| OpenTelemetry / Micrometer | PLANNED | Actuator health only |
| Cloud Logging / Monitoring / Trace | PLANNED | |
| Firebase Hosting | PLANNED | |
| Public recruiter demo | PLANNED | |
| Protected admin mutations | PLANNED | |
| Self-dogfood Git SHA / revision | PLANNED | |
| LIVE Graph tenant call | PLANNED | Explicitly not claimed |

No row may move to LIVE VERIFIED without dated evidence in PROJECT_STATUS.md.
