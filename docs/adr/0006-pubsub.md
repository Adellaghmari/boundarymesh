# ADR 0006 — Pub/Sub for post-boundary delivery

## Status

Accepted.

## Context

Downstream Graph/CRM instability must not pin public ingest. This is not Project B’s analysis bus.

## Decision

After ALLOW / ALLOW_WITH_TRANSFORMATION, publish a **normalized delivery instruction** to Pub/Sub. Worker pulls. DLQ after bounded attempts. **No ordering keys** (idempotency is enough).

## Why Pub/Sub?

GCP-native, integration-oriented **decoupling of delivery from ingress**. Not an EventBridge/SQS retelling: messages exist only for **already enforced** work, not for computing the product decision.

## Consequences

Outbox pattern. At-least-once + idempotent consumer. Public REJECT never publishes.
