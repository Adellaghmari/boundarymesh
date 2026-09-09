# Contract model — Contract Drift Firewall

Flagship capability #3.

Integration contracts are **runtime enforcement artifacts**, not wiki pages.

## Which spec where

| Spec | Use |
| --- | --- |
| **OpenAPI 3.1** | Synchronous Boundary API (public + admin) |
| **AsyncAPI 3** | Pub/Sub `delivery-instructions` and DLQ envelope |
| **JSON Schema 2020-12** | Inbound and outbound **payload** validation inside the engine |

All three are versioned under `contracts/` and loaded by version id from the integration record.

## Versioning

```text
contract_id:     helix-calendar-event
schema_version:  1.2.0
compatibility:   MAJOR.MINOR.PATCH
```

- **PATCH**: documentation / examples — accepted
- **MINOR**: additive optional fields — accepted; extra unknown fields per policy (`strip` or `quarantine`)
- **MAJOR**: required field removed/renamed/type change — `CONTRACT_DRIFT` or `CONTRACT_VERSION_UNSUPPORTED`

The runtime does not “guess” compatibility. A **compatibility table** is explicit:

```text
supported: ["1.1.0", "1.2.0"]
deprecated: ["1.0.0"]   → still validate, metric + warning in UI
unsupported: else       → QUARANTINE (default) or REJECT if policy hard_fail_unsupported=true
```

## Runtime behavior

1. Select schema by `integration.inbound_contract_ref`.
2. Validate payload with networknt (or equivalent) JSON Schema.
3. On failure: collect **pointer paths** (`/attendees/0/email` type mismatch) — **not** values.
4. Classify:
   - malformed required types → `CONTRACT_SCHEMA_INVALID` → `REJECT`
   - declared version not in supported list → `CONTRACT_VERSION_UNSUPPORTED` → `QUARANTINE`
   - breaking structural diff vs last supported (when producer sends a new advertised version) → `CONTRACT_DRIFT` → `QUARANTINE`

## Drift as operational state

Contract drift is **not** “400 Bad Request and forget.”

```text
authenticated producer sends unsupported / breaking payload
        → QUARANTINE
        → quarantine reason CONTRACT_DRIFT
        → safe diff (json-schema error paths + expected version)
        → operator / security admin inspects
        → new schema version activated (admin)
        → safe reprocess against new contract
```

Unknown schemas are **never auto-accepted**.

## Recruiter UI

Show:

- contract id + version
- result: `VALID` | `CONTRACT_DRIFT` | `QUARANTINED` | `REJECTED`
- JSON pointers that failed
- “what we expected” from the schema (types/required), without echoing Restricted values

## Pact and WireMock

- **WireMock:** Graph and partner HTTP, including error shapes.
- **Pact:** `GraphCalendarClient` as consumer of Graph; optional Pact for Vue vs public API if it catches breakage cheaply.

Contract tests must fail when Graph event `subject` becomes an object or `start.dateTime` disappears.

## AsyncAPI delivery instruction (conceptual)

Required fields: `instruction_id`, `transaction_id`, `integration_id`, `destination_system`, `idempotency_key`, `boundary_outcome`, `payload_hash`, `egress_policy_version`, `credential_ref`, `occurred_at`.

Payload body in the message is the **already transformed** JSON. Restricted raw source is not included.

## Explicit non-goals

- Generating code and never validating at runtime
- Storing three unused YAML files for CV keywords
- Using OpenAPI to describe Pub/Sub
