# Test strategy

Serious pyramid. No “we will test later.”

## Unit (fast, default CI)

- Domain: reason codes, decision table, RFC 8785 canonicalization golden files
- Egress policy reducer
- Replay window logic (in-memory clock)
- HMAC compute + `isEqual`
- Hash chain link function
- Vue components: outcome chips, graph view-model mapping (Vitest)

JUnit 5. Mockito only at adapter edges.

## Slice / Spring Security

- JWT: valid, expired, iss, aud, missing role
- CSRF on admin POST
- Anonymous reprocess → 401
- Operator cannot rotate → 403
- Public GET transactions 200

## Camel

- Content-based router
- Translator drops denied fields
- DLC after failures
- Idempotent consumer second message

## Contract

- JSON Schema fixtures: valid, missing required, type change
- WireMock Graph: 200/401/403/429/500 + notification shapes
- Pact: GraphCalendarClient
- OpenAPI examples match controllers (springdoc + snapshot)

## Resilience

- Retry-After honored
- 401 not retried
- Circuit opens after configured failures
- Time limiter

## Integration (Testcontainers)

- Firestore emulator: nonce create-if-absent
- Pub/Sub emulator: publish after persist, duplicate delivery
- KMS: **use test double** locally; live KMS only in LIVE VERIFIED phase

## Frontend E2E

Playwright: wow path, reject audience, replay, quarantine, attestation verify page, admin 401 without login.

## Live (Phase 18)

- Public smoke GET /health, /overview
- Real Cloud Run revision
- Real KMS verify if key exists
- Graph **only if** tenant path marked live

## AppSec in CI

Semgrep, Gitleaks, OWASP Dependency-Check, Dependabot. ZAP baseline against public URL after deploy (Phase 17/18).

## Coverage intent

Engine decision table: every reason code in [FAILURE_MODEL.md](FAILURE_MODEL.md) has a test. Not a vanity 100% line target.

## Explicit non-goals

- Load test as a Phase 0 deliverable (optional k6 later, not Project B duplication)
- Testing Microsoft’s Graph service itself
