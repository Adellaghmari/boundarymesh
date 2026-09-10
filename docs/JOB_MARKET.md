# Job market scorecard

Portfolio evidence only. Not a claim of professional employment.

Scores: 1–10 as **how clearly a recruiter hiring for that title would see relevant engineering**.

| Role | Score | Strongest evidence this architecture will produce | Phase 11–13 implemented evidence |
| --- | --- | --- | --- |
| Integration Engineer | **10** | Camel EIPs, contracts, HMAC, delivery vs ingress | Worker Camel + WireMock connectors + state-driven Integration Boundary Graph |
| Java Backend Engineer | **10** | Java 25, Spring Boot 4.1, ports, ArchUnit | SHA-256 audit chain, RFC 8785 canonicalizer, JCA ECDSA P-256, JUnit tamper tests |
| Technical Solutions Engineer | **10** | Honest failure → quarantine → recovery story | Graph + audit verify + attestation verify against real Java, not slides |
| Implementation Engineer | **8** | Rotation, consent vs credential, reprocess | Local consent model prepares Graph. No Entra yet — **8 not 9** |
| Solutions Engineer | **10** | One-minute crossing + outage + reprocess | Recruiter graph demo: deliver → quarantine → replay reject |
| Cloud Engineer | **8** | Cloud Run, Pub/Sub, WIF, Pulumi still the target | Still planned |
| Platform Engineer | **7** | Two local runtimes + future IAM split | Unchanged — not a platform product |
| Security-minded Software Engineer | **10** | AuthN/AuthZ, webhook, egress, secret references, admin reprocess | Hash-chain audit + public-key attestation verification. Not AI assurance |
| API Engineer | **9** | OpenAPI / AsyncAPI / JSON Schema + delivery contract | Read-only audit/attestation/graph APIs with bounded lists |
| IAM / identity-oriented | **8** | JWT scopes + consent grants | Entra/Graph still planned — **8** |
| Technical CS / implementation (API depth) | **9** | Quarantine workflow operators can inspect | Local orchestrator tested; persistence planned |

Platform Engineer is the only category below 8. Reason: BoundaryMesh is an application with solid delivery platform pieces, not a platform-engineering product. Raising it with Kubernetes would **hurt** cost and coherence (ADR: not Kubernetes).
