# Job market scorecard

Portfolio evidence only. Not a claim of professional employment.

Scores: 1–10 as **how clearly a recruiter hiring for that title would see relevant engineering**.

| Role | Score | Strongest evidence this architecture will produce | Phase 8–10 implemented evidence |
| --- | --- | --- | --- |
| Integration Engineer | **10** | Camel EIPs, contracts, HMAC, delivery vs ingress | Worker Camel CBR/translator/idempotent/DLC + WireMock connectors. Graph still planned |
| Java Backend Engineer | **9** | Java 25, Spring Boot 4.1, ports, ArchUnit | Separate API/worker modules, Resilience4j, JUnit delivery pipeline |
| Technical Solutions Engineer | **9** | Honest failure → quarantine → recovery story | Delivery Lab + Quarantine UI against real Java |
| Implementation Engineer | **8** | Rotation, consent vs credential, reprocess | Local consent model prepares Graph. No Entra yet — **8 not 9** |
| Solutions Engineer | **9** | One-minute crossing + outage + reprocess | Recruiter labs are now a full integration-security demo |
| Cloud Engineer | **8** | Cloud Run, Pub/Sub, WIF, Pulumi still the target | Still planned |
| Platform Engineer | **7** | Two local runtimes + future IAM split | Unchanged — not a platform product |
| Security-minded Software Engineer | **9** | AuthN/AuthZ, webhook, egress, secret references, admin reprocess | All nine ControlVector fields real |
| API Engineer | **9** | OpenAPI / AsyncAPI / JSON Schema + delivery contract | DeliveryInstruction is a real message type |
| IAM / identity-oriented | **8** | JWT scopes + consent grants | Entra/Graph still planned — **8** |
| Technical CS / implementation (API depth) | **9** | Quarantine workflow operators can inspect | Local orchestrator tested; persistence planned |

Platform Engineer is the only category below 8. Reason: BoundaryMesh is an application with solid delivery platform pieces, not a platform-engineering product. Raising it with Kubernetes would **hurt** cost and coherence (ADR: not Kubernetes).
