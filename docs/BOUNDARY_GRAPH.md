# Integration Boundary Graph

The **Integration Boundary Graph** is the recruiter-facing visual system. It is **not** a static architecture diagram. It renders **actual application state** for a selected transaction or live connector pair.

## Purpose

In one view:

- source and destination
- identity mechanism
- authorization result
- contract version
- credential **reference** (not secret)
- boundary outcome
- connector status

## Visual language

Nodes: systems (Helix 365, BoundaryMesh, CoveCRM, …).  
Edges: a crossing.  
BoundaryMesh expands into **control chips**:

```text
HELIX 365
    │  Entra application identity
    ▼
BOUNDARYMESH
    ├ Identity        VERIFIED
    ├ Authorization   ALLOWED
    ├ Authenticity    VALID
    ├ Replay          CLEAR
    ├ Contract        VALID  v1.2.0
    ├ Egress          2 REDACTIONS
    ├ Credential      ref …/versions/3
    ├ Connector       HEALTHY
    ▼
COVECRM
    outcome: ALLOW_WITH_TRANSFORMATION
```

Reject example:

```text
QUAYFORGE
    ▼
BOUNDARYMESH
    └ REPLAY DETECTED
    ▼
REJECT     (no edge into Rivertide)
```

Color: outcome only (allow / transform / quarantine / reject). Do not use red/green as the only encoding — include labels for accessibility.

## Data source

`GET /api/v1/transactions/{id}/graph` returns a **view model**:

```text
nodes[]  { id, kind, label, synthetic: true }
edges[]  { from, to, outcome }
controls[] { key, result, detail }
badge    SYNTHETIC DEMO | LIVE PROJECT
```

No tokens. No emails. No HMAC signatures.

The overview page can show an **aggregate** graph of current connectors (status + last outcome). Transaction detail shows the **instance** graph.

## Implementation (Phase 13)

Vue 3 + SVG (preferred) or Canvas. Pinia `graph` store. No GraphViz server. Layout: layered left-to-right, BoundaryMesh center.

If a library is added, it must be for layout math only, not to replace the product visual identity.

## Wow path

Default seed transaction is the calendar → CRM success with two redactions, then toggle to audience-fail and replay-fail comparators on the same page.

## Non-goals

- Editing integrations by dragging nodes
- Displaying real customer networks
- Copying Project B’s Change Impact Graph (that is blast radius of **releases**)
