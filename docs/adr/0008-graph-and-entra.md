# ADR 0008 — Microsoft Graph Calendar and Entra ID

## Status

Accepted.

## Context

Need a flagship Microsoft enterprise connector without Azure hosting. No dependency on a free M365 developer tenant.

## Decision

- **Entra ID**: admin OIDC BFF; machine client-credentials JWT; app roles.
- **Graph**: one deep connector — **Outlook Calendar** read + change notifications.
- **LIVE** vs **CONTRACT-HARNESS** (WireMock + Pact + real schemas). Never claim live without a real call.

## Why Graph?

Demonstrates Entra application identity, least-privilege `Calendars.Read`, subscription lifecycle, egress of attendee emails into CoveCRM. Cross-cloud: M365 → Java → GCP.

## Why not five Graph APIs?

Breadth without subscription/egress depth is a keyword move.

## Why Entra?

Real IAM for admin mutations and machine ingress. Login is not decoration: protected routes 401/403 tested.

## Consequences

Harness is the default production-quality path. Live tenant is a bonus badge.
