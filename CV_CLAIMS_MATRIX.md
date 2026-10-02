# CV claims matrix

**Rule:** promote only what evidence supports. This file is the internal claims boundary for
portfolio wording. BoundaryMesh is portfolio engineering in `boundarymesh-dev`, not professional
employment or enterprise production ownership.

Statuses below use three classes:

- **SAFE NOW** — true without extra caveats beyond “portfolio/dev GCP project”
- **SAFE WITH QUALIFIER** — true only with the stated limit
- **NOT YET SAFE** — do not claim

Application detail (`TESTED`, `LIVE VERIFIED LOCALLY`) still requires dated evidence in
[PROJECT_STATUS.md](PROJECT_STATUS.md).

## Claims boundary

| Claim | Class | Qualifier |
| --- | --- | --- |
| GCP infrastructure | **SAFE NOW** | `boundarymesh-dev` / `europe-west4` |
| Pulumi IaC | **SAFE NOW** | 83 managed; CD-owned traffic fenced |
| GitHub Actions CD | **SAFE NOW** | manual `workflow_dispatch` on `main` |
| GitHub Actions application CI | **SAFE WITH QUALIFIER** | local Maven/Vitest/Playwright; no PR workflow |
| Workload Identity Federation | **SAFE NOW** | 7 exact contexts |
| Keyless CI/CD/workloads | **SAFE NOW** | 0 user-managed keys on those SAs; workstation ADC exists; Firebase-managed Token Creator exists |
| Least-privilege IAM | **SAFE WITH QUALIFIER** | `firebasehosting.admin` and Firebase `users.update` are broader than the ideal minimum |
| Cloud Run | **SAFE NOW** | private API, public BFF, Worker Pool |
| Firebase Hosting | **SAFE NOW** | |
| Artifact Registry | **SAFE NOW** | |
| KMS | **SAFE NOW** | attestation + Pulumi state secrets |
| Secret Manager | **SAFE WITH QUALIFIER** | synthetic versions; hosted connector secret read not exercised |
| Pub/Sub | **SAFE WITH QUALIFIER** | no dead-letter policy |
| Firestore | **SAFE NOW** | |
| Immutable releases | **SAFE NOW** | digest-pinned; no rebuild on deploy |
| SBOM / provenance | **SAFE WITH QUALIFIER** | published, **unsigned** |
| Deployment evidence | **SAFE NOW** | |
| Rollback | **SAFE WITH QUALIFIER** | implemented, live-readable, **not live-executed** |
| Recovery | **SAFE NOW** | break-glass promotion-evidence recovery |
| Production deployment | **SAFE NOW** | portfolio/dev production only |
| Security engineering | **SAFE WITH QUALIFIER** | app-sec controls real; no SAST/DAST pipeline |
| DevOps | **SAFE NOW** | |
| Full-stack | **SAFE NOW** | |
| Distributed / event-driven | **SAFE WITH QUALIFIER** | one internal bus; external connectors not live |
| Software artifact signing | **NOT YET SAFE** | |
| OpenTelemetry / alerting | **NOT YET SAFE** | |
| Enterprise Graph notifications | **NOT YET SAFE** | |
| Public recruiter demo / paid production | **NOT YET SAFE** | |
| Complete credential hygiene | **NOT YET SAFE** | historical passphrase follow-up |

## Acceptable wording

**ACCEPTABLE**

- “Built and deployed a GCP-based full-stack system using Cloud Run, Firebase Hosting, Pub/Sub, Firestore and Pulumi.”
- “Implemented keyless GitHub Actions deployments with Workload Identity Federation and scoped service accounts.”
- “Built immutable release and deployment-evidence workflows with digest-pinned artifacts, SBOM/provenance publication and promotion recovery.”

**QUALIFIED**

- “Designed rollback workflows with preserved and verified rollback targets.”
- “Published SBOM and provenance evidence; the provenance is unsigned.”

**NOT ACCEPTABLE**

- “Proven zero-downtime rollback” — rollback was not live executed.
- “Implemented OpenTelemetry tracing” — no OTel dependency or exporter exists.
- “Signed software supply chain” — SBOM/provenance are unsigned. Cloud KMS signs **exchange attestations**, not release artifacts.
- “Enterprise production platform” — this is the portfolio/dev environment.
- “Fully automated CI/CD” — mutation is human-gated; application CI is local.
- “Zero-trust architecture” / “fully hardened” / “complete observability” — not evidenced.

## Capability evidence (detail)

| Capability | Status | Notes |
| --- | --- | --- |
| Vue 3 / TypeScript / Pinia / Vue Router / SCSS / Vite / Vitest | **TESTED** | |
| Playwright | **TESTED** | local Java backend |
| Integration Boundary Graph | **TESTED** / **LIVE VERIFIED LOCALLY** | not a blast-radius graph; Helix 365 SYNTHETIC |
| Java 25 / Maven / Spring Boot 4.1.1 / Spring Security | **TESTED** | |
| Identity Federation Gateway | **TESTED** / **LIVE VERIFIED LOCALLY** | hosted operator is Firebase session, not production Entra |
| Microsoft Graph Calendar | **LIVE VERIFIED LOCALLY** | personal Microsoft account; not enterprise Exchange Online |
| Graph notifications | **NOT YET SAFE** | |
| Webhook HMAC-SHA256 | **TESTED** | QuayForge |
| Contract Drift / Data Boundary Firewalls | **TESTED** / **LIVE VERIFIED LOCALLY** on one Graph event | |
| Apache Camel + Resilience4j | **TESTED** | worker only |
| Hosted multi-control crossing | **LIVE VERIFIED** | one synthetic browser transaction; connectors not called |
| Google Secret Manager | **LIVE VERIFIED LOCALLY** / IAM live | hosted connector secret not read |
| Firestore / outbox / Pub/Sub | **LIVE VERIFIED** | no `deadLetterPolicy` |
| Cloud Run API + Worker + Web BFF | **LIVE VERIFIED** | promoted 16D release |
| Cloud KMS exchange attestation | **LIVE VERIFIED** | signs enforcement proof, not OCI |
| SHA-256 audit chain | **LIVE VERIFIED** | not immutable storage |
| Pulumi TypeScript | **LIVE VERIFIED** | 83 unchanged |
| GitHub Actions + GCP WIF | **LIVE VERIFIED** | 7 contexts |
| Immutable artifact publication | **PUBLISHED / LIVE VERIFIED** | unsigned SBOM/provenance |
| Controlled CD | **LIVE VERIFIED** | stage + promote; rollback not executed |
| Promotion-evidence recovery | **LIVE VERIFIED** | break-glass |
| Firebase Hosting + BFF session | **LIVE VERIFIED** | |
| Micrometer | **TESTED** | local counters; not GCP-exported |
| Cloud Logging | **LIVE VERIFIED** as platform logs | no BM alert policies |
| Semgrep / Gitleaks / Dependency-Check / ZAP / Dependabot | **NOT YET SAFE** | optional |
| Public recruiter demo | **NOT YET SAFE** | |
| Software artifact signing | **NOT YET SAFE** | |
