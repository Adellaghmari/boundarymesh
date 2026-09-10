# Microsoft 365 / Graph integration

Microsoft Graph is an **enterprise connector plugged into BoundaryMesh**, not the product.

```text
MICROSOFT ENTRA / MICROSOFT 365
        → Graph Calendar connector adapter
        → normalize m365-calendar/v1
        → identity / authorization evidence
        → Contract Drift Firewall
        → Data Boundary Firewall
        → Boundary Enforcement Engine
        → ALLOW / TRANSFORM / QUARANTINE / REJECT
        → audit + exchange attestation
```

Helix 365 remains a **fictional** Velora system. It is not Microsoft 365.

## Chosen depth

One bounded read: **Outlook Calendar** `GET /me/calendarView` (next 7 days, `$top<=5`).

Not in this phase: mail, Teams, OneDrive, SharePoint, users/groups, calendar write, or Graph change notifications.

## Permission mode

**Delegated `Calendars.Read`** (least privilege that still allows a signed-in operator demo).

Application `Calendars.Read` is not requested. Admin-consent application permissions are not faked.

Consent is mapped to the generic Phase 9 model: `ConsentGrant` + `Permission(Calendars.Read)` + `GrantKind.DELEGATED` + `ConnectorOperation.CALENDAR_READ`.

## SDK

Live adapter: official **Microsoft Graph Java SDK 6.67.0** in the API module only (`MicrosoftGraphCalendarAdapter`). Domain and application modules must not import `com.microsoft`.

WireMock tests use `GraphCalendarHttpAdapter` (JDK HttpClient) so `mvn verify` never calls Microsoft.

## Modes

| Mode | When | Label |
| --- | --- | --- |
| CONTRACT HARNESS | default; no tenant required | `CONTRACT_HARNESS` |
| NOT CONFIGURED | Entra env vars blank | Recruiter mode; harness still available |
| LIVE | Entra configured **and** `BOUNDARYMESH_GRAPH_MODE=LIVE` **and** a real Graph call succeeds | `LIVE_VERIFIED` only then |

Never label a WireMock or harness payload as LIVE.

## Normalization and policy

Normalized contract: `contracts/json-schema/m365-calendar.v1.schema.json` (`m365-calendar/v1`).

Egress: `contracts/policies/m365-calendar.egress.v1.json` destination `M365_CALENDAR`.

| Path | Action |
| --- | --- |
| `/event_reference` | ALLOW (SHA-256 of provider id) |
| `/start_time` | ALLOW |
| `/end_time` | ALLOW |
| `/availability` | ALLOW |
| `/subject` | REDACT |
| `/attendees` | DENY STRIP |
| `/online_meeting_url` | DENY STRIP |
| `/body` | DENY STRIP |

This is deterministic minimization, not a legal-compliance claim.

Recruiter and admin JSON responses do not include subjects, attendees, bodies, or tokens.

## Resilience

Graph reads use a bounded retry (max 3). `401`/`403` are not retried. `429` honors `Retry-After` (capped). `5xx` and transport timeouts are retryable then fail closed. Camel Resilience4j stays on the delivery worker; Graph is not a CRM delivery.

## Recruiter demo

`Connections → Microsoft 365 Calendar` explains the architecture without login.

`POST /api/v1/dev/microsoft-lab/harness-crossing` runs the harness through contract + egress + engine + audit + attestation.

Operator live pull (session + CSRF + `Calendars.Read`): `POST /api/v1/admin/microsoft/calendar/pull`.

## Graph change notifications

**PLANNED.** A live subscription needs a reachable HTTPS notification URL. This project is not on Cloud Run yet. No ngrok.
