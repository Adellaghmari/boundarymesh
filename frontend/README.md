# BoundaryMesh console

Vue 3 console for **BoundaryMesh**.

- **Local** (`npm run dev`): lab routes against the Java API on this machine.
- **Hosted**: a separate operator SPA (`VITE_APP_MODE=cloud`) on Firebase Hosting. Sign-in required. Not a public recruiter demo.

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
