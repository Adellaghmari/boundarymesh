# Security model

BoundaryMesh is an **application-security and integration-security** product. Supply-chain container signing is not the center (that is Project B).

## Phase 3–4 local enforcement layers

| Layer | Owns | Does not own |
| --- | --- | --- |
| Spring Security | HTTP authentication, route authorization, security headers, CORS | Domain crossing outcomes |
| Boundary Enforcement Engine | `ControlVector` → `ALLOW` / `REJECT` / … | HTTP 401/403 admission |

**CSRF:** disabled on the **bearer JWT / HMAC** resource-server chain (`/api/**`, `/v3/**`). Cookie CSRF is **enabled** on the admin session chain (`/api/v1/admin/**`). Do not treat CSRF as globally off.

**CORS:** local origins only (`http://127.0.0.1:5173`, `http://localhost:5173`, `http://127.0.0.1:4173`). Credentials allowed for the session cookie via the Vite proxy. No `*`. Disallowed origins are rejected (HTTP 403) and do not receive `Access-Control-Allow-Origin`.

**TLS:** local HTTP is accepted. Cloud Run HTTPS is later. No fake local certificates.

**Public (local):** health, ready, OpenAPI docs, local JWKS/issuer, token mint (local/test), webhook lab helper, QuayForge webhook ingest (HMAC, not anonymous-arbitrary), GET audit chains, GET attestations, GET/POST attestation verify, GET boundary graph, local tamper-demo GETs.

**Protected (local):** `GET /api/v1/security/me` (authenticated), `POST /api/v1/identity/evaluate` (authenticated), `POST /api/v1/dev/evaluate` and delivery lab (`SCOPE_boundary.evaluate`). `POST /api/v1/quarantine/{id}/reprocess` requires `boundary.admin`. Production profile does not expose `/api/v1/dev/**`. There is no public audit write or public signing endpoint.

## Principles

1. **Fail closed** on authentication, authorization, authenticity, freshness, and replay.
2. **Separate AuthN from AuthZ.** A valid token can still be rejected.
3. **Least privilege** per workload identity and per Graph permission.
4. **No secret values** in Firestore, Git, frontend, logs, audit, or attestations.
5. **Public demo is read-only** except the labeled harness POSTs. Admin mutations require an Entra session + CSRF when Entra is configured. Live Entra remains unverified without tenant credentials.
6. **Provider-specific webhook rules stay provider-specific.**
7. **Deterministic policy only.** No AI classification of PII or risk.

## Public vs protected

| Surface | Identity | Capabilities |
| --- | --- | --- |
| Public recruiter APIs | anonymous + rate limit | GET demo data, GET transaction evidence, POST attestation verify (public key path) |
| Graph notification URL | none (Microsoft calls) | validation handshake + notification ingest; authenticity via Graph rules, not Entra user login |
| Partner webhook URL | HMAC headers | ingest only |
| Machine JWT URL | Local JWT/JWKS now; Entra client-credentials PLANNED | ingest only |
| Admin HTML `/admin` | Entra OIDC BFF session | configuration, rotation, reprocess |
| Admin APIs | same session + CSRF | mutations |

Public **verify attestation** is allowed because it uses the **local public key** (later: KMS public key). It does not change state and does not mint signatures.

Rate limiting on public POST ingest is a defensive control (Cloud Armor is **not** in the default cost model; application-level limits + Cloud Run max instance). If abuse appears later, Cloud Armor is an optional Phase 18 add with a cost ADR.

## Roles

Mapped from Entra **app roles** (not ad-hoc strings in Firestore):

| Role | Entra app role | Can |
| --- | --- | --- |
| `PUBLIC_VIEWER` | (none) | read synthetic/live-identity demo |
| `OPERATOR` | `Operator` | reprocess quarantine items that are retry-eligible; cannot rotate secrets or change policy |
| `SECURITY_ADMIN` | `SecurityAdmin` | rotate credentials, activate contract/egress versions, disable connectors |

Method security: `@PreAuthorize("hasAuthority('ROLE_SECURITY_ADMIN')")` on rotation and policy activation.

## Token handling

- Access tokens, refresh tokens, ID tokens, HMAC secrets, client secrets: never logged.
- Authorization header stripped from access logs.
- BFF session: `HttpOnly`, `Secure`, `SameSite=Lax` (or `Strict` if demo origin is single-site), Spring Session **in-memory is unacceptable in Cloud Run**. Session store: **Firestore-backed Spring Session** *or* encrypted cookie session. **Lock: signed/encrypted cookie session** (Spring Session does not require Redis). No Redis.

## Cryptography policy

| Use | Mechanism | Forbidden |
| --- | --- | --- |
| JWT | Spring Security Nimbus JwtDecoder + JWKS | handwritten JWT parse |
| HMAC | `Mac` + `MessageDigest.isEqual` constant-time | `String.equals` on signatures |
| Attestation | Cloud KMS `EC_SIGN_P256_SHA256` over RFC 8785 bytes | signing `json.toString()` |
| Audit chain | SHA-256 over canonical entry | “blockchain” claims |
| Passwords | N/A (Entra) | local password DB |

## Data classification

| Class | Examples | Persistence |
| --- | --- | --- |
| Secret | client secret, HMAC key, private key | Secret Manager / KMS only |
| Restricted | attendee email, settlement account refs | hashed or redacted in audit; transformed payload only in delivery instruction with minimization |
| Operational | reason codes, integration IDs, outcomes | Firestore, logs, metrics |
| Public demo | synthetic names, SYNTHETIC DEMO flag | Firestore + UI |

Do not persist raw before/after payloads when Restricted fields are present. Persist **field names, actions, and SHA-256 of canonical transformed body**.

## Workload IAM (target)

| Principal | Firestore | Pub/Sub | Secret Manager | KMS |
| --- | --- | --- | --- | --- |
| `sa-boundary-api` | RW app data | publish delivery | **none** | signer on attestation key |
| `sa-delivery-worker` | RW app data | subscribe + DLQ | accessor on connector secrets only | signer if needed |
| `sa-github-ci` | none | none | none | none (tests use emulators) |
| `sa-github-deploy` | none | none | none | none; Cloud Run + Artifact Registry + Pulumi |

Secret-level IAM, not `roles/secretmanager.secretAccessor` on the whole project.

## Microsoft Graph permission story

Flagship connector uses **application** permission `Calendars.Read` only.

Not requested: `Mail.Read`, `Files.Read.All`, `User.Read.All` (unless a later ADR justifies a second connector).

Delegated vs application is documented in the M365 UI: this connector is **application identity**, not a signed-in user reading another user's mailbox interactively.

Admin consent is required for application permissions. The product displays consent **metadata** (who/when/permission set), never a secret.

## JWKS rotation

Spring Security caches JWKS with TTL. On `kid` miss, refresh. If JWKS endpoint is down: **fail closed** (`AUTHN_JWKS_UNAVAILABLE`), do not skip signature checks.

## Logging redaction

Deny-list in the log encoder: `Authorization`, `cookie`, `client_secret`, `access_token`, `id_token`, `refresh_token`, `clientState` if it is a secret, HMAC headers, PEM blocks.

Allow-list context: `transaction_id`, `integration_id`, `connector_id`, `boundary_outcome`, `contract_version`, `reason_code`, `correlation_id`.

## Claims discipline

Correct:

- “Broken audit-chain continuity can be detected when verified.”
- “The attestation signature is valid for this canonical payload.”
- “This Graph path is contract-harness tested” vs “LIVE VERIFIED against tenant X.”

Incorrect:

- “The database cannot be changed.”
- “Immutable blockchain ledger.”
- “We proved the business data is true.”
- “Live Microsoft 365” without a real call.
