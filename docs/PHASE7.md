# Phase 7 — Boundary Enforcement Engine expansion

**Status: COMPLETE (local / TESTED)**

The engine still does not parse JWT, verify HMAC, validate JSON Schema, or redact. It composes control results.

Real / TESTED controls: identity, authorization, authenticity, freshness, replay, contract, egress.

Still foundational/synthetic: credential resolvability, connector safety.

Precedence (confirmed):

```text
REJECT > QUARANTINE > ALLOW_WITH_TRANSFORMATION > ALLOW
```

- Security FAIL/UNAVAILABLE → REJECT
- Contract unsupported / drift / unavailable → QUARANTINE
- Contract malformed / schema invalid → REJECT
- Egress policy unavailable → QUARANTINE
- Egress critical deny → REJECT
- Egress redaction → ALLOW_WITH_TRANSFORMATION

Early termination: HMAC/freshness/replay failures do not inspect payload fields. Contract blocks skip egress (`NOT_APPLICABLE`).

HTTP vs domain: 401/403 at Spring Security; signed webhook transport remains 200 + `BoundaryOutcome`. Untrusted webhook is not a schema oracle.

`POST /api/v1/dev/crossings/evaluate` is the composed local pipeline (JWT + contract + egress).

Do not start Apache Camel, Credential Vault, or Quarantine Orchestrator from this document.
