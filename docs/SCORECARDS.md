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
| M365 relevance | **9** | One deep calendar connector + harness TESTED; live tenant not assumed |
| GCP depth | **8** | Serverless suite; no GKE by design |
| Cloud architecture | **9** | Two services, pull worker, outbox |
| Backend engineering | **9** | |
| Frontend differentiation | **9** | Vue SVG Integration Boundary Graph from live Java state |
| Application security | **9** | Semgrep/ZAP/HMAC/JWT — not Trivy-as-story |
| IAM depth | **9** | Entra + WIF + SA split + app roles |
| Cryptography depth | **9** | Local JCA ECDSA P-256 + constrained RFC 8785 + SHA-256 chain; Cloud KMS PLANNED |
| Testing depth | **9** | Pyramid specified including Camel and replay |
| Job-market relevance | **9** | |
| Interview differentiation | **9** | AuthN≠AuthZ, replay≠duplicate |
| Project A differentiation | **9** | Score 2/10 similarity |
| Project B differentiation | **9** | Score 2/10 similarity |
| Implementation realism | **9** | Harness mode, cost, no Phase 0 cloud |
| Cost coherence | **9** | Scale to zero, logging trap called out |

**M365 9:** Graph SDK + harness through BoundaryMesh controls. Live tenant is a badge, not the architecture.  
**Frontend 9:** Phase 13 SVG graph is state-driven, not a static poster.  
**Cryptography 9:** local JCA is TESTED; Cloud KMS remains the managed-key follow-on, not a fake substitute.  
**Platform engineering 7** lives on the job-market card, not here as a product failure.

## Phase 14 navigation visual quality

Scored against the approved premium header direction (navy/teal gradient, shield lockup, teal active tile, grouped secondary nav, pale canvas). Not a claim that live Microsoft branding is present.

| Dimension | Score |
| --- | --- |
| Modernity | **9** |
| Professional polish | **9** |
| Visual hierarchy | **9** |
| Product identity | **9** |
| Route discoverability | **9** |
| Responsive behavior | **9** |
| Accessibility | **9** |
| Recruiter impression | **9** |
| Uniqueness vs Project A | **9** |
| Uniqueness vs Project B | **9** |

Core vs Labs vs Operations vs Evidence is expressed with spacing and dropdowns, not five permanent category headings. Mobile uses a grouped drawer.

No unjustified category below 8.
