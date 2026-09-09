# Data Boundary Firewall

Flagship capability #4.

Before information leaves one trust boundary, BoundaryMesh evaluates **destination-specific deterministic rules**. There is no ML PII classifier.

## Model

```text
source payload (after contract VALID)
        → policy lookup (destination + integration)
        → for each field path:
              ALLOW | REDACT | TRANSFORM | DENY
        → if any DENY and deny_mode=reject → REJECT
        → if any REDACT/TRANSFORM → ALLOW_WITH_TRANSFORMATION
        → else ALLOW
```

Policies are versioned: `egressPolicies/{policyId}/versions/{version}`.

## Actions

| Action | Meaning | Evidence |
| --- | --- | --- |
| `ALLOW` | field copied | `accepted[]` |
| `REDACT` | replaced with stable token `[REDACTED]` or hashed surrogate | `redacted[]` + rule id |
| `TRANSFORM` | deterministic function (e.g. date to date-only) | `transformed[]` |
| `DENY` | field must not appear; if present → reject or strip per `deny_mode` | `denied[]` |

`deny_mode`:

- `reject` — default for Rivertide (financial-like)
- `strip` — only if policy explicitly allows dropping (never for secrets: secrets are always reject)

## Example: Helix calendar → CoveCRM

| Field | Rule |
| --- | --- |
| `id` | ALLOW |
| `subject` | ALLOW |
| `start` / `end` | ALLOW |
| `attendees[].email` | REDACT |
| `organizer.email` | REDACT |
| `bodyPreview` | DENY (strip or reject — **reject** if contains, default strip with metric for calendar notes) |
| `internal_score` | DENY reject (if a producer ever adds it) |
| `api_secret` / `accessToken` | DENY reject |

UI: “2 fields redacted (`attendees.email`, `organizer.email`) because `covecrm.egress.v1`.” Values are not shown.

## Example: QuayForge → Rivertide

| Field | Rule |
| --- | --- |
| `order_id` | ALLOW |
| `amount` | ALLOW |
| `currency` | ALLOW |
| `customer_id` | ALLOW |
| `email` | REDACT |
| `pan` / `card_number` / `cvv` | DENY reject |
| `internal_risk_score` | DENY reject |

Rivertide is **not a real payment processor**. Data is synthetic. Still treat PAN-like fields as Restricted.

## Minimization

If a destination schema does not include a field, it is not “ALLOW by default.” Default is **implicit deny** for unspecified paths. Wildcard `attendees.*` must be explicit.

## Evidence without leakage

Persist:

```text
egress_policy_version
accepted_field_paths[]
redacted_field_paths[]
denied_field_paths[]
transformed_field_paths[]
payload_sha256
```

Do not persist original emails, PANs, or body text.

## Engine interaction

Egress runs only if AuthN/AuthZ/authenticity/replay/contract succeeded (or contract quarantined — **no egress on quarantine-from-contract**; no delivery).

## Tests

- allow unchanged
- redaction removes email from outbound JSON
- deny reject
- destination B allows a field destination A redacts (two policies)
- UI serializer never includes raw Restricted fixtures in public API fixtures
