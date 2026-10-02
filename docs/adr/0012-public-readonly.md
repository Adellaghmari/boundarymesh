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

**Current hosted reality:** the Firebase SPA is an operator console (Google sign-in). Anonymous public recruiter APIs are **not** the live Hosting path. Local labs remain the walkthrough. See [../../PROJECT_STATUS.md](../../PROJECT_STATUS.md).
