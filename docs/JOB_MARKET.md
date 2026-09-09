# Job market scorecard

Portfolio evidence only. Not a claim of professional employment.

Scores: 1–10 as **how clearly a recruiter hiring for that title would see relevant engineering**.

| Role | Score | Strongest evidence this architecture will produce | Phase 5–7 implemented evidence |
| --- | --- | --- | --- |
| Integration Engineer | **10** | Camel EIPs, Graph connector, contracts, HMAC, delivery vs ingress | Versioned contracts + destination egress now tested. Camel/Graph still planned — score stays architecture-forward, not live Camel |
| Java Backend Engineer | **9** | Java 25, Spring Boot 4.1, domain/engine, tests, exception/reason-code model | Ports, networknt adapter isolation, ArchUnit, composed crossing |
| Technical Solutions Engineer | **9** | Cross-cloud story, recruiter graph, failure modes, honest live/harness | Contract vs egress distinction is a strong demo story |
| Implementation Engineer | **8** | Entra consent, subscription lifecycle, rotation, quarantine reprocess | Policy/contract versioning helps. No Entra consent yet — **8 not 9** |
| Solutions Engineer | **8** | Demo wow path; slightly less “pre-sales deck” than a SaaS SKU — still strong | Full Crossing lab |
| Cloud Engineer | **8** | Cloud Run, Pub/Sub, WIF, Pulumi, IAM split; not GKE (intentional) | Still planned |
| Platform Engineer | **7** | WIF + Pulumi + two SAs; no internal developer platform — **justified tradeoff** | Unchanged |
| Security-minded Software Engineer | **9** | AuthN/AuthZ split, webhook defense, egress, KMS, threat/failure models | Contract + egress fail-closed composition now tested |
| API Engineer | **9** | OpenAPI 3.1, AsyncAPI, JSON Schema runtime, problem+json | Runtime schema + versioned artifacts tested. Was 8 |
| IAM / identity-oriented | **8** | Entra, app roles, JWKS | Unchanged. Entra not live — **8** |
| Technical CS / implementation (API depth) | **8** | Quarantine workflow, contract drift as ops state, Graph permission evidence | Drift quarantine exists; orchestrator still planned |

Platform Engineer is the only category below 8. Reason: BoundaryMesh is an application with solid delivery platform pieces, not a platform-engineering product. Raising it with Kubernetes would **hurt** cost and coherence (ADR: not Kubernetes).
