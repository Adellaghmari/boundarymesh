# Phase 8 — Apache Camel outbound integration delivery

**Status: COMPLETE (local / TESTED)**

Camel is a first-class integration framework on the **delivery worker**. It is not the security engine.

## Resolved versions

| Artifact | Version | Notes |
| --- | --- | --- |
| Apache Camel | **4.22.0 LTS** | `camel-spring-boot-bom`. No 4.22.1 on Maven Central at implementation time. |
| Java | **25** | Not downgraded |
| Spring Boot | **4.1.1** | Not downgraded |
| Resilience4j | **2.4.0** | Core modules from BOM. `resilience4j-spring-boot4` is pinned explicitly because the 2.4.0 BOM historically omitted that artifact. Programmatic Retry + CircuitBreaker registries are the runtime path. |
| WireMock | **3.13.1** | `org.wiremock:wiremock-standalone` for external connector simulation |

## Module boundary

```text
backend/api     public Boundary API — no Camel on the compile/runtime classpath
backend/worker  local delivery worker — Camel, Resilience4j, secret resolution, WireMock stubs
```

The API talks to the worker only on the `local` profile (`POST http://127.0.0.1:8081/internal/deliveries`). Test profile uses `RecordingDeliveryPort`. Pub/Sub remains PLANNED.

## What Camel owns

Content-based router, Lumenway message translator, idempotent consumer, dead-letter channel for unknown destinations, metadata-only wire tap.

## What Camel does not own

JWT, HMAC, contract drift, egress policy, BoundaryOutcome selection.

## Retry ownership

Resilience4j owns outbound HTTP retries: **maxAttempts = 3 total attempts** (first try + 2 retries). Camel has zero redeliveries. Future Pub/Sub would be a third bounded layer. Do not stack all three blindly.

## EIPs used

| EIP | Why |
| --- | --- |
| Content-Based Router | Destination → CoveCRM / Lumenway / Rivertide. Unknown → DLC. |
| Message Translator | Internal approved JSON → Lumenway fulfillment schema |
| Idempotent Consumer | Same delivery instruction ID must not create a second provider side effect |
| Dead Letter Channel | Unknown destination / terminal route failure → quarantine handoff |
| Wire Tap | Safe metadata only (route, destination, delivery id) |

Circuit breaker is Resilience4j, not a custom Camel CB. Only retryable provider exceptions increment the failure window.
