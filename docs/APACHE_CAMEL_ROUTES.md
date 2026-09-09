# Apache Camel routes

Apache Camel is a **first-class integration framework** on the **delivery worker**. It is not a keyword and not the policy engine.

The Boundary Enforcement Engine decides **whether and how** a transaction may cross. Camel then:

- translates the normalized delivery instruction into a connector-specific request
- routes to the correct endpoint
- applies integration patterns for failure, idempotency, and safe audit metadata
- isolates HTTP/Graph SDK details behind endpoints

## Why Camel is justified

BoundaryMesh is an **enterprise integration product**. After enforcement, work must be delivered to heterogeneous systems (Graph, CRM HTTP, settlements HTTP) with translation, dead-lettering, and idempotent consumers. That is Camel’s job. Putting those patterns in ad-hoc RestClient wrappers would hide the integration story and duplicate EIP.

Camel is **not** justified for JWT validation, HMAC, or egress policy. Those stay in explicit Java.

## Process rule

```text
Pub/Sub delivery instruction (already ALLOW / ALLOW_WITH_TRANSFORMATION)
        → Camel consumer (direct:delivery)
        → Content-Based Router on destination_system
        → Message Translator (instruction → connector body)
        → Resilience4j / HTTP / Graph producer
        → success evidence  |  Dead Letter Channel
```

`.choice().when(header("authorized"))` for security is **forbidden**. The worker refuses instructions that lack a persisted `EnforcementDecision` with ALLOW*.

## Enterprise Integration Patterns used

| Pattern | Where | Why |
| --- | --- | --- |
| **Message Router / Content-Based Router** | `destination_system` | Helix/CoveCRM vs QuayForge/Rivertide vs Lumenway |
| **Message Translator** | before each producer | Delivery instruction schema ≠ destination schema |
| **Dead Letter Channel** | after bounded retries | Poison / exhausted work → quarantine, not infinite loop |
| **Idempotent Consumer** | on `idempotency_key` | Legitimate Pub/Sub and Graph duplicates |
| **Circuit Breaker** | Camel + Resilience4j around outbound | Protect worker from downstream collapse; open circuit → quarantine |
| **Wire Tap** | after translation, **metadata only** | Safe field lists, hashes, correlation IDs to audit — **no** Restricted raw values |

Patterns **not** used in v1 (no real need): Aggregator, Saga, Claim Check (unless payload size forces it later), Durable Subscriber, Message Bus-as-Kafka.

## Route catalog (target)

| Route id | From | To | Notes |
| --- | --- | --- | --- |
| `delivery.inbound` | `direct:delivery` | cbr | Worker entry |
| `sink.covecrm.activity` | cbr | HTTP CoveCRM | Calendar → CRM |
| `sink.rivertide.instruction` | cbr | HTTP Rivertide | Order → settlement |
| `sink.lumenway.fulfillment` | cbr | HTTP Lumenway | Optional third sink |
| `error.dlc` | dead letter | quarantine writer | Always |

Graph **fetch** (if pointer-style notifications) is a worker use-case **before** Camel or a dedicated `source.helix.event.fetch` route. Fetch still goes through Resilience4j. Fetched payload is contract-validated **before** Camel sink.

## Idempotent consumer

- Key: `integration_id + destination + idempotency_key`
- Store: Firestore `idempotency/{hash}` with TTL ≥ 7 days (Phase 15 tunes TTL)
- Concurrent duplicate: first writer wins; second returns previous delivery result
- This is **not** the replay window

## Error handling

Camel `OnException`:

- `SignatureException` / policy exceptions: **never retry** (should not appear on worker if API did its job; treat as poison → security event)
- HTTP 401/403: no retry, quarantine
- HTTP 429: retry with `Retry-After` if present
- HTTP 5xx / timeout: bounded retry (max 3 in-route) then DLC
- Circuit open: do not pile retries; quarantine `CONNECTOR_CIRCUIT_OPEN`

Pub/Sub redelivery is a **second** retry layer. Combined budget is documented in [FAILURE_MODEL.md](FAILURE_MODEL.md). In-route retries + Pub/Sub max attempts must remain bounded (target: ≤ 8 total outbound tries).

## Testing

Camel `CamelTestSupport` / Spring Camel test:

- router selects sink
- translator omits denied fields
- DLC invoked on 500 after retries
- idempotent consumer skips second send
- wire tap does not contain email plaintext in test fixtures that assert log/audit payloads

## Explicit non-goals

- Camel as HTTP inbound gateway
- YAML-only routes hiding Java policy
- Kafka component
- Hundreds of Kamelets
