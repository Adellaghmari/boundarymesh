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

## Camel (Phase 8, worker module)

- Content-based router (CoveCRM / Lumenway / Rivertide / unknown → DLC)
- Lumenway translator uses approved JSON, not the original email
- Idempotent consumer / adapter store suppresses the second provider call
- Camel headers do not carry secret values
- WireMock simulates 200 / 400 / 401 / 403 / 429 / 500 / 503 / timeout

## Resilience4j (Phase 8)

- 200 / 400 / 401 / 403: no retry
- 429 / 500 / 503 / timeout: exactly 3 total attempts
- Circuit CLOSED → OPEN, OPEN fail-fast, HALF_OPEN probe
- Retry-After parsed and capped

## Credential / quarantine (Phases 9–10)

- Active, missing, revoked, rotated, unavailable references
- Consent missing vs credential missing
- Secret marker absent from API JSON
- Reprocess requires `boundary.admin`
- Security REJECT does not create a quarantine item
- State machine rejects invalid transitions

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

## Frontend unit (Phase 2)

Vitest + Vue Test Utils. MSW mocks HTTP for the typed API client only. MSW is not WireMock and is not a Java integration test stand-in.

## Hash chain and attestation (Phases 11–12)

- Valid chain verifies; metadata/timestamp/sequence/previous-hash tamper fails
- Removed, reordered, and foreign-chain entries fail
- Concurrent append does not reuse sequence
- Signed attestation verifies; semantic change / wrong key / corrupted signature fail
- UNSIGNED is explicit; secrets absent from artifacts

## Boundary graph (Phase 13)

- Configured nodes/edges without traffic
- Observed delivery/quarantine/reject update edge state
- Vue SVG selection, filters, stage overlay
- Playwright: delivered edge, quarantine hold, replay reject, tamper demo INVALID

## Frontend E2E

Playwright starts the local **worker + API** and the Vite app. It covers Delivery Lab, Credential Lab, audit-chain verification, attestation signature verification, isolated tamper demos, and graph updates for delivered / quarantined / rejected crossings. Entra/Graph/KMS remain later.

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
