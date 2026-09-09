# Job market scorecard

Portfolio evidence only. Not a claim of professional employment.

Scores: 1–10 as **how clearly a recruiter hiring for that title would see relevant engineering**.

| Role | Score | Strongest evidence this architecture will produce | Phase 3–4 implemented evidence |
| --- | --- | --- | --- |
| Integration Engineer | **10** | Camel EIPs, Graph connector, contracts, HMAC, delivery vs ingress | HMAC ingress + replay/idempotency. Camel/Graph still planned — score stays architecture-forward, not live Camel |
| Java Backend Engineer | **9** | Java 25, Spring Boot 4.1, domain/engine, tests, exception/reason-code model | Resource server, JCA HMAC, ports, ArchUnit |
| Technical Solutions Engineer | **9** | Cross-cloud story, recruiter graph, failure modes, honest live/harness | Identity + webhook labs, AuthN≠AuthZ, replay≠duplicate |
| Implementation Engineer | **8** | Entra consent, subscription lifecycle, rotation, quarantine reprocess | Local issuer/scopes and webhook contract. No Entra consent yet — **8 not 9** |
| Solutions Engineer | **8** | Demo wow path; slightly less “pre-sales deck” than a SaaS SKU — still strong | Recruiter labs |
| Cloud Engineer | **8** | Cloud Run, Pub/Sub, WIF, Pulumi, IAM split; not GKE (intentional) | Still planned |
| Platform Engineer | **7** | WIF + Pulumi + two SAs; no internal developer platform — **justified tradeoff** | Unchanged |
| Security-minded Software Engineer | **9** | AuthN/AuthZ split, webhook defense, egress, KMS, threat/failure models | JWT/JWKS + HMAC + freshness + replay now tested |
| API Engineer | **8** | OpenAPI 3.1, AsyncAPI, JSON Schema runtime, problem+json | Secured endpoints + OpenAPI schemes. Runtime contract firewall not yet — **8 not 9** |
| IAM / identity-oriented | **8** | Entra, app roles, JWKS | Local JWT/JWKS/scopes proven. Entra not live — **8** |
| Technical CS / implementation (API depth) | **8** | Quarantine workflow, contract drift as ops state, Graph permission evidence | Webhook/identity labs help; Graph still planned |

Platform Engineer is the only category below 8. Reason: BoundaryMesh is an application with solid delivery platform pieces, not a platform-engineering product. Raising it with Kubernetes would **hurt** cost and coherence (ADR: not Kubernetes).
