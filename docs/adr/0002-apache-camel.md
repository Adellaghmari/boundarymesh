# ADR 0002 — Apache Camel 4.22 on the delivery worker

## Status

Accepted.

## Context

BoundaryMesh delivers to heterogeneous HTTP/Graph destinations after a security decision.

## Decision

Apache **Camel 4.22.0 LTS** with `camel-spring-boot-bom`, routes **only in the worker**. EIPs: content-based router, translator, DLC, idempotent consumer, circuit breaker (Resilience4j), wire tap (metadata).

## Why Camel?

This is an enterprise integration product. Post-allow delivery, translation, and failure routing are Camel’s purpose. Policy stays in `BoundaryEnforcementEngine` so it remains unit-testable.

## Consequences

Two processes. Developers must not put AuthZ in `.choice()`. Camel test module is mandatory in Phase 8.
