# Phase 6 — Data Boundary Firewall

**Status: COMPLETE (local / TESTED)**

Destination-specific deterministic egress. Not AI PII classification. Not OPA/Rego.

- Policies: `contracts/policies/*.json`, validated at startup
- Actions: `ALLOW`, `REDACT` (`[REDACTED]`), `DENY` with `REJECT` or `STRIP`
- Paths: RFC 6901 JSON Pointer
- Implicit deny: unlisted fields omitted on controlled schemas. Ancestor ALLOW covers children
- Critical `DENY` + `REJECT` → no outbound body, `EGRESS_FIELD_DENIED`
- Redaction / strip / implicit deny → real new outbound JSON and `ALLOW_WITH_TRANSFORMATION`
- Original payload string is not mutated
- Evidence: path, action, rule id, policy version. Not values

CoveCRM may receive email. Lumenway and Rivertide redact it. `internal_risk_score` is a critical deny.

Do not start Phase 8 from this document.
