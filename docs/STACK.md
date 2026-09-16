# Stack lock

Phase 0 version lock as of **2026-09-09**. No runtime is provisioned. Patch versions may move forward within the locked major/minor lines during later phases if a security fix requires it. Major upgrades require a new ADR.

## Language and runtime

| Component | Locked | Why |
| --- | --- | --- |
| JDK | **Java 25 LTS** (Eclipse Temurin) | Current Java LTS. Spring Boot 4.1 supports 17–26. Camel 4.22.0 supports 17, 21, and 25. |
| Build | **Apache Maven 3.9.x** | Standard Spring Boot packaging; one module with two Spring Boot applications sharing a domain jar. |
| Bytecode | `--release 25` | No accidental older-API drift. |

Java 21 remains an emergency fallback only if a transitive library later proves incompatible. It is not the target.

**Phase 1 resolved (2026-09-09):** Eclipse Temurin **25.0.4.1+1**, Maven Wrapper **3.9.11**, Spring Boot **4.1.1**, Spring Security **7.1.1** (BOM, not forced).

**Phase 3–4 resolved (2026-09-10):** Spring Boot **4.1.1**, Spring Security **7.1.1** (BOM), Nimbus JOSE JWT **10.9.1** (via `spring-security-oauth2-jose`, not a second JWT library). Node **22.23.2** (22 LTS patch; not Node 24).

## Backend

| Component | Locked | Why |
| --- | --- | --- |
| Spring Boot | **4.1.1** | Current OSS line (OSS support through 2027-07). 4.0 OSS ends 2026-12. |
| Spring Framework | **7.x** (BOM-managed) | Jakarta EE 11, Jackson 3, JSpecify nullness. |
| Spring Security | **7.1.x** (BOM-managed) | OAuth2 resource server, OIDC client, method security. |
| Apache Camel | **4.22.0 LTS** | Current Camel LTS with Spring Boot 4 and Java 25. |
| Resilience4j | **2.4.0** (`resilience4j-spring-boot4` pinned explicitly; BOM does not manage that artifact) | Retry + CircuitBreaker on **outbound connectors only**. Timeouts via RestClient read timeout. Spring `@Retryable` is **not** stacked. |
| JSON Schema | `com.networknt:json-schema-validator` **3.0.6** | Runtime JSON Schema 2020-12 / OpenAPI 3.1 dialect. Jackson 3 / Java 17+. Apache 2.0. Selected after checking maintenance and Java 25 compatibility. Adapter lives in `api`, not domain. |
| JSON Canonicalization | Constrained RFC 8785 writer in `application` (`Rfc8785Canonicalizer`) | No extra JCS library. Titanium JCS is Jackson 2; Boot 4 is Jackson 3. Integers only; floats rejected. |
| Microsoft Graph | `microsoft-graph` **6.67.0** | Official Graph Java SDK used in `MicrosoftGraphCalendarAdapter`. |
| Azure Identity | not added | Delegated token comes from Spring OAuth2 client session, not azure-identity. |
| OpenTelemetry | Spring Boot 4 official OTel starter | Traces and metrics to Cloud Trace / Monitoring. |
| springdoc | OpenAPI 3.1 compatible starter for Boot 4 | Generated + authored OpenAPI stay in sync. |

**Rejected as hosting/runtime:** Spring Cloud Azure Entra starter as the primary security stack (Azure-centric in a GCP product). Entra is an **identity provider**, configured through Spring Security OAuth2.

**Rejected as duplicate resilience:** Spring Framework 7 `@Retryable` on Camel connector methods. One retry owner: Resilience4j.

## Frontend

| Component | Locked |
| --- | --- |
| Vue | **3.5.42** line (3.5.x) |
| TypeScript | **6.x** selected by official Vue tooling (strict) |
| Pinia | **4.0.3** line (4.x) |
| Vue Router | **5.3.1** line (5.x) |
| Vite | **8.2.x** |
| SCSS | `sass` modern compiler |
| HTTP | Typed native `fetch` client — no Axios |

**Phase 2 resolved (2026-09-09):** Vue **3.5.42**, Pinia **4.0.3**, Vue Router **5.3.1**, Vite **8.2.2**, TypeScript **6.0.3**, Vitest **4.1.11**, Playwright **1.63.0**, MSW **2.15.0**. Details: [PHASE2.md](PHASE2.md).
| Graph viz | Custom Vue SVG Integration Boundary Graph (no extra graph library) |
| Tests | Vitest + Vue Test Utils + Playwright |

No Next.js. No Nuxt as a hosting framework (Vite SPA is enough). No Tailwind-as-identity: SCSS is the visual system.

## Cloud and IaC

| Component | Locked |
| --- | --- |
| Region | **`europe-west4`** (Netherlands) — single region, matches operator timezone UTC+2 |
| Compute | Cloud Run API Service + continuous Worker Pool |
| Messaging | Pub/Sub |
| Data | Firestore **Native** mode, regional `europe-west4` |
| Secrets | Secret Manager |
| Signing | Cloud KMS `EC_SIGN_P256_SHA256` |
| Frontend host | Firebase Hosting |
| Images | Artifact Registry |
| Observability | Cloud Logging, Monitoring, Trace |
| IaC | Pulumi 3.x + TypeScript + `@pulumi/gcp` |
| State | GCS DIY backend with object versioning (no Pulumi Cloud subscription required) |

## Test and AppSec (planned)

| Tool | Role |
| --- | --- |
| JUnit 5 | Unit / slice |
| Mockito | Mocks where isolation is required |
| Spring Boot Test / Spring Security Test | HTTP and security |
| Camel test support | Route and error-path tests |
| WireMock | Graph and partner HTTP simulation |
| Pact JVM | Consumer contracts for Graph client and selected API consumers |
| Testcontainers | Firestore emulator, Pub/Sub emulator, WireMock |
| OWASP Dependency-Check | Java dependencies |
| Gitleaks | Secret scanning |
| Semgrep | SAST |
| OWASP ZAP | Baseline DAST against public demo later |
| Dependabot | Dependency PRs |

Not central: Trivy / Syft / Cosign. Container scan may exist because Cloud Run images exist. It is not the Project C story.

## Module shape

Phase 1 implements `backend/domain`, `backend/application`, `backend/api` only.

## Module shape (later phases)

```text
backend/
  domain/            # no Spring, no GCP, no Camel
  application/       # use cases, engine orchestration
  security/          # Spring Security adapters, JWT, HMAC
  integration/       # contract, egress, replay, idempotency
  connectors/        # Graph, CRM, orders, settlements adapters
  camel/             # routes only — no policy decisions
  gcp/               # Google Cloud adapters (Phase 15C boundary; no production clients yet)
  persistence/       # Firestore ports + adapters (later, inside gcp)
  audit/             # chain + attestation
  delivery/          # Pub/Sub publisher/consumer
  api/               # Spring MVC — Boundary API
  worker/            # Spring Boot — Delivery Worker
frontend/            # Vue 3 SPA
infra/               # Pulumi TypeScript (Phase 15A foundation; zero resources)
contracts/           # OpenAPI, AsyncAPI, JSON Schema (source of truth)
```

Two deployable Spring Boot applications share `domain`, `application`, `integration`, `audit`. The API process does not call Graph. The worker process does not accept public ingress.
