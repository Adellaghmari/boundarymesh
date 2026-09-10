# Integration Boundary Graph

The graph answers: which systems are connected, which security boundaries exist, which identity/contract/data/credential controls apply, and what recent outcomes occurred.

It is **not** Project B’s dependency blast-radius graph, a release timeline, or a static architecture SVG.

## Semantics

| Piece | Meaning |
| --- | --- |
| Node | Configured synthetic system or the BoundaryMesh control plane |
| Edge | Configured integration crossing (source → destination, mediated by BoundaryMesh) |
| CONFIGURED | Integration exists; no recent observed crossing |
| ACTIVE | Recent observed allow/transform/delivery |
| BLOCKED | Recent REJECT |
| QUARANTINED | Recent hold or open quarantine for that destination |
| NO_RECENT_ACTIVITY | Helix 365 remains a fictional unused connector |
| CONFIGURED | Microsoft 365 Calendar edge until a harness/live crossing is observed |

Configured ≠ delivered. Observed crossings update recent status.

## Data source

`BoundaryGraphAssembler` derives nodes and edges from:

- configured Velora integrations (QuayForge, Helix 365, CoveCRM, Lumenway, Rivertide)
- Microsoft 365 Calendar → BoundaryMesh (`m365-calendar`)
- credential/consent/contract/policy metadata
- `ObservedCrossingStore` (latest crossing per destination from the live pipeline)
- open quarantine counts

Vue does not hardcode edges as the source of truth. It renders `GET /api/v1/boundary-graph`.

## Visual

Custom Vue SVG. No graph library. Selecting an edge shows identity, contract, egress policy, credential reference, consent, connector route, recent outcome, delivery, audit chain, and attestation links, plus the nine ControlVector stages from the linked attestation.

Recruiter demo on `/boundary-graph`:

1. QuayForge → CoveCRM delivery → ACTIVE / DELIVERED / SIGNED
2. Contract drift → QUARANTINED
3. Replay reject → BLOCKED / no delivery

## Differentiation

Project B graph: upstream/downstream service blast radius for releases.

BoundaryMesh graph: system-to-system **security boundaries**. No reverse-BFS, no rollback impact.

## Storage truth

Derived from local in-memory repositories. Does not survive restart. Firestore PLANNED.
