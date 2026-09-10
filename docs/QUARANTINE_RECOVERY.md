# Quarantine and recovery

Flagship capability #6 — local implementation TESTED. Cloud-persistent implementation PLANNED.

Companion: [QUARANTINE.md](QUARANTINE.md) (original target design). This document records what Phase 10 actually runs.

## State machine

```text
OPEN ──beginReprocess──► REPROCESSING ──resolve──► RESOLVED
                         │
                         └──remain (still failing)──► OPEN
                         └──cap exhausted──► TERMINAL
```

`PENDING` / `DONE` are not used.

## Eligibility

Temporary provider failure, credential infrastructure outage, and circuit-open are eligible after conditions change.

Critical egress deny and security REJECT never enter this workflow.

Contract unsupported is eligible only after the registry would accept a new evaluation — reprocess re-runs the current pipeline rather than replaying the old approval.

## Authorization

`POST /api/v1/quarantine/{id}/reprocess` requires a JWT with `boundary.admin`. Anonymous and `boundary.evaluate`-only callers are denied.

## Failure classification (delivery)

| Provider signal | Retry | Terminal class |
| --- | --- | --- |
| 200 | no | DELIVERED |
| 400 | no | PERMANENT_FAILURE |
| 401 / 403 | no | QUARANTINED (credential/consent configuration) |
| 429 | yes, bounded Retry-After (capped) | QUARANTINED if exhausted |
| 500 / 503 / timeout | yes, bounded | QUARANTINED if exhausted |
| circuit OPEN | no external call | QUARANTINED, connectorSafety UNAVAILABLE |

## Responsibility chain

1. **Camel** — route, translate, select connector, DLC for unknown destination
2. **Resilience4j** — bounded provider call resilience
3. **Delivery service** — classify final `DeliveryResult`
4. **Quarantine Orchestrator** — record recoverable terminal state and reprocess
