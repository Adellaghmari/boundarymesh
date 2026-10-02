# Roadmap

Current status: [PROJECT_STATUS.md](../PROJECT_STATUS.md). Historical `docs/PHASE_*.md` files are gate records, not this roadmap.

## Completed

Major implemented milestones, without reprinting every internal gate:

| Area | Outcome |
| --- | --- |
| Product / architecture lock | Java 25, Spring Boot 4.1, Vue 3, Camel on the worker, GCP `europe-west4`, Pulumi, no AI |
| Local enforcement | Identity, HMAC webhooks, contract drift, data-boundary firewall, composed engine |
| Delivery | Camel EIPs, Resilience4j, local credential/consent, quarantine reprocess, audit chain, KMS/JCA attestation, boundary graph |
| Microsoft | Entra OIDC + one Graph calendar event **LIVE VERIFIED LOCALLY**; production Entra disabled; Graph notifications not implemented |
| Cloud foundation | Private API, Worker Pool, Firestore, Pub/Sub, Secret Manager, Cloud KMS, Firebase Hosting, public Web BFF |
| Hosted verification | One authenticated browser-origin synthetic transaction through outbox, Pub/Sub, Worker ACK, KMS attestation, and audit |
| Shared IaC state | GCS DIY backend + dedicated KMS secrets provider; Compute Engine API disabled |
| Keyless CI preview | WIF verification + Pulumi preview isolation |
| Immutable publication | Four-subject release bound to source `a876a7bfa5fa86f88e8387efb557773281daa6e6`; unsigned SBOM/provenance |
| Controlled CD | A person starts stage and promote for that frozen release. Images stay pinned to a digest. Runtime and Hosting use separate identities |
| Promotion evidence | Immutable Generic evidence. Break glass recovery was verified after the original promotion succeeded |
| Rollback path | Implemented and tested. Identities are readable in the live project. Rollback has not been executed there |

## Current

The documentation a reviewer reads, and the CV claim boundaries, match the verified project.
No further product features are required before this can be used on a CV.

Optional follow-ups below are not blockers.

## Optional / future

Not required for current portfolio/CV readiness:

- Signed provenance / Cosign
- SAST / secret scanning / Dependabot / Dependency-Check / ZAP
- GitHub application CI on pull requests
- Pub/Sub dead-letter forwarding
- Alerting and tracing (including OpenTelemetry, if ever added)
- Live rollback exercise
- Historical Cloud Run / Hosting / Artifact Registry cleanup
- Operator credential hygiene (historical Pulumi passphrase follow-up)
- Graph change notifications or production Entra, only if a real tenant path exists

## Non-goals until a new ADR

Kubernetes, Kafka, Redis, AI, extra Graph APIs, Terraform, GKE, always-on VMs.
