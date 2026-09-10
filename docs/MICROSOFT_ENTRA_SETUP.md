# Microsoft Entra setup (local)

This is the operator / security-admin **OIDC Authorization Code** path. It does **not** replace the existing machine JWT resource server.

Do not paste client secrets into chat, Git, or screenshots.

## Security model

| Identity | Protocol | Session | CSRF |
| --- | --- | --- | --- |
| Machine / API | Bearer JWT + JWKS (local issuer) | Stateless | Disabled on `/api/**` resource-server chain |
| Operator browser | Microsoft Entra OIDC via Spring `oauth2Login()` | HttpOnly session cookie | Enabled on `/api/v1/admin/**` |

The Vue app never receives Graph access tokens, ID tokens, or refresh tokens. Tokens stay in the API process / server session.

Localhost cookies: `HttpOnly`, `SameSite=Lax`, `Secure=false`. Production must set `Secure=true` behind HTTPS.

Redirect URI for local Vite BFF:

```text
http://127.0.0.1:5173/login/oauth2/code/entra
```

Vite proxies `/oauth2`, `/login`, `/logout`, and `/api` to `http://127.0.0.1:8080`.

## App registration (human)

In Microsoft Entra admin center:

1. Register an application (single-tenant is enough for a personal demo).
2. Authentication → add a **Web** platform redirect URI: `http://127.0.0.1:5173/login/oauth2/code/entra`.
3. Certificates & secrets → create a **client secret**. Store it only in local OS environment configuration.
4. API permissions → **Microsoft Graph** → **Delegated** → `Calendars.Read` only. Grant admin consent if the tenant requires it.
5. Do **not** add `Directory.ReadWrite.All`, `Mail.ReadWrite`, `Files.ReadWrite.All`, or `Calendars.ReadWrite`.

## Local environment (names only)

Set these in the OS or a Git-ignored local file. Do not commit values.

```text
BOUNDARYMESH_ENTRA_TENANT_ID
BOUNDARYMESH_ENTRA_CLIENT_ID
BOUNDARYMESH_ENTRA_CLIENT_SECRET
BOUNDARYMESH_ENTRA_REDIRECT_URI=http://127.0.0.1:5173/login/oauth2/code/entra
BOUNDARYMESH_FRONTEND_ORIGIN=http://127.0.0.1:5173
BOUNDARYMESH_GRAPH_MODE=HARNESS
```

Leave them unset for Recruiter mode. The Graph **contract harness** still runs without Entra.

Live Graph reads use `BOUNDARYMESH_GRAPH_MODE=LIVE` with the three Entra values configured. A Graph HTTP 2xx on the Operator calendar pull is the only path to `LIVE_VERIFIED`. Local evidence includes one real event crossing (`m365-calendar/v1`, `m365-calendar-egress/v1`) recorded in PROJECT_STATUS.md. The contract harness is not live evidence.

## Login / logout

- Sign-in URL: `/oauth2/authorization/entra`
- Logout: `GET /logout` (local only; clears the session cookie)
- Success redirect: `/connections`

## Troubleshooting

| Symptom | Likely cause |
| --- | --- |
| Header shows Recruiter mode / Sign in unavailable | Entra env vars are blank |
| `AADSTS50011` | Redirect URI does not match the app registration |
| `AADSTS65001` | Delegated `Calendars.Read` consent missing |
| 403 on admin pull | Session lacks `SCOPE_Calendars.Read` |
| 403 on admin POST | Missing `X-XSRF-TOKEN` (GET `/api/v1/admin/session` first) |

Graph change notifications are **not** configured here. They remain planned until a public HTTPS endpoint exists.
