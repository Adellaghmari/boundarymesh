# Boundary Enforcement Engine

The Boundary Enforcement Engine is the core of Project C. It is **not** Project A's Decision Assurance Engine. After Phase 7 it composes **real** identity, authorization, authenticity, freshness, replay, contract, and egress results. It still does not parse JWT, verify HMAC, validate JSON Schema, or redact.

| | Decision Assurance (A) | Boundary Enforcement (C) |
| --- | --- | --- |
| Question | Can a human trust an AI recommendation? | Can this transaction cross this boundary? |
| Inputs | retrieved chunks, claims, gates | identity, authz, authenticity, replay, contract, egress, connector |
| Output | assurance packet, possible abstention | `ALLOW` / `ALLOW_WITH_TRANSFORMATION` / `QUARANTINE` / `REJECT` |
| Human | required for the business decision | not required for the crossing decision |
| AI | surrounding the engine | **none** |

The engine executes **deterministic system policy**. It does not estimate truth, confidence, or model quality.

## Position in the flow

```text
normalized inbound envelope
        │
        ▼
   ControlPipeline (ordered stages)
        │
        ▼
   ControlVector (typed results, no payload copies of secrets)
        │
        ▼
   BoundaryEnforcementEngine.decide(ControlVector, IntegrationPolicy)
        │
        ▼
   EnforcementDecision
        ├── outcome
        ├── reasonCodes[]
        ├── appliedRules[]
        ├── nextAction   (PUBLISH_DELIVERY | PERSIST_QUARANTINE | RESPOND_REJECT)
        ├── transformationSummary  (field actions, never raw values)
        └── evidenceRefs
```

Camel is **not** the engine. Camel consumes `nextAction == PUBLISH_DELIVERY`.

## Ordered stages

Security-verification stages **fail closed**. Availability stages may **quarantine**.

| Order | Stage | Fail posture | Typical outcomes |
| --- | --- | --- | --- |
| 0 | `INGEST` | closed on malformed envelope | reject |
| 1 | `AUTHENTICATE` | closed | reject |
| 2 | `AUTHORIZE` | closed | reject |
| 3 | `AUTHENTICITY` | closed | reject |
| 4 | `FRESHNESS` | closed | reject |
| 5 | `REPLAY` | closed | reject (malicious replay) |
| 6 | `CONTRACT` | closed on schema invalid; **quarantine** on version drift / unsupported version if policy says hold | reject or quarantine |
| 7 | `DATA_EGRESS` | closed on `DENY` fields if policy is reject; transform on `REDACT` | reject, transform, or allow |
| 8 | `CREDENTIAL_RESOLVABILITY` | quarantine if reference missing / disabled | quarantine |
| 9 | `CONNECTOR_SAFETY` | quarantine if circuit open | quarantine |
| 10 | `DECIDE` | n/a | combine |
| 11 | `ROUTE` | n/a | publish / quarantine / reject |
| 12 | `ATTEST` | attestation failure does not rewind a completed REJECT/ALLOW; status becomes `UNSIGNED` | see failure model |
| 13 | `AUDIT` | fail closed on hash write failure after decision — decision already persisted; chain gap is a security event | detective |

Stages 1–5 short-circuit. Later stages do not run if identity is invalid. That is intentional: do not schema-validate an unauthenticated attacker payload into operational quarantine by default. Policy flag `quarantineUnauthenticated` is **false**.

Contract drift for an **authenticated, authorized** caller **does** quarantine so operators can inspect safe diffs.

## Control vector (conceptual types)

```text
AuthenticationResult    valid | invalid | expired | issuer | audience | jwks_unavailable
AuthorizationResult     allowed | missing_scope | missing_app_role | integration_denied
AuthenticityResult      valid | invalid_signature | missing_signature | provider_mismatch
FreshnessResult         fresh | timestamp_expired | clock_skew_exceeded
ReplayResult            clear | replay_detected | nonce_missing
DuplicateHint           unknown | possible_duplicate   (informational; not the security decision)
ContractResult          valid | schema_invalid | version_unsupported | drift
EgressResult            allow | transformed | field_denied
CredentialResult        resolvable | missing | disabled | rotation_pending
ConnectorSafetyResult   healthy | circuit_open | unknown
```

`DuplicateHint` is **not** `ReplayResult`. Duplicate delivery is handled by idempotency on the worker. Replay is a security reject on the API.

## Decision table (simplified)

The engine is table-driven in tests. Production uses the same ordered reduction.

| Condition | Outcome | Next action |
| --- | --- | --- |
| AuthN invalid / expired / wrong iss/aud | `REJECT` | RESPOND_REJECT |
| AuthZ missing scope/role | `REJECT` | RESPOND_REJECT |
| Invalid signature | `REJECT` | RESPOND_REJECT |
| Timestamp outside window | `REJECT` | RESPOND_REJECT |
| Replay nonce seen | `REJECT` | RESPOND_REJECT |
| Schema invalid (malformed) | `REJECT` | RESPOND_REJECT |
| Version unsupported or breaking drift | `QUARANTINE` | PERSIST_QUARANTINE |
| Egress DENY with deny-mode=reject | `REJECT` | RESPOND_REJECT |
| Egress REDACT only | `ALLOW_WITH_TRANSFORMATION` | PUBLISH_DELIVERY |
| Secret missing / circuit open | `QUARANTINE` | PERSIST_QUARANTINE |
| All pass, no transform | `ALLOW` | PUBLISH_DELIVERY |

No weighted scoring. No “mostly authenticated.”

## Reason codes

Stable strings for API, UI, metrics, attestations:

```text
INGEST_MALFORMED
AUTHN_INVALID_JWT
AUTHN_EXPIRED
AUTHN_WRONG_ISSUER
AUTHN_WRONG_AUDIENCE
AUTHN_JWKS_UNAVAILABLE
AUTHZ_MISSING_SCOPE
AUTHZ_MISSING_APP_ROLE
AUTHZ_INTEGRATION_DENIED
AUTHENTICITY_INVALID_SIGNATURE
AUTHENTICITY_MISSING
FRESHNESS_EXPIRED
REPLAY_DETECTED
CONTRACT_SCHEMA_INVALID
CONTRACT_VERSION_UNSUPPORTED
CONTRACT_DRIFT
EGRESS_FIELD_DENIED
EGRESS_REDACTED
CREDENTIAL_UNAVAILABLE
CREDENTIAL_DISABLED
CONNECTOR_CIRCUIT_OPEN
DELIVERY_TIMEOUT
DELIVERY_HTTP_429
DELIVERY_HTTP_5XX
DELIVERY_RETRY_EXHAUSTED
ATTESTATION_SIGNING_FAILED
AUDIT_CHAIN_APPEND_FAILED
```

UI copy maps codes to sentences. Codes never include payload values.

## Applied rules

Each decision lists rule identifiers, for example:

- `entra.jwt.audience:boundary-api`
- `helix.calendar.contract:v1`
- `covecrm.egress.v3.email:REDACT`
- `replay.window:300s`

The Integration Boundary Graph reads these, not free-text logs.

## Testability

The engine is a plain Java class in `application` / `domain`. Tests supply a `ControlVector`. No Spring context required for decision-table tests.

Forbidden:

- putting `decide()` inside Camel `.choice()`
- calling Graph from the engine
- consulting an LLM
- returning a double “score”

## Output persisted

`boundaryDecisions/{transactionId}` stores the decision plus hashes of canonical payload **after** transformation, never the secret material, never the raw JWT.
