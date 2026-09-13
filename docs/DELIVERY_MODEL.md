# Delivery model

After the Boundary Enforcement Engine emits `ALLOW` or `ALLOW_WITH_TRANSFORMATION`, BoundaryMesh may create a `DeliveryInstruction`.

REJECT and pre-delivery QUARANTINE produce **zero** outbound delivery.

## DeliveryInstruction

Safe application message (AsyncAPI concept, now a Java type):

- event / transaction / integration identifiers
- source and destination
- contract and egress policy references
- boundary outcome
- **approved** payload JSON (already redacted when required)
- correlation id, created-at
- credential **reference** and consent id

Never includes raw JWT, HMAC secret, OAuth client secret, Authorization headers, or KMS material.

## DeliveryPort

Application code depends on `DeliveryPort`. Adapters:

| Profile | Adapter | Truth |
| --- | --- | --- |
| `test` | `RecordingDeliveryPort` | In-process capture. No worker. |
| `local` | `HttpWorkerDeliveryPort` | HTTP to the local worker. Not Pub/Sub. |
| `gcp-emulator` / future prod | Outbox + Pub/Sub | Adapters **EMULATOR TESTED + LIVE VERIFIED LOCALLY**. Infra **LIVE / PROVISIONED**. Not the local default. Not Cloud Run workload-live. |

Camel is behind `CamelDeliveryAdapter` in the worker.

## DeliveryResult

`DELIVERED` | `DUPLICATE_SUPPRESSED` | `QUARANTINED` | `PERMANENT_FAILURE` | `NOT_ATTEMPTED`

HTTP status is evidence, not the domain model.

## Invariants

1. Transformed deliveries must not contain the original governed email.
2. Duplicate consumption of the same instruction ID does not call the provider again.
3. Connector secret material is resolved in the worker immediately before HTTP, then discarded with the adapter call.
4. Persist the outbox record before Pub/Sub publish. Transport is at-least-once. End-to-end delivery is **not** exactly-once.
5. `REJECT` and `QUARANTINE` create zero outbox records.
