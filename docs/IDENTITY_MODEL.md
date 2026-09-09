# Identity model

Flagship capability #1: **Identity Federation Gateway**.

Authentication answers **who**. Authorization answers **whether this who may perform this boundary crossing**.

## Three identity cases

### 1. Admin user (delegated)

- Protocol: OpenID Connect **authorization code + PKCE**
- Provider: Microsoft Entra ID
- Client: Spring Security OAuth2 Client on `boundary-api` (BFF)
- Browser: never stores access tokens (HttpOnly session cookie)
- Scopes: `openid profile`, plus the API’s app-role token
- App roles: `Operator`, `SecurityAdmin`
- Used for: connector config, rotation, quarantine reprocess, policy activation
- **Not used for:** Graph calendar connector calls

### 2. Machine connector (application)

- Protocol: OAuth 2.0 **client credentials**
- Provider: Entra ID tenant for Velora / Helix
- Token: JWT access token
- Validation on API: issuer, audience, exp, nbf, signature via JWKS, `tid` match
- Authorization: **app roles** or **roles** claim and integration ACL
- Used for: trusted machine ingress that is not a webhook

Example reject (must appear in UI):

```text
Identity:     VALID
Token:        VALID
Audience:     VALID
Required role / scope: MISSING
Result:       REJECT
Reason:       AUTHZ_MISSING_APP_ROLE
```

### 3. Inbound third party

Two sub-cases:

| Sub-case | Identity established by | Authorization |
| --- | --- | --- |
| QuayForge webhook | Shared HMAC secret reference + `X-Velora-Connector-Id` | Integration ACL for that connector |
| Graph notification | Subscription `clientState` + tenant/app validation per Graph rules | Subscription belongs to a registered integration |

Graph notifications are **not** generic HMAC. See [WEBHOOK_SECURITY.md](WEBHOOK_SECURITY.md).

## Entra token validation (machine + admin API)

| Check | Required | Failure code |
| --- | --- | --- |
| Signature vs JWKS | yes | `AUTHN_INVALID_JWT` |
| `iss` matches `https://login.microsoftonline.com/{tid}/v2.0` (v2) | yes | `AUTHN_WRONG_ISSUER` |
| `aud` matches API application ID URI or app ID | yes | `AUTHN_WRONG_AUDIENCE` |
| `exp` in future (leeway ≤ 60s) | yes | `AUTHN_EXPIRED` |
| `nbf` | yes | `AUTHN_INVALID_JWT` |
| `tid` allowlist | yes | `AUTHN_WRONG_ISSUER` (tenant) |
| JWKS reachable | yes | `AUTHN_JWKS_UNAVAILABLE` |

v1 vs v2 issuer URLs are configured **per integration**. Do not accept both accidentally.

## Authorization model

After authentication:

1. Map token `roles` / `scp` to `BoundaryPrincipal`.
2. Load `integrations/{id}.acl` — allowed principals, actions (`calendar.event.upsert`, `order.settlement.create`).
3. Require **both** token role/scope **and** ACL entry.

A valid Entra token for the wrong integration is `AUTHZ_INTEGRATION_DENIED`.

Scopes vs app roles:

- Client-credentials tokens typically carry **app roles**, not delegated `scp`.
- Admin user tokens carry app roles assigned to the user/group.
- Do not treat `scp: User.Read` as permission to cross a CRM boundary.

## User vs machine on Graph

| | Admin console | Calendar connector |
| --- | --- | --- |
| Identity | Delegated user | Application (`HelixCalendarConnector`) |
| Permission type | App roles on BoundaryMesh API | Graph **application** `Calendars.Read` |
| Secret | none in SPA | Secret Manager reference for client credential **or** federated credential later |
| Consent | user + admin consent for app roles | admin consent for Graph app permission |

Do not mix: the worker must not use the admin user’s token to call Graph (no on-behalf-of in v1).

## JWKS / key rotation

- Decoder uses Entra’s OpenID metadata.
- Key rotation: new `kid` appears; old `kid` remains until tokens expire.
- Tests: rotate stub JWKS in WireMock; in-flight tokens with old `kid` still verify until removed; unknown `kid` refreshes then fails closed.

## What the UI must show

For every transaction:

- identity subject **reference** (oid or appid hash, not a raw token)
- authentication method (`OIDC_BEARER`, `CLIENT_CREDENTIALS`, `HMAC`, `GRAPH_NOTIFICATION`)
- authentication result
- authorization result **separately**
- missing scope/role name when rejected (the **name**, not the token)

## Local / harness identity

Phase 3 ships a **local JWT issuer** (test keys in `src/test/resources`, never production KMS) and WireMock JWKS so Java tests do not need a real tenant.

Production Entra app registration is Phase 14/16 and is marked LIVE VERIFIED only after a real token is validated.
