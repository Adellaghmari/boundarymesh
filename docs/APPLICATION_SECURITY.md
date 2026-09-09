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

- Spring Security OAuth2
- HMAC constant-time
- Replay window
- CSRF on BFF
- Problem+json no stack traces in prod
- Size-limited ingest
- Implicit-deny egress
- Secret references
- KMS signing

## ZAP scope

Public GETs + attestation verify. Do not fuzz HMAC ingest with production secrets. Staging/harness only.
