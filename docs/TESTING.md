# Testing

Application tests run on a developer machine. There is no pull-request workflow that runs Maven, Vitest, or Playwright. That is a limit of the repository, not a hidden green check.

Suites are separate. Do not add their counts into one number.

## Backend

From `backend/`:

```powershell
.\mvnw.cmd verify
```

The default verify goal does not need Docker, Application Default Credentials, or Google network access. It covers the enforcement engine, HMAC, contract and egress policy, Camel routes, quarantine, audit, and attestation logic with local adapters.

Opt-in profiles exist for the Firestore emulator and for a narrow live Cloud KMS check. The live KMS profile signs synthetic input with the existing `exchange-attestation` key version. It does not create infrastructure and it does not persist documents. Leave those profiles out of a normal run.

## Frontend

From `frontend/`:

```powershell
npm run typecheck
npm run test
npm run test:e2e
```

Vitest covers the lab console, the public console, and the operator console. `npm run test:e2e` walks the local lab against the local Java API and does not set `VITE_APP_MODE=cloud`. `npx playwright test -c playwright.public.config.ts` builds the cloud bundle and walks the shared console at `/`, the `/explore` redirect, and the protected `/operator` page.

`npm run lint` includes oxlint findings in test mocks. Those findings are known. ESLint is configured to fail on warnings.

## Infrastructure and deploy scripts

`infra` has a Node test suite, including the Hosting rewrite assertions. `scripts/deploy` has a Node test suite for artifact verification, evidence, and fail-closed promotion rules. Neither suite applies infrastructure or deploys.

## What is not tested in CI

Semgrep, Gitleaks, ZAP, Dependabot, and a browser test against the live Hosting channel are not part of this repository. Do not fuzz the live session or HMAC endpoints.
