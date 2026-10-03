# API contracts

Generated OpenAPI: `/v3/api-docs` (springdoc). Partner/runtime contracts live under `contracts/` and are enforced at runtime.

401/403 bodies are sanitized JSON (`timestamp`, `request_id`, `error_code`, `message`). Not HTML. No token, signature, or stack traces.

Base path: `/api/v1`.

## HTTP surface

| Method | Path | Auth | Notes |
| --- | --- | --- | --- |
| GET | `/health`, `/ready` | public | Status only |
| GET | `/security/me` | bearer JWT | Safe identity only |
| POST | `/identity/evaluate` | bearer JWT (authenticated) | Domain AuthN/AuthZ → engine |
| POST | `/dev/evaluate` | bearer JWT + `boundary.evaluate` | Local/test synthetic evaluate |
| POST | `/dev/tokens` | local/test only | Mint scenario tokens. Not Entra |
| GET | `/dev/jwks`, `/dev/issuer` | local/test only | Test JWKS |
| POST | `/webhooks/quayforge` | HMAC headers (`quayforge-hmac`, **not** OAuth) | Raw-body signature |
| POST | `/dev/webhook-lab/{scenario}` | local/test only | Server-side signing. No secret to Vue |
| POST | `/dev/contract-lab/{scenario}` | local/test only | Contract Drift Lab. Safe violation paths |
| POST | `/dev/egress-lab/{destination}/{scenario}` | local/test only | Data Boundary Lab |
| POST | `/dev/crossings/evaluate` | bearer JWT + `boundary.evaluate` | Composed local pipeline |

Security schemes in OpenAPI: `bearer-jwt` and `quayforge-hmac`. No secret values.

## Public (no auth)

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/health` | liveness |
| GET | `/ready` | Firestore connectivity |
| GET | `/overview` | wow-path summary |
| GET | `/integrations` | list (synthetic flag, no secrets) |
| GET | `/transactions` | filter: outcome, integration, source |
| GET | `/transactions/{id}` | full evidence questions below |
| GET | `/transactions/{id}/graph` | boundary graph view-model |
| GET | `/quarantine` | open items (safe) |
| GET | `/attestations/{id}` | attestation document |
| POST | `/attestations/{id}/verify` | signature verify |
| GET | `/audit-chains/{id}/verify` | chain verify |
| GET | `/connectors/m365` | mode badge, permissions, subscription expiry |
| GET | `/posture` | controls + later scan summaries |
| GET | `/project-identity` | git SHA, revisions (live project fields) |
| POST | `/ingest/jwt` | machine JWT transactions |
| POST | `/ingest/webhooks/quayforge` | HMAC webhooks |
| POST | `/ingest/graph/notifications` | Graph notifications |
| GET/POST | `/ingest/graph/notifications` | validation handshake (`validationToken`) |

Ingest endpoints are public **to the producer**, not anonymous-arbitrary: they enforce JWT/HMAC/Graph authenticity. Recruiter browsers do not call ingest.

## Protected (Entra session + CSRF)

| Method | Path | Role |
| --- | --- | --- |
| POST | `/admin/quarantine/{id}/reprocess` | OPERATOR+ |
| POST | `/admin/credentials/{id}/rotate` | SECURITY_ADMIN |
| POST | `/admin/connectors/{id}/disable` | SECURITY_ADMIN |
| POST | `/admin/contracts/{id}/activate` | SECURITY_ADMIN |
| POST | `/admin/egress-policies/{id}/activate` | SECURITY_ADMIN |

Unauthenticated call → 401. Wrong role → 403. Tests must prove both.

## Transaction detail — required answers

The JSON for `GET /transactions/{id}` maps 1:1 to UI:

| Question | Field |
| --- | --- |
| Where from? | `source_system` |
| Where to? | `destination_system` |
| Which identity? | `identity_subject_ref`, `authentication_method` |
| AuthN valid? | `authentication_result` |
| AuthZ sufficient? | `authorization_result` |
| Authenticity? | `authenticity_result` |
| Replay? | `replay_result` |
| Contract? | `contract_id`, `contract_version`, `contract_result` |
| Schema ok? | `contract_result` + `schema_pointers[]` |
| Transformed/redacted? | `redaction_summary` + field **paths** |
| Credential ref? | `credential_ref` + version |
| Delivery? | `delivery_result` |
| Outcome? | `boundary_outcome` |
| Attestation verifiable? | `attestation_id` + verify endpoint |

## AsyncAPI

`delivery-instructions` message is the AsyncAPI payload for an approved crossing.
DLQ envelope adds `delivery_attempt`, `last_error_class` (enum, no body dump).

## Versioning

API URI `/api/v1`. Breaking changes increment URI. Additive fields allowed.

## Rate limits

Public reads are loose. Ingest is limited per connector. Verify is limited per caller. The application enforces the numbers. They are not a published service level.
