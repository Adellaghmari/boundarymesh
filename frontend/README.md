# BoundaryMesh console

Vue 3 recruiter console for **BoundaryMesh**. Phase 2 talks to the local Java backend only.

`VITE_*` values are public browser configuration. Never put secrets there.

## Scripts

```powershell
npm install
npm run dev
npm run typecheck
npm run lint
npm test
npm run build
npm run test:e2e
```

Local API calls use the Vite proxy (`/api` → `http://127.0.0.1:8080`) when `VITE_API_BASE_URL` is empty.

## OpenAPI client choice

**Option A — manual typed client.** The backend currently exposes health, ready, and one synthetic evaluate endpoint. A generated OpenAPI client would add generator weight without contract value. Service modules are isolated so a later generated client can replace `src/api/` without rewriting views.

MSW is used only for frontend request-level unit tests. It is not WireMock and not a Java integration substitute.

## Persistence

Boundary events are **session memory only**. Refresh resets the list. Phase 2 has no event store.
