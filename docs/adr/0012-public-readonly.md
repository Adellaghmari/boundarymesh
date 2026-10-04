# ADR 0012 — Public recruiter read-only surfaces

## Status

Accepted.

## Context

Recruiters will not complete Entra signup. Mutations are dangerous if anonymous.

## Decision

Public: GET demo/evidence, POST attestation/chain **verify** (crypto, no state change).  
Protected: configuration, rotation, reprocess, policy activation — Entra OIDC BFF + app roles.

## Why?

Usable demo + real authorization boundaries. Login is not a banner; 401/403 tests prove it.

## Consequences

Ingest endpoints are “public” to producers but authenticated by JWT/HMAC/Graph, not by recruiter cookies. Rate-limit verify and ingest.

**Current implementation:** the cloud root is the synthetic product, and `/operator` is the protected operator page. `/explore` redirects to `/`. The public application does not call the API and cannot mutate cloud state. Anonymous APIs are not the live Hosting path. A source change reaches the live channel only through a later approved release. Local labs remain the full walkthrough. See [../ARCHITECTURE.md](../ARCHITECTURE.md).
