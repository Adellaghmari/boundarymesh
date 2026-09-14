# Architecture

BoundaryMesh is two Cloud Run workloads, one enforcement engine, and a delivery fabric. Security policy lives in explicit Java types. Apache Camel runs **after** the engine has decided.

## Decision summary

| Decision | Choice | Why |
| --- | --- | --- |
| Frontend | Vue 3 + TS + Pinia + Vue Router + SCSS + Vite | Distinct from React (A) and Angular (B); recruiter console |
| Backend | Java 25 + Spring Boot 4.1.1 + Spring Security 7.1 | Real Java/Spring security and API engineering |
| Integration runtime | Apache Camel 4.22.0 on the **worker** | Connector routes, translation, DLC, idempotent consumer |
| Primary cloud | GCP `europe-west4` | Distinct from Azure (A) and AWS (B); serverless |
| Compute | Cloud Run ×2, request-based, scale-to-zero | No GKE, no VMs |
| Ingress vs delivery | Sync enforcement, async delivery | Downstream instability must not pin the public boundary |
| Data | Firestore Native | Access-pattern first; not Postgres (A) or DynamoDB (B) |
| Async | Pub/Sub + DLQ | Integration-oriented delivery bus, not a release event bus |
| Secrets | Secret Manager | References only in Firestore |
| Signatures | Cloud KMS ECDSA P-256 | Private key never leaves KMS |
| IaC | Pulumi TypeScript + GCS state | Distinct from Terraform (B) |
| CI identity | GitHub → GCP WIF | No JSON keys |
| Admin identity | Microsoft Entra OIDC (BFF) | Real authorization, not login theatre |
| Graph | Outlook Calendar, one deep connector | Contract harness TESTED; live Graph blocked without tenant |
| Product shell | Custom Vue/SCSS premium header | Core / Labs / Operations / Evidence / Status; no UI framework |
| AI | None | Deterministic policy is the product |

## High-level flow (local now / cloud later)

```text
EXTERNAL SYSTEM  (QuayForge HMAC | JWT machine call | future Graph notification)
        │
        ▼
PUBLIC BOUNDARY API  —  boundarymesh-api  (:8080)
        │  identity / authorization / authenticity / freshness / replay
        │  contract / data egress
        │  credential resolvability (metadata) / connector safety
        │  Boundary Enforcement Engine
        │
        ├─ REJECT      →  evidence only  (no DeliveryInstruction)
        ├─ QUARANTINE  →  local quarantine item  (no Camel)
        └─ ALLOW / ALLOW_WITH_TRANSFORMATION
                │
                ▼
        DeliveryInstruction  (approved payload only)
                │
                ▼  local HTTP ingest today; outbox + Pub/Sub adapters **EMULATOR TESTED + LIVE VERIFIED LOCALLY**; Pub/Sub infra **LIVE / PROVISIONED**; Cloud Run messaging **PLANNED FOR 15I**
LOCAL DELIVERY WORKER  —  boundarymesh-worker  (:8081)
        │  just-in-time credential resolve (local provider)
        │  consent already decided on the API
        │  Camel CBR + translator
        │  Resilience4j around outbound HTTP
        │  WireMock synthetic CoveCRM / Lumenway / Rivertide
        │  DELIVERED | DUPLICATE_SUPPRESSED | QUARANTINED
```

Firestore adapters exist in `backend/gcp` and are **not** the default local runtime. The Firestore database `boundarymesh` is **LIVE / PROVISIONED**. `GoogleFirestoreFacade` is **LIVE VERIFIED AGAINST REAL GCP FIRESTORE**. Live verification residue is **CLEANED**. The transactional outbox and Pub/Sub adapters are **EMULATOR TESTED + LIVE VERIFIED LOCALLY**. Pub/Sub API, topics, and pull subscriptions are **LIVE / PROVISIONED**. Dead-letter forwarding is **PLANNED / NOT CONFIGURED**. Live verification residue is **CLEANED**. Cloud Run workload persistence is **not** live. Cloud Run Worker Pool is **PLANNED FOR PHASE 15I**. Cloud KMS signer adapter is **IMPLEMENTED + UNIT TESTED** and not default-wired. Cloud KMS in GCP is **LIVE / PROVISIONED**. Live KMS signing is **LIVE VERIFIED LOCALLY (15H-C)**. Secret Manager adapters exist in `backend/gcp` and are **not** the default local runtime. Empty secret containers are **LIVE / PROVISIONED IN GCP**. No versions. No workload IAM.

## Why this is not an API gateway

A gateway would authenticate optionally, rate-limit, and reverse-proxy. BoundaryMesh **terminates the untrusted transaction**, runs a multi-control engine with typed reason codes, may transform or hold the payload, and only then emits a **normalized delivery instruction** whose contract is AsyncAPI. The destination never sees the raw untrusted request.

## Service boundaries

### `boundary-api` (public)

- Receives HTTP transactions and Graph validation handshakes.
- Runs synchronous enforcement stages that do not require destination I/O.
- Connector **availability** is a local circuit snapshot + credential **metadata**, not a live Graph call.
- Publishes to Pub/Sub only after `ALLOW` or `ALLOW_WITH_TRANSFORMATION`.
- Serves public read APIs and Entra-protected admin APIs.
- Identity: Cloud Run service account `sa-boundary-api`.
- Permissions: Firestore read/write (app collections), Pub/Sub publish (delivery topic only), KMS sign (attestation key), Secret Manager **denied**, Graph network **not used**.

### `delivery-worker` (not publicly invokable)

- Consumes Pub/Sub.
- Resolves connector credentials from Secret Manager.
- Executes Camel routes with Resilience4j.
- Identity: `sa-delivery-worker`.
- Permissions: Firestore read/write, Pub/Sub consume + DLQ publish, Secret Manager accessor on **named** connector secrets, KMS sign if delivery attestation update is required, no public HTTP.

Ingress protection for the worker: Cloud Run ingress **internal** (Pub/Sub push with OIDC) **or** pull subscription (preferred for portfolio: pull from worker, no public URL). **Lock: pull subscription.** Worker has no HTTP ingress.

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

1. Microsoft 365 / partner internet → public HTTPS on `boundary-api`
2. `boundary-api` → Firestore / KMS (Google backplane, IAM)
3. `boundary-api` → Pub/Sub (IAM)
4. Pub/Sub → `delivery-worker` (pull, SA)
5. `delivery-worker` → Secret Manager / Firestore / KMS
6. `delivery-worker` → external destinations (Graph, synthetic sinks)

Each hop has a different identity. Secret Manager is unreachable from the public service.

## Frontend architecture

Vue 3 SPA (Firebase Hosting remains PLANNED). Recruiter console uses a sticky premium header: Core destinations, Labs/Operations/Evidence dropdowns, Status, and a compact Recruiter/Operator control. Microsoft 365 Calendar lives under **Connections**, not a top-level `/m365` route.

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
| Recruiter | none | Public read + labeled harness POSTs |
| Admin / operator | Entra OIDC authorization code, Spring BFF HttpOnly session | Implemented + TESTED. Live tenant **BLOCKED** without credentials |
| Machine / API | Local JWT/JWKS resource server | **TESTED**. Entra client-credentials JWT remains PLANNED |

**Spring Security chains (local):**

1. `@Order(1)` admin/session: `/api/v1/admin/**`, `/oauth2/**`, `/login/**`, `/logout` — CSRF on, cookie session.
2. `@Order(2)` resource server: `/api/**`, `/v3/**` — bearer JWT/HMAC, CSRF off (stateless).

Delegated Graph permission **`Calendars.Read`** is the flagship calendar connector. Application `Calendars.Read` is not claimed. Graph change notifications remain **PLANNED** until a reachable HTTPS endpoint exists.

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

## Repository layout (later phases — do not scaffold empty code now)

```text
backend/          Java modules (Phase 1)
frontend/         Vue app (Phase 2)
contracts/        schemas (Phase 5)
infra/            Pulumi TypeScript foundation (Phase 15A–15B; zero resources)
backend/gcp       Cloud adapter boundary (Phase 15C; no production clients)
.github/workflows (Phase 17)
docs/             this folder
```

## Implementation freeze for Phase 0

No production application code. No GCP. No Entra app registration. No Graph secrets. No service-account JSON keys.
