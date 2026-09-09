# Product

**Name:** BoundaryMesh  
**Tagline:** Secure every system crossing.  
**Owner:** Adel Laghmari — portfolio project, not professional employment.  
**Project:** C of three flagship portfolio projects. A and B are frozen.

## A. Product definition

BoundaryMesh is a **boundary enforcement product for system-to-system integrations**.

It intercepts an untrusted transaction that is attempting to leave one trust domain and enter another. Before the transaction continues, the product evaluates identity, authorization, authenticity, freshness, replay, contract compatibility, destination-specific data egress, credential resolvability, and connector safety. It then emits a **deterministic enforcement outcome**, optionally transforms the payload, routes or quarantines the work, delivers through enterprise integration patterns, and records **machine-verifiable evidence** of what was enforced.

The recruiter-facing impression that must be true:

> This developer built a cross-system security enforcement layer that controls identity, contracts, data movement and delivery, then produces cryptographically verifiable evidence of what was enforced.

### Unique product audit (must remain NO)

| Trap | Answer | Why BoundaryMesh is not that |
| --- | --- | --- |
| Just an API gateway? | **NO** | Gateways terminate TLS and route. BoundaryMesh owns contract versions, field-level egress, replay vs duplicate, quarantine as an operational state, and signed attestations of enforcement. |
| Just an integration dashboard? | **NO** | The UI explains enforcement. The product is the engine and the evidence, not charts of HTTP calls. |
| Zapier / iPaaS clone? | **NO** | iPaaS optimizes connectivity and workflow authoring. BoundaryMesh optimizes **whether a crossing is allowed** and **what proof exists**. |
| Project A without AI? | **NO** | Project A asks whether an AI recommendation is evidence-supported for a human. BoundaryMesh asks whether a machine transaction may cross a boundary. No LLM, no RAG, no human-in-the-loop as the core decision. |
| Project B on GCP? | **NO** | Project B is release risk, health, blast radius, rollback. BoundaryMesh is integration security. Pub/Sub is delivery decoupling, not an EventBridge/SQS retelling. |

## B. Product thesis

A **successful HTTP request does not prove** that one system should exchange data or actions with another.

The missing product is a deterministic enforcement layer that treats every external transaction as a **boundary crossing**, not as a successful webhook.

BoundaryMesh therefore asks:

**CAN THIS TRANSACTION CROSS THIS SYSTEM BOUNDARY?**

It does **not** ask:

- Is this information true?
- Should a human trust an AI answer?
- What changed in a release, and how risky is recovery?

The decision is **application/security policy**. It is not an AI recommendation, not fact-checking, and not a human approval workflow.

### Core execution flow

```text
UNTRUSTED SYSTEM TRANSACTION
  → AUTHENTICATE
  → AUTHORIZE
  → VERIFY AUTHENTICITY
  → CHECK FRESHNESS / REPLAY
  → VALIDATE CONTRACT
  → GOVERN DATA EGRESS
  → ENFORCE BOUNDARY
  → ROUTE / QUARANTINE / REJECT
  → DELIVER RESILIENTLY
  → RECORD AUDIT CHAIN
  → GENERATE CRYPTOGRAPHIC EXCHANGE ATTESTATION
```

### Enforcement outcomes

| Outcome | Meaning |
| --- | --- |
| `ALLOW` | All controls passed. Payload unchanged. Delivery may proceed. |
| `ALLOW_WITH_TRANSFORMATION` | Controls passed after deterministic redaction/transformation. Delivery uses the transformed payload. |
| `QUARANTINE` | Not a silent drop. The crossing is held: contract drift, connector unsafety, exhausted retry, missing secret. Operators may inspect safe metadata and reprocess. |
| `REJECT` | Fail closed. Authentication, authorization, authenticity, replay, or hard data-policy denial. |

No probabilities. No confidence scores. No generated verdicts.

## Six flagship differentiators

### 1. Identity Federation Gateway

Cryptographically valid identity is not permission. Entra / JWT / JWKS validation is authentication. Scopes, app roles, and integration ACLs are authorization. Both appear separately in API evidence and UI.

### 2. Webhook & Subscription Defense

Generic HMAC-SHA256 (timestamp, nonce, constant-time compare, replay window) is distinct from Microsoft Graph change-notification validation (subscription validation handshake, `clientState`, lifecycle notifications). Malicious replay is distinct from legitimate duplicate delivery.

### 3. Contract Drift Firewall

OpenAPI 3.1, AsyncAPI, and JSON Schema are **runtime artifacts**. Drift is an operational state (`QUARANTINE` + `CONTRACT_DRIFT`), not merely HTTP 400.

### 4. Data Boundary Firewall

Destination-specific allow / redact / deny. Explicit rules, not ML PII classification. Evidence records field actions without storing sensitive values.

### 5. Credential & Consent Vault

Google Secret Manager holds secrets. Firestore holds references, versions, rotation status, Entra consent metadata. Rotation is a demonstrated control.

### 6. Quarantine & Recovery Orchestrator

Timeouts, bounded retries, 429 Retry-After, circuit breaker, Pub/Sub redelivery, dead-letter, idempotent reprocess. Quarantine is a workflow with reason codes, not a trash folder.

## Synthetic integration organization

**Do not reuse Northstar Commerce or any Project B fiction.**

**Organization:** Velora Interchange  
**Marking:** every business record displays **SYNTHETIC DEMO**  
**Nature:** fictional B2B operations network that coordinates partner work across calendar, CRM, orders, settlements, and fulfillment. Not a real company. Not a real customer.

| System | Role | Security character |
| --- | --- | --- |
| **Helix 365** | Microsoft 365 tenant (Entra + Graph) | Entra application identity, Graph calendar notifications, least-privilege `Calendars.Read` |
| **CoveCRM** | CRM destination | Inbound activity records; strict egress (emails redacted, internal scores denied) |
| **QuayForge Orders** | Commerce / order platform | Signed partner webhooks (HMAC-SHA256) |
| **Rivertide Settlements** | Financial / payment-like destination | High-sensitivity egress; extra denylist; never a real payment processor |
| **Lumenway Fulfillment** | Analytics / fulfillment destination | Broader allowlist, still no secrets; used to show destination-specific policy |

### Flagship live-or-harness scenarios

1. **Helix 365 → CoveCRM** — Outlook calendar event (customer meeting created/updated) becomes a CRM activity after identity, contract, and egress checks.
2. **QuayForge → Rivertide** — signed order webhook becomes a settlement instruction after HMAC, replay, contract, and field denial checks.

These two connectors are enough. Additional Graph APIs are out of scope unless a later phase finds a genuine gap.

## Recruiter experience

Public, no signup:

- Overview
- Connections
- Boundary Events
- Quarantine
- Attestations
- Boundary Graph
- Microsoft 365 Connector
- Security Posture
- Architecture

Protected (Entra OIDC, Spring Security BFF session):

- connector configuration
- credential rotation
- quarantine reprocess
- policy/contract version activation
- administrative operations

### One-minute wow path

A Helix 365 calendar transaction appears:

| Control | Result |
| --- | --- |
| Identity | VERIFIED |
| Authorization | VERIFIED |
| Contract | VALID |
| Replay | CLEAR |
| Data boundary | 2 FIELDS REDACTED |
| Delivery | SUCCESS |
| Boundary outcome | `ALLOW_WITH_TRANSFORMATION` |
| Exchange attestation | SIGNATURE VALID |

Then the same shape with wrong audience → `REJECT`.  
Then the same signed webhook outside the replay window → `REJECT`.  
Then unsupported schema version → `QUARANTINE`.

A transaction detail page must answer the questions in [docs/API_CONTRACTS.md](API_CONTRACTS.md) without exposing secrets or raw tokens.

## Truth boundaries

**SYNTHETIC DEMO:** Velora systems, payloads, failures, transactions.  
**LIVE PROJECT DATA (later phases only):** Git SHA, CI, GCP revision, Pulumi stack, AppSec scan results, real Graph calls **only if executed**, real KMS verify, real Pub/Sub, real Cloud Run.

Never blur them. Never claim live Microsoft 365 behavior from WireMock.

## Career purpose (honest)

Portfolio evidence for Integration Engineer, Java Backend, Technical Solutions / Implementation, API, IAM-aware software, Cloud (GCP), and Microsoft-ecosystem integration conversations.

Never claim: professional security job, paid M365 consulting, enterprise production ownership, years of IAM or Java employment.
