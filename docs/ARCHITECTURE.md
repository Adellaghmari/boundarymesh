# Architecture

BoundaryMesh is a Cloud Run API Service plus a Cloud Run Worker Pool, one enforcement engine,
and a delivery fabric. Security policy lives in explicit Java types. Apache Camel runs **after**
the engine has decided.

## Decision summary

| Decision | Choice | Why |
| --- | --- | --- |
| Frontend | Vue 3 + TS + Pinia + Vue Router + SCSS + Vite | Distinct from React (A) and Angular (B); recruiter console |
| Backend | Java 25 + Spring Boot 4.1.1 + Spring Security 7.1 | Real Java/Spring security and API engineering |
| Integration runtime | Apache Camel 4.22.0 on the **worker** | Connector routes, translation, DLC, idempotent consumer |
| Primary cloud | GCP `europe-west4` | Distinct from Azure (A) and AWS (B); serverless |
| Compute | Cloud Run Service + Worker Pool | API request-based/scale-to-zero; worker is a continuous pull process |
| Ingress vs delivery | Sync enforcement, async delivery | Downstream instability must not pin the public boundary |
| Data | Firestore Native | Access-pattern first; not Postgres (A) or DynamoDB (B) |
| Async | Pub/Sub (+ unused DLQ topic) | Integration-oriented delivery bus. Dead-letter forwarding is not configured |
| Secrets | Secret Manager | References only in Firestore |
| Signatures | Cloud KMS ECDSA P-256 | Private key never leaves KMS |
| IaC | Pulumi TypeScript + GCS state | Distinct from Terraform (B) |
| CI identity | GitHub → GCP WIF | No JSON keys |
| Hosted operator identity | Firebase Google Auth + Web BFF session | Real authorization, no browser workload credentials |
| Microsoft connector admin identity | Microsoft Entra OIDC (local BFF) | Delegated Graph authorization remains a distinct local integration |
| Graph | Outlook Calendar, one deep connector | Contract harness TESTED; one personal-account event LIVE VERIFIED LOCALLY; production Entra disabled; notifications not implemented |
| Product shell | Custom Vue/SCSS premium header | Core / Labs / Operations / Evidence / Status; no UI framework |
| AI | None | Deterministic policy is the product |

## High-level production flow

```text
FIREBASE HOSTING  (SPA + session/API rewrites)
        │
        ▼
PUBLIC WEB BFF  —  boundarymesh-web
        │  Firebase Google session / CSRF / route allowlist
        ▼
IAM-PRIVATE BOUNDARY API  —  boundarymesh-api
        │  identity / authorization / authenticity / freshness / replay
        │  contract / data egress
        │  credential resolvability (metadata) / connector safety
        │  Boundary Enforcement Engine
        │
        ├─ REJECT      →  evidence only  (no DeliveryInstruction)
        ├─ QUARANTINE  →  quarantine item  (no Camel)
        └─ ALLOW / ALLOW_WITH_TRANSFORMATION
                │
                ▼
        DeliveryInstruction  (approved payload only)
                │
                ▼  prod: transactional outbox + Pub/Sub
CLOUD RUN WORKER POOL  —  boundarymesh-worker  (no HTTP server or URL)
        │  just-in-time Secret Manager credential resolve
        │  Camel CBR + translator
        │  Resilience4j around outbound HTTP
        │  DELIVERED | DUPLICATE_SUPPRESSED | QUARANTINED
```

Hosted connectors remain **intentionally unconfigured**. The live Worker path verified durable
quarantine and ACK without calling an external provider or reading a connector secret.
Dead-letter forwarding remains unconfigured. Production Entra/OIDC is disabled. There is no
OpenTelemetry or Cloud Trace component in this topology.

## Why this is not an API gateway

A gateway would authenticate optionally, rate-limit, and reverse-proxy. BoundaryMesh **terminates the untrusted transaction**, runs a multi-control engine with typed reason codes, may transform or hold the payload, and only then emits a **normalized delivery instruction** whose contract is AsyncAPI. The destination never sees the raw untrusted request.

## Service boundaries

### `boundarymesh-api` (Cloud Run Service)

- Receives HTTP transactions and Graph validation handshakes.
- Runs synchronous enforcement stages that do not require destination I/O.
- Connector **availability** is a local circuit snapshot + credential **metadata**, not a live Graph call.
- Publishes to Pub/Sub only after `ALLOW` or `ALLOW_WITH_TRANSFORMATION`.
- Identity: dedicated keyless service account `boundarymesh-api`.
- Permissions: Firestore data read/write, publish only to `boundarymesh-deliveries`, sign and read
  the public key only on `exchange-attestation`, and access only `hmac-quayforge`.
- Cloud Run invocation remains IAM-authenticated. Only the dedicated Web BFF service account has
  service-level Invoker access; the core has no public or project-wide Invoker grant.

### `boundarymesh-worker` (Cloud Run Worker Pool)

- Consumes Pub/Sub.
- Resolves connector credentials from Secret Manager.
- Executes Camel routes with Resilience4j.
- Identity: dedicated keyless service account `boundarymesh-worker`.
- Permissions: Firestore data read/write, subscribe only to `boundarymesh-delivery-worker`, and
  Secret Manager accessor only on `covecrm-api`, `lumenway-api`, and `rivertide-api`.
- No KMS, Pub/Sub publisher, DLQ publisher, or administrative permission.

The worker uses the locked pull subscription architecture. Worker Pools have no HTTP ingress or
service URL. The Spring production profile starts no web server and keeps the pull consumer alive.

## Process architecture (Java)

```text
domain          entities, typed IDs, enums, reason codes, errors
application     BoundaryEnforcementEngine, use cases
security        JwtAuthn, HmacAuthenticity, EntraAuthorizer  (adapters)
integration     ContractFirewall, EgressFirewall, ReplayWindow, IdempotencyStore
connectors      ports (CalendarSource, CrmSink, OrderSource, SettlementSink)
camel           RouteBuilder only: translate, route, DLC, wire-tap metadata
persistence     Firestore adapters
audit           HashChain, AttestationCanonicalizer, KmsSigner port
delivery        DeliveryInstruction, PubSubPublisher, PubSubConsumer
web             Spring MVC, problem+json
worker          Spring Boot consumer loop
```

Rules:

- HTTP controllers do not contain policy.
- Camel DSL does not contain authorization decisions.
- GCP SDKs do not leak into `domain`.
- Engine input is a `ControlVector` of typed stage results; output is `EnforcementDecision`.

## Network / trust boundaries

See [NETWORK_BOUNDARIES.md](NETWORK_BOUNDARIES.md). Short form:

1. Browser → Firebase Hosting → public Web BFF (application session)
2. Web BFF → IAM-private `boundarymesh-api` (service ID token)
3. `boundarymesh-api` → Firestore / KMS / Pub/Sub (workload SA)
4. Pub/Sub → `boundarymesh-worker` (pull)
5. Worker → Secret Manager / Firestore (named secrets only)
6. Worker → external destinations **only when configured** (currently not)

Each hop has a different identity. Secret access is scoped to the named secrets each workload
actually resolves.

## Frontend architecture

Vue 3 SPA is hosted on Firebase Hosting. Hosted Google authentication, the Firebase-compatible HttpOnly `__session`, operator authorization, CSRF, a non-mutating keyless BFF-to-IAM-private-core read, and exactly one controlled browser-origin governed transaction are live verified. The transaction reached durable Worker quarantine and ACK without external connector contact. Recruiter console uses a sticky premium header: Core destinations, Labs/Operations/Evidence dropdowns, Status, and a compact Recruiter/Operator control. Microsoft 365 Calendar lives under **Connections**, not a top-level `/m365` route.

| Route | Auth | Purpose |
| --- | --- | --- |
| `/` | public | Overview |
| `/connections` | public | Velora synthetic systems + Microsoft 365 Calendar connector |
| `/boundary-events` `/boundary-events/:id` | public | Boundary events |
| `/quarantine` `/quarantine/:id` | public read / operator reprocess | Quarantine workflow |
| `/audit` `/audit/:integrationId` | public | Tamper-evident audit chain |
| `/attestations` `/attestations/:id` | public | Exchange attestation + Java verify |
| `/boundary-graph` | public | Integration Boundary Graph |
| `/architecture` | public | Honest stack |
| `/status` | public | Capabilities / project status |
| `/identity` `/webhooks` `/contracts` `/data-boundary` `/crossing` `/delivery` `/credentials` | public labs | Local control demonstrations |
| `/api/v1/admin/**` | Entra session + CSRF when configured | Operator Graph pull |

Pinia stores: `events`, `quarantine`, `evidence` (audit chains, attestations, boundary graph), `microsoft` session (no tokens). No JWTs in Vue or localStorage.

Visual system: premium navy/teal product header plus **Integration Boundary Graph** — live application state, not a static architecture poster. Spec: [BOUNDARY_GRAPH.md](BOUNDARY_GRAPH.md).

## Identity at the product edge

Three distinct cases, never mixed:

| Case | Flow | Current status |
| --- | --- | --- |
| Recruiter | none | Public static product shell; application routes remain session-gated |
| Hosted operator | Firebase Google identity, Web BFF HttpOnly `__session` | **LIVE VERIFIED** with one controlled browser transaction |
| Microsoft connector admin | Entra OIDC authorization code, local Spring BFF HttpOnly session | **LIVE VERIFIED LOCALLY** with delegated `Calendars.Read` |
| Machine / API | Local JWT/JWKS resource server | **TESTED**. Entra client-credentials JWT remains PLANNED |

**Spring Security chains (local):**

1. `@Order(1)` admin/session: `/api/v1/admin/**`, `/oauth2/**`, `/login/**`, `/logout` — CSRF on, cookie session.
2. `@Order(2)` resource server: `/api/**`, `/v3/**` — bearer JWT/HMAC, CSRF off (stateless).

Delegated Graph permission **`Calendars.Read`** is the flagship calendar connector. Application `Calendars.Read` is not claimed. Graph change notifications are **not implemented**.

## Contracts as runtime

```text
contracts/openapi/boundary-api.yaml          OpenAPI 3.1
contracts/asyncapi/delivery-instructions.yaml AsyncAPI 3
contracts/json-schema/m365-calendar.v1.schema.json
contracts/json-schema/quayforge-order.v1.json
contracts/json-schema/covecrm-activity.v1.json
contracts/json-schema/rivertide-instruction.v1.json
contracts/policies/m365-calendar.egress.v1.json
```

The engine loads schema **version** from the integration record. Unknown versions → `QUARANTINE` + `CONTRACT_VERSION_UNSUPPORTED` (or `REJECT` if policy says hard-fail). See [CONTRACT_MODEL.md](CONTRACT_MODEL.md).

## Coherence audit

| Technology | Job in this architecture | Keyword-only? |
| --- | --- | --- |
| Vue | Recruiter console | No |
| Spring Boot | Composition of API and worker | No |
| Spring Security | Resource server + OIDC BFF | No |
| Apache Camel | Post-decision connector routes | No |
| Microsoft Graph / Entra | Flagship enterprise identity + calendar crossing | No |
| GCP / Cloud Run | Hosting | No |
| Pub/Sub | Decouple delivery from ingress | No |
| Firestore | Access-pattern store | No |
| Secret Manager | Credential vault | No |
| Cloud KMS | Attestation signing | No |
| Pulumi | Real GCP ownership | No |

Removed at Phase 0: Kubernetes, Kafka, Redis, RabbitMQ, service mesh, Spring Cloud Gateway, Quarkus, Micronaut, Next.js, extra databases, AI.

Spring Cloud Gateway is **NOT APPROPRIATE**: it would turn the product into “a gateway with plugins.” Enforcement stays in the application.

## Repository layout

```text
backend/          Java modules (Phase 1)
frontend/         Vue app (Phase 2)
contracts/        schemas (Phase 5)
infra/            Pulumi TypeScript; **83** managed resources; CD-owned traffic fenced
backend/gcp       Production Google Cloud adapters used by the live API and Worker
.github/workflows Keyless verify, preview, publish, deploy, and break-glass evidence recovery
docs/             current truth in README.md of this folder; PHASE_* files are historical
```

## Phase 0 implementation freeze (historical)

At the Phase 0 lock there was no production application code, no GCP, and no Entra registration.
That freeze is closed. Current live topology is documented above.
