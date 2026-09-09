# Application security pipeline

Project B already covers OCI supply chain (Trivy/Syft/Cosign) heavily. Project C’s differentiator is **API + identity + application + integration security**.

## CI tools (planned)

| Tool | Why |
| --- | --- |
| **Semgrep** | SAST: JWT, SSRF, log injection, Spring misconfig |
| **Gitleaks** | secrets in git |
| **OWASP Dependency-Check** | Java CVE |
| **Dependabot** | npm + Maven |
| **OWASP ZAP** | DAST baseline on public demo |

Container scan (Artifact Registry / optional Trivy) may exist because Cloud Run ships containers. It is **not** the story.

## Application controls (product)

Implemented locally (Phases 3–4):

- Spring Security OAuth2 resource server (JWT/JWKS)
- HMAC-SHA256 + `MessageDigest.isEqual`
- Freshness window + replay nonce store
- Sanitized JSON 401/403
- Size-limited webhook ingest (65536)

Still planned:

- CSRF on future Entra BFF
- Implicit-deny egress engine
- Secret Manager references
- KMS signing
- Semgrep / Gitleaks / ZAP in CI

## ZAP scope

Public GETs + attestation verify. Do not fuzz HMAC ingest with production secrets. Staging/harness only.
