# Architecture scorecard (Phase 0)

Desired: no major category below 8 without an explicit justified tradeoff.

| Category | Score | Notes |
| --- | --- | --- |
| Product originality | **9** | Enforcement + attestation + drift-as-ops, not iPaaS |
| Product clarity | **9** | One question: can this cross? |
| Security depth | **9** | STRIDE, fail closed, IAM split |
| Integration depth | **9** | Camel after engine, two connectors |
| Java depth | **9** | Engine, types, adapters — not CRUD |
| Spring depth | **9** | Security resource server + BFF, not controllers-only |
| Apache Camel relevance | **9** | Real EIP list; policy kept out of DSL |
| M365 relevance | **8** | One deep calendar connector + harness; live tenant not assumed — honest |
| GCP depth | **8** | Serverless suite; no GKE by design |
| Cloud architecture | **9** | Two services, pull worker, outbox |
| Backend engineering | **9** | |
| Frontend differentiation | **8** | Vue + graph visual; polish in Phase 2/13 |
| Application security | **9** | Semgrep/ZAP/HMAC/JWT — not Trivy-as-story |
| IAM depth | **9** | Entra + WIF + SA split + app roles |
| Cryptography depth | **8** | KMS ECDSA + JCS + hash chain; no homemade ciphers — correct |
| Testing depth | **9** | Pyramid specified including Camel and replay |
| Job-market relevance | **9** | |
| Interview differentiation | **9** | AuthN≠AuthZ, replay≠duplicate |
| Project A differentiation | **9** | Score 2/10 similarity |
| Project B differentiation | **9** | Score 2/10 similarity |
| Implementation realism | **9** | Harness mode, cost, no Phase 0 cloud |
| Cost coherence | **9** | Scale to zero, logging trap called out |

**M365 8:** depends on tenant luck; architecture still complete via harness.  
**Frontend 8:** visual wow is Phase 13.  
**Cryptography 8:** using KMS/JCA is the *right* depth, not a gap.  
**Platform engineering 7** lives on the job-market card, not here as a product failure.

No unjustified category below 8.
