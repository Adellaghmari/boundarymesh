# CV claims matrix

**Rule:** promote only what is actually supported. `LIVE VERIFIED LOCALLY` requires dated evidence in PROJECT_STATUS.md.

Statuses: `PLANNED` → `IMPLEMENTED` → `TESTED` → `LIVE VERIFIED LOCALLY` / `LIVE VERIFIED`

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
| Integration Boundary Graph | **TESTED** / **LIVE VERIFIED LOCALLY** | Vue SVG from `GET /api/v1/boundary-graph`. Live `m365-calendar` edge OBSERVED/ACTIVE after a real Graph event. Helix 365 remains SYNTHETIC. Not blast-radius |
| Java 25 | **TESTED** | Temurin 25.0.4.1; `--release 25`; Enforcer |
| Maven | **TESTED** | Wrapper 3.9.11; `mvnw.cmd verify` |
| Spring Boot 4.1.1 | **TESTED** | Context load + HTTP tests |
| Spring Security (local filter chain) | **TESTED** | Two chains: admin session CSRF + bearer JWT resource server |
| Spring Security OAuth2 resource server | **TESTED** | Local JWT decoder + JWKS. Machine identity unchanged |
| Spring Security OIDC BFF | **LIVE VERIFIED LOCALLY** | Authorization Code `oauth2Login`, server-side session, SPA CSRF. Vue never receives Microsoft tokens |
| Identity Federation Gateway | **TESTED** / **LIVE VERIFIED LOCALLY** | Local issuer TESTED. Operator Entra OIDC login LIVE VERIFIED LOCALLY (common authority, personal Microsoft account) |
| AuthN vs AuthZ evidence | **TESTED** | HTTP 401/403 and domain identity/authorization controls |
| JWT validation (iss/aud/exp/nbf/sig) | **TESTED** | Asymmetric RS256 |
| JWKS validation / key rotation tests | **TESTED** | In-process JWKS |
| Scope/role authorization | **TESTED** | `boundary.read` / `boundary.evaluate` / `boundary.admin` / `SCOPE_Calendars.Read` |
| Microsoft Entra ID (admin) | **LIVE VERIFIED LOCALLY** | Common v2 authority. Work/school and personal Microsoft accounts. Not an enterprise Exchange Online mailbox |
| Entra client-credentials JWT | PLANNED | Machine identity remains the local resource server |
| OIDC Authorization Code / browser login | **LIVE VERIFIED LOCALLY** | Human Operator browser; server-side session; delegated `Calendars.Read` |
| Webhook HMAC-SHA256 | **TESTED** | QuayForge raw-body contract |
| Constant-time signature comparison | **TESTED** | `MessageDigest.isEqual` |
| Timestamp freshness | **TESTED** | ±300s via `Clock` |
| Replay defense | **TESTED** | Atomic in-memory nonce store |
| Idempotency foundation | **TESTED** | Separate from replay; `duplicate_delivery` |
| Replay vs duplicate distinction | **TESTED** | Same nonce ≠ same event_id |
| Graph notification validation (harness) | PLANNED | |
| Graph subscription lifecycle | PLANNED | |
| Microsoft Graph Calendar connector | **LIVE VERIFIED LOCALLY** | Real Graph `calendarView` HTTP 200 via `MicrosoftGraphCalendarAdapter`. Live event `count=1` crossed `m365-calendar/v1` |
| Microsoft Graph Java SDK | **LIVE VERIFIED LOCALLY** | `com.microsoft.graph:microsoft-graph:6.67.0` used on the live 2xx path |
| Calendars.Read least privilege | **LIVE VERIFIED LOCALLY** | Delegated `Calendars.Read` only. `SCOPE_Calendars.Read` still required for admin pull |
| Microsoft 365 integration | **LIVE VERIFIED LOCALLY** | Live Microsoft Graph calendar integration using delegated `Calendars.Read` with a personal Microsoft account, server-side OIDC session, `m365-calendar/v1` normalization, `m365-calendar-egress/v1` enforcement, audit, local JCA attestation, and an OBSERVED graph edge. Helix 365 remains SYNTHETIC DEMO. Not an enterprise Exchange Online mailbox |
| Apache Camel 4.22 routes | **TESTED** | Worker-only 4.22.0. API artifact has no Camel |
| EIP: Content-Based Router | **TESTED** | Destination discriminator |
| EIP: Message Translator | **TESTED** | Lumenway fulfillment mapping |
| EIP: Idempotent Consumer | **TESTED** | Delivery instruction id |
| EIP: Dead Letter Channel | **TESTED** | Unknown destination / terminal route |
| EIP: Wire Tap | **TESTED** | Metadata only |
| connector delivery | **TESTED** | Synthetic CoveCRM / Lumenway / Rivertide + WireMock |
| OpenAPI 3.1 | **TESTED** | Partner artifact `contracts/openapi/` plus springdoc. Runtime uses referenced JSON Schema |
| AsyncAPI delivery instructions | **TESTED** | Validated AsyncAPI 3 artifact. Pub/Sub runtime still PLANNED |
| JSON Schema runtime validation | **TESTED** / **LIVE VERIFIED LOCALLY** | networknt 3.0.6. Live Graph event: contract control PASS for `m365-calendar/v1` |
| versioned integration contracts | **TESTED** / **LIVE VERIFIED LOCALLY** | QuayForge v1/v2 TESTED. `m365-calendar/v1` LIVE VERIFIED LOCALLY on one real event |
| Contract Drift Firewall | **TESTED** | Drift ≠ malformed. Affects BoundaryOutcome |
| destination-specific data egress policy | **TESTED** / **LIVE VERIFIED LOCALLY** | CoveCRM / Rivertide / Lumenway TESTED. `m365-calendar-egress/v1` LIVE VERIFIED LOCALLY on one real Graph event |
| deterministic redaction | **TESTED** / **LIVE VERIFIED LOCALLY** | `[REDACTED]` outbound copy. Live calendar subject redaction required (`EGRESS_REDACTION_REQUIRED`) |
| data minimization | **TESTED** / **LIVE VERIFIED LOCALLY** | Implicit deny + destination allow-lists. Live Graph adapter does not select subject/attendees/body/join URL |
| Data Boundary Firewall | **TESTED** / **LIVE VERIFIED LOCALLY** | Real transformation, not UI masking. Live Graph event: `ALLOW_WITH_TRANSFORMATION`, transformation_count=4 |
| Boundary Enforcement Engine foundation | **TESTED** | Synthetic ControlVector plus real contract/egress |
| expanded Boundary Enforcement Engine | **TESTED** / **LIVE VERIFIED LOCALLY** | Nine ControlVector fields. Live Graph event: contract PASS, egress PASS, outcome `ALLOW_WITH_TRANSFORMATION` |
| integrated multi-control boundary crossing | **TESTED** / **LIVE VERIFIED LOCALLY** | Labs use `POST /api/v1/dev/crossings/evaluate`. Live Graph event used `evaluateWithoutDelivery` |
| Outcomes ALLOW / TRANSFORM / QUARANTINE / REJECT | **TESTED** / **LIVE VERIFIED LOCALLY** | Live Graph event outcome `ALLOW_WITH_TRANSFORMATION` |
| JUnit 5 | **TESTED** | Identity + webhook + engine suites |
| ArchUnit | **TESTED** | Domain/application must not depend on Spring/cloud |
| Spotless | **TESTED** | Bound to `verify` |
| Spring Actuator health | **TESTED** | `/api/v1/health` and `/api/v1/ready` |
| Credential & Consent Vault (local model) | **TESTED** | References, versions, consent. Not a managed vault |
| CredentialReference / version / rotation | **TESTED** | Active version only; no silent retired fallback |
| consent / permission model | **TESTED** | Distinct from credential availability |
| credential resolvability control | **TESTED** | PASS / FAIL / UNAVAILABLE |
| connector safety control | **TESTED** | Circuit / configured destination |
| Google Secret Manager | **LIVE** / **CONTAINERS ONLY** | API enabled. Four empty secrets. No versions. No IAM |
| Quarantine & Recovery Orchestrator (local) | **TESTED** | State machine, eligibility, admin reprocess |
| bounded reprocess | **TESTED** | Cap 3; `boundary.admin` |
| delivery idempotency | **TESTED** | Distinct from inbound webhook replay |
| Resilience4j Retry | **TESTED** | 3 total attempts; 400/401/403 never retried |
| Resilience4j Circuit Breaker | **TESTED** | CLOSED / OPEN / HALF_OPEN |
| WireMock | **TESTED** | External connector simulation |
| Pub/Sub delivery decoupling | **IMPLEMENTED + EMULATOR TESTED** | Persist-before-publish. At-least-once + worker idempotency. Not LIVE |
| Firestore Native access patterns | **IMPLEMENTED + EMULATOR TESTED + LIVE VERIFIED LOCALLY** | Named DB `boundarymesh` LIVE / PROVISIONED. Residue CLEANED. Not Cloud Run workload-live |
| Cloud Run API + worker | PLANNED | |
| Cloud KMS EC_SIGN_P256_SHA256 | PLANNED | Local JCA ECDSA P-256 is TESTED; KMS adapter is not |
| Cryptographic Exchange Attestation | **LIVE VERIFIED LOCALLY** | Local JCA P-256 SIGNED + verify VALID for one real Graph calendar event. Does not prove business truth. Cloud KMS PLANNED |
| RFC 8785 canonicalization | **TESTED** | Constrained application writer. Floats rejected. No Jackson default |
| Attestation signature verification | **TESTED** | ECDSA P-256 P1363 + Base64. UNSIGNED is explicit |
| SHA-256 tamper-evident audit chain | **TESTED** / **LIVE VERIFIED LOCALLY** | Live `m365-calendar` chain VALID: CONNECTOR_READ count=1, BOUNDARY_DECISION, ATTESTATION_CREATED. Not immutable |
| Deterministic audit canonicalization | **TESTED** | RFC 8785 subset over audit-entry/v1 |
| Audit-chain verification | **TESTED** | Sequence, previous-hash, recalculated SHA-256 |
| Concurrency-safe audit append | **TESTED** / **EMULATOR TESTED** / **LIVE VERIFIED LOCALLY** | In-memory per-chain lock; Firestore transactional append |
| ECDSA P-256 | **TESTED** | JCA `SHA256withECDSAinP1363Format` |
| JCA asymmetric cryptography | **TESTED** | Ephemeral local key `local-jca-p256` |
| state-driven Vue SVG/graph visualization | **TESTED** | Custom SVG, no graph library |
| Pulumi TypeScript repository foundation | **IMPLEMENTED** / **LOCALLY VALIDATED** | `infra/` typecheck + zero-resource test. No `pulumi up` |
| GCP project | **CREATED + BILLING LINKED** | `boundarymesh-dev`. No application resources |
| gcloud local authentication | **CONFIGURED + VERIFIED** | Active project `boundarymesh-dev` |
| Application Default Credentials | **CONFIGURED + VERIFIED LOCALLY** | Token discarded; ADC not in Git |
| Pulumi local backend | **CONFIGURED + VERIFIED LOCALLY** | `file://~`. GCS state PLANNED |
| Pulumi `dev` stack configuration | **IMPLEMENTED** / **LOCALLY VALIDATED** | `gcp:project`, `gcp:region`, `boundarymesh:environment` |
| GCP project configuration | **IMPLEMENTED** / **LOCALLY VALIDATED** | `gcp:project = boundarymesh-dev` |
| GCP region configuration | **IMPLEMENTED** / **LOCALLY VALIDATED** | `europe-west4` |
| Zero-resource Pulumi preview | **TESTED** | Phase 15B envelope-only preview. Superseded by 15D registry preview |
| API Docker image definition | **IMPLEMENTED** / **LOCALLY TESTED** | `backend/api/Dockerfile` |
| Worker Docker image definition | **IMPLEMENTED** / **LOCALLY TESTED** | `backend/worker/Dockerfile` |
| API container Artifact Registry image | **PUSHED** / **REMOTE DIGEST VERIFIED** | `boundarymesh-api:phase15d`. See `infra/README.md` |
| Worker container Artifact Registry image | **PUSHED** / **REMOTE DIGEST VERIFIED** | `boundarymesh-worker:phase15d`. See `infra/README.md` |
| Artifact Registry Pulumi definition | **IMPLEMENTED** / **APPLIED** | One Docker repo `boundarymesh` in `europe-west4` |
| Artifact Registry API | **LIVE** / **ENABLED IN GCP** | `artifactregistry.googleapis.com` via Pulumi; `disableOnDestroy=false` |
| Artifact Registry repository | **LIVE** / **PROVISIONED IN GCP** | `projects/boundarymesh-dev/locations/europe-west4/repositories/boundarymesh` |
| Artifact Registry round-trip | **LIVE VERIFIED** | Push → remote digest → pull by digest → local smoke |
| Secret Manager adapter architecture | **IMPLEMENTED** / **TESTED** | `SecretValueProvider` + `WebhookSecretProvider` in `backend/gcp` |
| Secret Manager production adapter | **IMPLEMENTED** / **LOCALLY TESTED WITH MOCKED/CONTROLLED CLIENT** | Default `mvnw verify` does not call GCP |
| Secret Manager API Pulumi definition | **IMPLEMENTED** / **APPLIED** | `secretmanager.googleapis.com`; `disableOnDestroy=false` |
| Secret containers | **LIVE** / **PROVISIONED IN GCP** | `hmac-quayforge`, `covecrm-api`, `lumenway-api`, `rivertide-api` |
| Secret Manager API in GCP | **LIVE** / **ENABLED IN GCP** | |
| Secret containers in GCP | **LIVE** / **EMPTY** / **NO VERSIONS** | User-managed replication `europe-west4` |
| Secret versions | PLANNED | No payloads in this phase |
| Workload IAM | PLANNED | |
| Cloud Run env-from-secret | PLANNED FOR PHASE 15I | Entra remains `BOUNDARYMESH_ENTRA_CLIENT_SECRET` |
| GCP provider / authentication | **CONFIGURED + VERIFIED LOCALLY** | ADC. No `GOOGLE_APPLICATION_CREDENTIALS`. No JSON keys |
| GCP adapter module architecture | **IMPLEMENTED** / **TESTED** | `backend/gcp`. Domain/application remain cloud-independent |
| GCP emulator test foundation | **IMPLEMENTED** / **LOCALLY VALIDATED** | Opt-in `gcp-emulator` tag. Default verify needs no ADC |
| Firestore production adapter | **IMPLEMENTED + EMULATOR TESTED + LIVE VERIFIED LOCALLY** | Not default-wired. `GoogleFirestoreFacade` against named DB `boundarymesh`. Residue **CLEANED** |
| Pub/Sub production adapter | **IMPLEMENTED + EMULATOR TESTED + LIVE VERIFIED AGAINST REAL GCP** | Publisher + pull consumer in `backend/gcp`. Not default-wired. Not Cloud Run workload-live |
| Pulumi TypeScript GCP resources | Artifact Registry **LIVE**; Secret Manager containers **LIVE** (empty); Firestore database **LIVE / PROVISIONED**; Pub/Sub topics + pull subscriptions **LIVE / PROVISIONED** | Dead-letter forwarding, Cloud KMS, Cloud Run, Hosting |
| GitHub Actions + GCP WIF | PLANNED | |
| Semgrep | PLANNED | |
| Gitleaks | PLANNED | |
| OWASP Dependency-Check | PLANNED | |
| OWASP ZAP | PLANNED | |
| Dependabot | PLANNED | |
| Pact | PLANNED | |
| Testcontainers emulators | PLANNED | |
| Camel tests | **TESTED** | Worker `CamelDeliveryPipelineTest` + `ResilienceAndCircuitTest` against WireMock |
| Frontend API integration | **TESTED** | health, ready, JWT evaluate, webhook lab |
| Micrometer | **TESTED** | `boundarymesh.delivery.*`, quarantine/reprocess counters, R4j tagged metrics. No GCP export |
| Cloud Logging / Monitoring / Trace | PLANNED | |
| Firebase Hosting | PLANNED | |
| Public recruiter demo | PLANNED | |
| Protected admin mutations | **TESTED** | Session CSRF on `/api/v1/admin/**`. Local `boundary.admin` reprocess remains |
| Self-dogfood Git SHA / revision | PLANNED | |
| LIVE Graph tenant call | **LIVE VERIFIED LOCALLY** | Operator pull Graph HTTP 200, `count=1`. Contract harness is not this evidence |

No row may move to LIVE VERIFIED or LIVE VERIFIED LOCALLY without dated evidence in PROJECT_STATUS.md.
