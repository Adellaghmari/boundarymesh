# Job market scorecard

Portfolio evidence only. Not a claim of professional employment.

Scores: 1–10 as **how clearly a recruiter hiring for that title would see relevant engineering**.

| Role | Score | Strongest evidence this architecture will produce |
| --- | --- | --- |
| Integration Engineer | **10** | Camel EIPs, Graph connector, contracts, HMAC, delivery vs ingress |
| Java Backend Engineer | **9** | Java 25, Spring Boot 4.1, domain/engine, tests, exception/reason-code model |
| Technical Solutions Engineer | **9** | Cross-cloud story, recruiter graph, failure modes, honest live/harness |
| Implementation Engineer | **9** | Entra consent, subscription lifecycle, rotation, quarantine reprocess |
| Solutions Engineer | **8** | Demo wow path; slightly less “pre-sales deck” than a SaaS SKU — still strong |
| Cloud Engineer | **8** | Cloud Run, Pub/Sub, WIF, Pulumi, IAM split; not GKE (intentional) |
| Platform Engineer | **7** | WIF + Pulumi + two SAs; no internal developer platform — **justified tradeoff** |
| Security-minded Software Engineer | **9** | AuthN/AuthZ split, webhook defense, egress, KMS, threat/failure models |
| API Engineer | **9** | OpenAPI 3.1, AsyncAPI, JSON Schema runtime, problem+json |
| Technical CS / implementation (API depth) | **8** | Quarantine workflow, contract drift as ops state, Graph permission evidence |

Platform Engineer is the only category below 8. Reason: BoundaryMesh is an application with solid delivery platform pieces, not a platform-engineering product. Raising it with Kubernetes would **hurt** cost and coherence (ADR: not Kubernetes).
