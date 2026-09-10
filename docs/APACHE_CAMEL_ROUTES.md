# Apache Camel routes

Apache Camel is a **first-class integration framework** on the **delivery worker**. It is not a keyword and not the policy engine.

Resolved version: **Camel 4.22.0 LTS** via `camel-spring-boot-bom`. Java 25 and Spring Boot 4.1.1 were not downgraded.

The Boundary Enforcement Engine decides **whether and how** a transaction may cross. Camel then:

- routes the already-approved `DeliveryInstruction` to a destination connector
- translates the approved body into a provider-specific schema (Lumenway)
- applies idempotent consumption and dead-letter handling
- never sees JWT, HMAC secrets, or unredacted rejected payloads

## Process rule (current local)

```text
ALLOW / ALLOW_WITH_TRANSFORMATION
        → DeliveryInstruction (approved payload only)
        → DeliveryPort
        → local HTTP worker ingest  (Pub/Sub PLANNED)
        → Camel direct:delivery
        → Content-Based Router on destination_system
        → Message Translator where required
        → Resilience4j-protected HTTP connector
        → WireMock synthetic provider  (live providers PLANNED)
        → DELIVERED | DLC / quarantine
```

Security `choice().when(header("authorized"))` is **forbidden**. REJECT and pre-delivery QUARANTINE never create an instruction.

## Enterprise Integration Patterns used

| Pattern | Where | Why |
| --- | --- | --- |
| **Content-Based Router** | `destination_system` | CoveCRM / Lumenway / Rivertide. Unknown → no default sink |
| **Message Translator** | Lumenway route | Internal approved schema ≠ fulfillment API schema. Separate from Data Boundary Firewall |
| **Idempotent Consumer** | delivery instruction id | Second consume must not create a second provider side effect |
| **Dead Letter Channel** | unknown destination / terminal route | Quarantine handoff. Camel does **not** retry |
| **Wire Tap** | inbound route, metadata only | route id, destination, delivery id. No secrets, no payload |

Not used: Aggregator, Saga, Claim Check, Kafka, every EIP in the catalog.

Circuit breaker is **Resilience4j**, composed around the HTTP client, not a decorative Camel policy.

## Route catalog

| Route id | From | To | Notes |
| --- | --- | --- | --- |
| `delivery.inbound` | `direct:delivery` | CBR | Worker entry + wire tap |
| `sink.covecrm.activity` | CBR | HTTP CoveCRM | Simple JSON REST |
| `sink.lumenway.fulfillment` | CBR | translator → HTTP | Requires mapping |
| `sink.rivertide.instruction` | CBR | HTTP Rivertide | Stricter credential/consent demo |
| `error.dlc` | dead letter | quarantine result | No accidental default delivery |

## Retry ownership

| Layer | Retries |
| --- | --- |
| Resilience4j | **3 total attempts** on retryable provider failures (timeout, 429, 500, 503) |
| Camel | **zero** redeliveries |
| Pub/Sub | PLANNED, must stay a separate bounded budget |

`maxAttempts = 3` means first try + two retries, not three retries after success.

Retry-After is parsed and capped (`retry-after-cap`, local default 50ms) so a provider cannot force hours-long sleep.

## API classpath

`boundarymesh-api` must not depend on Camel. ArchUnit and the module POM enforce this. Camel stays in `boundarymesh-worker`.
