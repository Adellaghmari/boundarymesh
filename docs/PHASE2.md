# Phase 2 implementation

Phase 2 is the **Vue 3 recruiter console** against the Phase 1 local Java backend. It is not Entra, Graph, Camel, or GCP.

## Verified toolchain

Recorded 2026-09-09 on this machine after `npm install` in `frontend/`.

Official `create-vue` 3.23 selected the Vue 3.5 / Vite 8 / Pinia 4 / Vue Router 5 / Vitest 4 / TypeScript 6 line. TypeScript 7.0.2 and Vitest 5.0.0 were on npm; Vue tooling did not select them.

| Tool | Installed |
| --- | --- |
| Node | **v22.14.0** |
| npm | **11.2.0** |
| Vue | **3.5.42** |
| TypeScript | **6.0.3** |
| Pinia | **4.0.3** |
| Vue Router | **5.3.1** |
| Vite | **8.2.2** |
| Vitest | **4.1.11** |
| Vue Test Utils | **2.5.0** |
| Playwright | **1.63.0** |
| MSW | **2.15.0** |
| Sass | **1.104.0** |
| @lucide/vue | **1.43.0** |

`create-vue` 3.23 prefers Node `^22.18.0 || >=24.12.0`. This machine built and tested on **22.14.0**.

## OpenAPI

Manual typed client (`src/api/`). One evaluate endpoint does not justify a generator. Layout keeps a later generated contract possible.

## MSW vs WireMock

- **MSW** — browser/Node request mocking for Vue unit tests.
- **WireMock** — planned later for Java connector integration tests.

They are not interchangeable.

## Persistence

No backend event store. The Boundary Events list is Pinia session state.

## Local run

```powershell
# terminal 1
cd backend
.\mvnw.cmd -pl api -am spring-boot:run

# terminal 2
cd frontend
npm run dev
```

Or `scripts/dev-local.ps1`.

Playwright starts the Java backend and the Vite app itself. Do not make CI depend on a human launching those processes.

## Not in Phase 2

OAuth2/OIDC, JWT/JWKS, Entra, Graph, HMAC, Camel, Firestore, Pub/Sub, Secret Manager, KMS, Pulumi, GCP, persistent quarantine, signed attestations, Integration Boundary Graph.
