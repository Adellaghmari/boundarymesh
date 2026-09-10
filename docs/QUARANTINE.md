# Quarantine and recovery orchestrator

Flagship capability #6.

**Local implementation: see [QUARANTINE_RECOVERY.md](QUARANTINE_RECOVERY.md).** This page keeps the original target design, including Firestore and Pub/Sub, which remain PLANNED.

Integration failure is a **controllable operational state**, not an infinite retry loop and not a silent drop.

## When we quarantine vs reject

| Class | Action |
| --- | --- |
| Security verification failed (AuthN/AuthZ/signature/replay) | **REJECT** — do not quarantine attacker traffic by default |
| Contract drift / unsupported version | **QUARANTINE** |
| Secret missing / disabled | **QUARANTINE** |
| Circuit open | **QUARANTINE** |
| Downstream timeout / 5xx / 429 exhausted | **QUARANTINE** |
| Graph 401/403 | **QUARANTINE** (credential/consent problem, authenticated path) |

## Quarantine record (safe metadata)

A quarantined item must answer:

| Question | Field |
| --- | --- |
| Why? | `reason_codes[]` |
| Which check? | `failed_stage` |
| Retry appropriate? | `retry_eligible` boolean + policy |
| Did policy change? | `policy_versions_at_failure` vs `current` |
| Downstream unavailable? | `http_status`, `circuit_state` |
| Can operator reprocess? | `reprocess_allowed` + required role |

Never store raw Restricted payload. Store hashes, field paths, instruction id.

## Resilience4j (outbound only)

| Primitive | Used? | Where |
| --- | --- | --- |
| Time limiter | yes | Graph and HTTP sinks |
| Retry | yes | 429 (Retry-After), 5xx, timeout — bounded |
| Circuit breaker | yes | per connector |
| Rate limiter | yes on Graph | respect Graph throttling |
| Bulkhead | **no** in v1 | Cloud Run concurrency already bounds work |

Do not stack Spring `@Retryable` on the same methods.

### Retry policy

| Status | Retry | Notes |
| --- | --- | --- |
| 429 | yes | honor `Retry-After`; else exponential backoff |
| 5xx / timeout | yes | max 3 in-route |
| 401 / 403 | **no** | quarantine |
| invalid signature (ingress) | **no** | never a delivery retry |

## Pub/Sub

- Max delivery attempts: 5
- Then dead-letter topic → worker/DLC handler creates/updates quarantine
- Idempotency key prevents double sink success on redelivery

Combined with in-route retries, total outbound attempts stay **bounded** (document the product of the two in operator UI).

## Circuit breaker

- Open → new ALLOW decisions that need that connector **quarantine at engine stage 9** (API sees last published circuit state in Firestore `connectors/{id}.circuit`).
- Worker updates circuit state after failures.
- Half-open: one probe from worker, not from public API.

## Safe reprocess

`OPERATOR` or `SECURITY_ADMIN` (Phase 0 lock: **OPERATOR** may reprocess retry-eligible items; **SECURITY_ADMIN** may reprocess after policy change).

Preconditions:

- `retry_eligible == true`
- current contract still supports the payload **or** admin activated a new version and revalidation passes
- connector circuit closed
- credential ACTIVE

Reprocess **replays the engine** on stored hashes + stored **minimized** payload if we kept transformed JSON. If we did not keep payload (Restricted), reprocess may require a **new producer delivery**. Calendar pointer style: re-fetch from Graph if still authorized.

Reprocess creates a **new** `transaction_id` linked via `supersedes` / `reprocess_of`. Audit chain records both.

No infinite retry. Manual reprocess count cap: 5.

## Recruiter UI

Quarantine list: reason, stage, eligibility, connector, age.  
Detail: safe diff for contract, rule ids for egress, no secrets.
