# Documentation map

Current project truth lives in a small set of reviewer-facing documents. Historical
phase notes remain as audit records; they are not current status. You do not need
Phase 15/16 numbering to understand the product.

## Current truth

| Document | Use for |
| --- | --- |
| [../README.md](../README.md) | Product thesis and portfolio entry |
| [../PROJECT_STATUS.md](../PROJECT_STATUS.md) | What is live |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Topology and service boundaries |
| [CI_CD.md](CI_CD.md) | Keyless WIF, publication, stage, promote, rollback, recovery |
| [PULUMI.md](PULUMI.md) | IaC, 83 managed resources, fenced CD-owned fields |
| [APPLICATION_SECURITY.md](APPLICATION_SECURITY.md) | Implemented controls and explicit gaps |
| [OBSERVABILITY.md](OBSERVABILITY.md) | Logs, health, metrics; no OpenTelemetry claim |
| [LOCAL_DEVELOPMENT.md](LOCAL_DEVELOPMENT.md) | JDK 25 / Node 22 local loop |
| [PRODUCT.md](PRODUCT.md) | Product definition and honest career purpose |
| [ROADMAP.md](ROADMAP.md) | Completed work and optional follow-ups |
| [../CV_CLAIMS_MATRIX.md](../CV_CLAIMS_MATRIX.md) | SAFE NOW / SAFE WITH QUALIFIER / NOT YET SAFE |

## Implementation detail (when you need depth)

| Document | Topic |
| --- | --- |
| [BOUNDARY_ENFORCEMENT_ENGINE.md](BOUNDARY_ENFORCEMENT_ENGINE.md) | Deterministic enforcement |
| [SECURITY_MODEL.md](SECURITY_MODEL.md) | Trust boundaries |
| [NETWORK_BOUNDARIES.md](NETWORK_BOUNDARIES.md) | Network hops |
| [IDENTITY_MODEL.md](IDENTITY_MODEL.md) | AuthN vs AuthZ |
| [SECRET_MODEL.md](SECRET_MODEL.md) | Credential vault |
| [TEST_STRATEGY.md](TEST_STRATEGY.md) | Test pyramid |
| [STACK.md](STACK.md) | Locked language/runtime versions |
| [COST_MODEL.md](COST_MODEL.md) | Portfolio cost model |
| [adr/README.md](adr/README.md) | Architecture Decision Records |

## Historical / not primary reviewer docs

Files named `docs/PHASE_*.md` are implementation and audit records for a specific gate.
They freeze the facts that were true **at that gate**, including resource counts and
“NOT STARTED” statements that later gates closed.

Read them as evidence, not as current status. If a historical freeze conflicts with
`PROJECT_STATUS.md` or the root README, those current docs win.

Planning scorecards in [SCORECARDS.md](SCORECARDS.md) and [JOB_MARKET.md](JOB_MARKET.md)
are Phase 0 artifacts. They are not live inventory.
