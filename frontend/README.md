# BoundaryMesh console

Vue 3 console for **BoundaryMesh**.

- **Local** (`npm run dev`): the lab console against the Java API on this machine. This is the default when `VITE_APP_MODE` is unset.
- **Hosted root** (`VITE_APP_MODE=cloud`): the same console at `/`, backed by synthetic data. No sign in and no private API call. `/explore` redirects to `/`. `/about` is About the project.
- **Hosted operator page** (`operator.html`, routed from `/operator`): the existing operator console. Google sign in and a web edge session are required.

`VITE_*` values are public browser configuration. Never put secrets there.

Full commands: [docs/LOCAL_DEVELOPMENT.md](../docs/LOCAL_DEVELOPMENT.md).

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

MSW is used only for frontend request-level unit tests. It is not WireMock and not a Java integration substitute.
