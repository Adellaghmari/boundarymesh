# ADR 0010 — Contracts and Resilience4j

## Status

Accepted.

## Context

Contracts must execute at runtime. Outbound calls need bounded retry and circuit breaking. Spring Framework 7 has `@Retryable` but not a circuit breaker.

## Decision

- **OpenAPI 3.1** for HTTP API.
- **AsyncAPI** for Pub/Sub delivery instructions.
- **JSON Schema 2020-12** for payload firewall.
- **Resilience4j** on outbound connector methods only (time limiter, retry with Retry-After, circuit breaker, Graph rate limiter).
- Do **not** stack Spring `@Retryable` on those methods.

## Why these contract specs?

Each matches a real surface. Unused YAML is forbidden.

## Why Resilience4j?

Circuit breaker and Retry-After are required for quarantine/recovery. Spring 7 retry alone is insufficient. Bulkhead not used (Cloud Run concurrency).

## Consequences

Drift → quarantine operational state, not only HTTP 400.
