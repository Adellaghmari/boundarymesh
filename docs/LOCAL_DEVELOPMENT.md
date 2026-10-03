# Local development

Requires JDK 25 (Eclipse Temurin) and Node 22. Do not put secrets in Git or in `VITE_*` variables. Those values are public browser configuration.

Microsoft Entra and live Graph are optional. Copy [`.env.example`](../.env.example) only if you are configuring local Entra. Leave the values blank to keep the lab and the Graph contract harness.

## Backend

From `backend/`:

```powershell
.\mvnw.cmd verify
.\mvnw.cmd -pl worker -am spring-boot:run
.\mvnw.cmd -pl api -am spring-boot:run
```

`scripts/start-backend.ps1` starts the worker (`http://127.0.0.1:8081`) then the API (`http://127.0.0.1:8080`).

Default `mvnw verify` does not need Docker, ADC, or Google network access. Opt-in emulator and live KMS groups are documented in [TESTING.md](TESTING.md).

## Frontend

From `frontend/`:

```powershell
npm install
npm run typecheck
npm run test
npm run dev
```

`npm run lint` may report pre-existing oxlint findings in tests. Playwright (`npm run test:e2e`) starts the local Java backend and Vite.

`npm run dev` is the lab console. It is not the hosted site. A cloud build (`VITE_APP_MODE=cloud`) serves a public portfolio page at the root and the operator console from `operator.html`. Operator actions still require Google sign in. The lab routes stay out of that cloud build.

## Full stack helper

```powershell
.\scripts\dev-local.ps1
```

| Process | URL |
| --- | --- |
| Frontend | http://localhost:5173 |
| API | http://localhost:8080 |
| Worker | http://localhost:8081 |
| Health | `GET /api/v1/health` |
| Ready | `GET /api/v1/ready` |

Labs mint or sign tokens on the Java side. Do not paste tokens or HMAC secrets into the UI.

Production profile does not expose `/api/v1/dev/**`. Local labs are a trusted-developer path, not the hosted operator console.
