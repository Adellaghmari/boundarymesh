# Documentation map

Current project truth lives in a small set of reviewer-facing documents. Historical
phase notes remain as audit records; they are not current status.

## Current truth

| Document | Use for |
| --- | --- |
| [../PROJECT_STATUS.md](../PROJECT_STATUS.md) | What is live, what is closed, what is optional |
| [../README.md](../README.md) | Product thesis and concise current state |
| [../CV_CLAIMS_MATRIX.md](../CV_CLAIMS_MATRIX.md) | SAFE NOW / SAFE WITH QUALIFIER / NOT YET SAFE |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Topology and service boundaries |
| [CI_CD.md](CI_CD.md) | Keyless WIF, publication, stage, promote, rollback, recovery |
| [PULUMI.md](PULUMI.md) | IaC, 83 managed resources, fenced CD-owned fields |
| [APPLICATION_SECURITY.md](APPLICATION_SECURITY.md) | Implemented controls and explicit gaps |
| [OBSERVABILITY.md](OBSERVABILITY.md) | Logs, health, metrics; no OpenTelemetry claim |

## Historical / not primary reviewer docs

Files named `docs/PHASE_*.md` are implementation and audit records for a specific gate.
They freeze the facts that were true **at that gate**, including resource counts and
“NOT STARTED” statements that later gates closed.

Read them as evidence, not as current status. If a historical freeze conflicts with
`PROJECT_STATUS.md`, `PROJECT_STATUS.md` wins.

Architecture Decision Records in [adr/](adr/README.md) remain accepted design decisions
unless a later ADR supersedes them.
