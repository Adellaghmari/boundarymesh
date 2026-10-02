# Application security

Project B covers OCI supply-chain tooling (Trivy/Syft/Cosign). BoundaryMesh’s differentiator is
**API, identity, integration, and deployment-boundary security**. This is not a claim of
comprehensive AppSec maturity.

Current claims: [CV_CLAIMS_MATRIX.md](../CV_CLAIMS_MATRIX.md).

## Implemented (evidence-backed)

- IAM-private core API; public edge is the Web BFF plus Firebase Hosting
- Firebase Google sign-in, HttpOnly `__session`, operator allowlist, CSRF, route allowlisting
- Local JWT/JWKS resource server; AuthN distinct from AuthZ
- HMAC-SHA256 (`MessageDigest.isEqual`), freshness window, replay nonce store
- Contract schema validation and destination egress allow/redact/deny
- Cloud KMS ECDSA P-256 **exchange attestation** (enforcement proof, not artifact signing)
- Secret Manager containers with resource-scoped accessor IAM; values never in Git
- Keyless WIF for CI/preview/publish/deploy/recovery; 0 user-managed keys on those SAs and on workload SAs
- Custom least-privilege Run roles for existing-resource CD
- Immutable release tags, digest pinning, Generic deployment evidence, collision-fail
- Hosting headers: CSP, `no-cache` SPA shell, immutable hashed `/assets/**`
- Fail-closed CD: split runtime/Hosting authority, no automatic promote/rollback
- Fail-closed evidence recovery that cannot mutate production

## Explicit gaps

- No SAST pipeline (Semgrep)
- No secret scanner in CI (Gitleaks)
- No DAST/ZAP
- No Dependabot / OWASP Dependency-Check in CI
- `roles/firebasehosting.admin` is broader than Hosting-only operations
- Web session custom role includes `firebaseauth.users.update`
- SBOM/provenance are **unsigned**
- Rollback not live-executed
- Operator workstation ADC is not keyless
- Firebase-managed Token Creator exists in the project
- Historical Pulumi passphrase exposure remains a hygiene follow-up

Optional scanners and Cosign are listed in [ROADMAP.md](ROADMAP.md). They are not required for
current portfolio/CV readiness.

## Public vs protected (current)

The hosted public surface is the Firebase SPA plus BFF-gated session/API rewrites. The core API
is not publicly invocable. Local `dev` labs remain a separate trusted-developer path.

Do not fuzz HMAC ingest or hosted session cookies against the live project.
