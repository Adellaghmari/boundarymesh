# ADR 0008 — Microsoft Graph Calendar and Entra ID

## Status

Accepted (Phase 14).

## Context

Need a flagship Microsoft enterprise connector without Azure hosting and without requiring a live tenant for CI.

## Decision

- **Entra ID**: operator OIDC BFF (`oauth2Login` Authorization Code). Machine JWT resource server stays.
- **Graph**: one deep connector — Outlook Calendar **read** via delegated `Calendars.Read`.
- **SDK**: Microsoft Graph Java SDK 6.67.0 in the API adapter only.
- **Harness default**: WireMock + synthetic calendar events. Never claim LIVE without a real Graph call.
- **Graph change notifications**: remain PLANNED until a public HTTPS endpoint exists.

## Why delegated Calendar read?

Least privilege that still allows a signed-in operator demo. Application permissions are not faked when admin consent is unavailable.

## Why not five Graph APIs?

Breadth without contract/egress/audit depth is a keyword move.

## Consequences

`mvn verify` and Playwright do not call Microsoft. Live tenant verification is a manual checkpoint.

The hosted operator session is Firebase Google sign-in, not Entra. Production Entra stays disabled. One local calendar pull against a personal account is the Graph evidence. Change notifications are still not implemented.
