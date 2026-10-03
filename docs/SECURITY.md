# Security

BoundaryMesh is an application and integration boundary. It is not a container-signing product. Cloud KMS signs exchange attestations. It does not sign release artifacts.

## Authentication and authorization

The hosted operator signs in with Firebase Google authentication. The web edge stores an HttpOnly `__session` cookie, checks an operator allowlist, requires CSRF on mutating calls, and allowlists routes. The browser does not receive a workload credential.

The core API is a separate IAM-private Cloud Run service. Only the web edge service account may invoke it.

Local labs use a JWT and JWKS resource server. Authentication and authorization are different stages. Authorization checks scopes, including `SCOPE_Calendars.Read` on the local admin chain.

Local Microsoft administration uses an Entra authorization-code session. That path is not the hosted operator session. Production Entra is disabled. See [IDENTITY_AND_ACCESS.md](IDENTITY_AND_ACCESS.md).

## Session model

Hosted session cookies are set by the web edge. Firebase Hosting forwards `__session`. Operator actions fail closed when the session probe fails.

Local admin routes use a separate Spring Security chain with CSRF and a cookie session. The API resource-server chain is stateless. CSRF is off on that chain because callers present a bearer credential or HMAC, not a browser cookie.

## Trust boundaries

| Boundary | What crosses it |
| --- | --- |
| Browser to Hosting | Static files, then session and API rewrites |
| Web edge to API | Allowlisted calls with a service identity token |
| API to Firestore, KMS, Pub/Sub | Workload identity, no user credential |
| Worker to Secret Manager | Named connector secrets only, and only when delivery needs them |
| Worker to a partner | Not configured on the hosted path |

HMAC-SHA256 uses a constant-time comparison. Freshness uses a timestamp window. Replay uses a nonce store. Contract validation and egress policy can allow, redact, or deny fields. A failed control does not become an allow.

## Network boundaries

There is no load balancer, Cloud NAT, or private VPC connector in the current design. Firebase Hosting and the public web edge are the internet edge. The API ingress is internal and IAM-authenticated. The worker pool has no ingress.

Do not treat the local lab as a network exposure of production. Production profiles do not serve `/api/v1/dev/**`.

## Secrets

Secret values are not in Git. `.env.example` contains placeholders. Firestore stores secret resource names.

Secret Manager holds named containers. Accessor IAM is per secret. The hosted worker stopped before connector-secret retrieval on the verified synthetic transaction. Secret Manager IAM is live. A hosted connector-secret read has not been exercised.

CI, publication, deployment, and the three workload service accounts have no user-managed keys. A Firebase-managed identity in the project holds Token Creator. That role is outside the keyless CI and workload claim. Operator workstation access uses Application Default Credentials. That path is not keyless.

Current Pulumi state secrets use Cloud KMS. An earlier passphrase exposure remains a hygiene follow-up. It is not a claim of complete credential history.

## Webhook security

Partner webhooks are authenticated with HMAC, not with a browser session. The QuayForge contract uses `X-Velora-Connector-Id`, `X-Velora-Timestamp`, `X-Velora-Nonce`, and `X-Velora-Signature`. The signature is HMAC-SHA256 over the UTF-8 string `v1:{timestamp}:{nonce}:{sha256hex(rawBody)}`. The header value is `v1=` plus lowercase hex. The raw body is hashed. JSON is not reserialized before verification. Comparison uses `MessageDigest.isEqual`.

A timestamp outside the freshness window, or a reused nonce, is a reject. A new nonce with the same event id is a duplicate delivery, not a replay. Invalid signature, expired timestamp, and replay are not delivery failures and are not published to Pub/Sub. The body limit is 65536 bytes.

Microsoft Graph change notifications are not implemented. Do not invent an HMAC wrapper for Graph. Do not send probe traffic at the live webhook or session endpoints.

## Data protection

Egress policy is part of the decision. The local Graph calendar event required redaction: subject redacted, and attendees, body, and join URL denied. The live adapter selects only id, start, end, and showAs from Graph.

The audit chain is SHA-256 over enforcement records. It detects tampering of the chain the application stores. It is not an immutable storage guarantee.

Attestations are canonicalized and signed with Cloud KMS ECDSA P-256. Verification uses the retrieved NIST P-256 public key. A signing failure returns unsigned and still preserves the crossing outcome.

## Deployment controls

Publication, staging, promotion, rollback, and evidence recovery use separate Workload Identity Federation contexts. Promotion evidence is written only after promotion completes. Recovery can publish a missing evidence document. It cannot promote, roll back, or redeploy. Detail: [CI_CD.md](CI_CD.md).

Hosting responses set a content security policy. The SPA shell is not cached. Hashed assets are immutable.

`roles/firebasehosting.admin` is broader than Hosting-only operations. The web session custom role includes `firebaseauth.users.update`. Those grants are wider than the ideal minimum.

## Known limitations

- No Semgrep, Gitleaks, ZAP, Dependabot, or OWASP Dependency-Check pipeline.
- SBOM and provenance are published and unsigned.
- Rollback has not been executed against live traffic.
- Frontend tests still have oxlint findings in mocks.
- Frontend `npm audit` reports high findings on the Firebase and `@grpc` path.
- Infrastructure packages have three transitive audit findings.
- The deploy script audit is clean.

This repository does not claim zero vulnerabilities, zero trust, or a fully hardened platform.
