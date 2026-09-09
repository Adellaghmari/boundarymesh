# Phase 5 — Contract Drift Firewall

**Status: COMPLETE (local / TESTED)**

Contracts are runtime enforcement artifacts.

- Canonical files: `contracts/openapi/`, `contracts/asyncapi/`, `contracts/json-schema/`
- Version header: `X-Velora-Contract-Version` (default `v1`)
- Runtime payload validation: JSON Schema 2020-12 via `com.networknt:json-schema-validator:3.0.6`
- OpenAPI 3.1 is the QuayForge HTTP partner contract, not a springdoc dump
- AsyncAPI 3.0 is the future delivery-instruction event contract. Pub/Sub remains PLANNED
- Classifications: `VALID`, `MALFORMED_PAYLOAD`, `SCHEMA_VIOLATION`, `UNSUPPORTED_CONTRACT_VERSION`, `CONTRACT_DRIFT`, `CONTRACT_UNAVAILABLE`
- `CONTRACT_DRIFT` only when the declared version fails but another supported version accepts the payload
- Evidence: JSON Pointer + keyword + type category. No payload values. Max 20 violations
- Unsupported / drift → QUARANTINE. Malformed / schema violation → REJECT

Do not start Phase 8 from this document.
