# Phase 3 — Identity Federation Gateway

**Status: COMPLETE** (local JWT / JWKS resource server). Microsoft Entra ID remains **PLANNED**.

## What this phase proves

BoundaryMesh can answer **who is calling** and **whether that identity may request this crossing**.

Authentication and authorization stay separate:

| Check | Result | HTTP / domain |
| --- | --- | --- |
| Missing, malformed, expired, wrong issuer/audience, unknown kid, invalid signature, future nbf | Authentication cannot be established | **401** |
| Valid JWT, `boundary.evaluate` missing on `POST /api/v1/dev/evaluate` | Authenticated, not authorized at the HTTP layer | **403** |
| Valid JWT, `boundary.evaluate` missing on `POST /api/v1/identity/evaluate` | Identity **PASS**, authorization **FAIL** | **200 + REJECT** |
| Valid JWT with `boundary.evaluate` | Identity **PASS**, authorization **PASS** | **200 + ALLOW** (remaining synthetic controls pass) |

Spring Security owns HTTP admission. The Boundary Enforcement Engine owns domain `ControlVector.identity` and `ControlVector.authorization` after a request is admitted.

This is **not** a browser OIDC Authorization Code / BFF login. It is an OAuth 2.0 JWT resource server with JWKS verification against a local test issuer.

## Local issuer

- Asymmetric RSA-2048, RS256, `kid` on the JWS header
- In-process JWKS at `GET /api/v1/dev/jwks` (local/test only)
- Configurable issuer and audience (`boundarymesh.security.*`)
- Token scenarios minted by `POST /api/v1/dev/tokens` — do not paste JWTs into docs
- Key rotation keeps previously advertised keys until they age out of the JWKS source

No Entra tenant, no `login.microsoftonline.com`, no Graph.

## Principal model

Domain types live in `backend/domain/.../identity` and do not depend on Spring `Authentication`.

`JwtToPrincipalMapper` adapts a verified Spring Security `Jwt` to `BoundaryPrincipal`.

Phase 3 proves **APPLICATION** machine identity. `USER` and `SERVICE` exist for later Entra mapping.

Scopes: `boundary.read`, `boundary.evaluate`, `boundary.admin`.
