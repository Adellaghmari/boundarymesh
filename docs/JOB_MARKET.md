# Job market scorecard

**Historical Phase 0 planning artifact.** Not live inventory. Current claims: [../CV_CLAIMS_MATRIX.md](../CV_CLAIMS_MATRIX.md), [../PROJECT_STATUS.md](../PROJECT_STATUS.md).

Portfolio evidence only. Not a claim of professional employment.

Scores: 1–10 as **how clearly a recruiter hiring for that title would see relevant engineering**.

| Role | Score | Strongest evidence | Current verified evidence |
| --- | --- | --- | --- |
| Integration Engineer | **10** | Camel EIPs, contracts, HMAC, Graph connector through the engine | Worker Camel + WireMock + M365 calendar harness through contract/egress |
| Java Backend Engineer | **10** | Java 25, Spring Boot 4.1, ports, ArchUnit | Graph SDK adapter isolated from domain; dual SecurityFilterChain |
| Technical Solutions Engineer | **10** | Honest failure → quarantine → recovery story | Connections page explains Graph without claiming live tenant |
| Implementation Engineer | **9** | Rotation, consent vs credential, Entra setup doc | Delegated Calendars.Read mapped to ConsentGrant; one local real-account Graph crossing verified |
| Solutions Engineer | **10** | One-minute crossing + outage + reprocess | Recruiter graph + Microsoft harness crossing |
| Cloud Engineer | **8** | Cloud Run, Pub/Sub, WIF, Pulumi | Private API, Worker, Web BFF, shared GCS/KMS state, keyless WIF, and least-privilege hosted preview live verified |
| Platform Engineer | **7** | Runtime separation + scoped IAM + shared state | Three keyless runtimes plus keyless publish/deploy; application PR CI is still local-only |
| Security-minded Software Engineer | **10** | AuthN/AuthZ, webhook, egress, CSRF on session chain | Hash-chain audit + OIDC BFF without tokens in Vue |
| API Engineer | **9** | OpenAPI / AsyncAPI / JSON Schema + delivery contract | `m365-calendar/v1` normalized Graph contract |
| IAM / identity-oriented | **9** | JWT scopes + Entra/Firebase BFFs + WIF | Local Entra and hosted Firebase auth live verified; WIF positive/negative boundaries live verified |
| Microsoft integration-oriented | **8** | Graph SDK + Entra OIDC + Calendar.Read | One real personal-account Graph event live verified locally; enterprise mailbox and notifications remain unverified/planned |
| Technical CS / implementation (API depth) | **9** | Quarantine workflow operators can inspect | Firestore-backed terminal quarantine and evidence reads workload live verified |

Platform Engineer is the only category below 8. Reason: BoundaryMesh is an application with solid delivery platform pieces, not a platform-engineering product. Raising it with Kubernetes would **hurt** cost and coherence (ADR: not Kubernetes).
