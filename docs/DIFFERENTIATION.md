# Differentiation from Projects A and B

Projects A and B are **COMPLETE and FROZEN**. This document only compares. No files outside BOUNDARYMESH were modified.

## Project A — AI Operations Copilot

Profile: React, Python, FastAPI, Azure, Foundry, PostgreSQL, pgvector, RAG, agents, Decision Assurance, human decision.

Question: *Can an AI-generated recommendation be sufficiently supported by evidence for a human to decide?*

| Dimension | A | C | Similar? |
| --- | --- | --- | --- |
| Product problem | AI evidence / assurance | System boundary crossing | No |
| Decision model | Gates around LLM | Deterministic policy, no LLM | No |
| User workflow | Human approve/edit/reject AI | Recruiter inspects enforcement; admin operates integrations | Low |
| Data model | Tickets, chunks, assurance reports | Transactions, decisions, attestations | No |
| Cloud | Azure | GCP | No |
| Backend | FastAPI | Spring Boot | No |
| Frontend | React | Vue | No |
| Security story | Responsible AI, abstention | IAM, webhooks, egress, KMS | Different |
| Recruiter story | Evidence-grounded AI | Cryptographic enforcement of integrations | No |

**Similarity score: 2 / 10** (shared: portfolio polish, public demo, deterministic *engines* as a product pattern — not the same engine or domain). Target ≤ 3. **Pass.**

Forbidden in C: LLMs, RAG, agents, AI verdicts, confidence scores, Decision Assurance concepts, hallucination eval, human-as-core-decision.

## Project B — CloudOps Release Intelligence

Profile: Angular, Go, AWS, Terraform, DynamoDB, Lambda, EventBridge, SQS, OPA, release risk, blast radius, rollback, supply chain.

Question: *What changed in a release, what might be affected, how should ops understand risk/recovery?*

| Dimension | B | C | Similar? |
| --- | --- | --- | --- |
| Product | Release intelligence | Integration boundary security | No |
| Domain | Deployments, health, rollback | Identity, contracts, egress, delivery | No |
| Frameworks | Angular + Gin | Vue + Spring + Camel | No |
| Cloud | AWS | GCP | No |
| IaC | Terraform | Pulumi | No |
| Async | EventBridge/SQS analysis | Pub/Sub **delivery after allow** | Superficial only |
| Recruiter story | Change / risk / recovery | Can this crossing happen, with proof | No |

**Similarity score: 2 / 10.** Async messaging + CI OIDC/WIF are **allowed overlap** and do not dominate. Target ≤ 3. **Pass.**

Forbidden in C: release replay as the product, blast radius of services, rollback readiness, DevOps dashboards as the center, EventBridge→Pub/Sub clone.

## Unique product audit (C)

Not an API gateway, not iPaaS, not A without AI, not B on GCP. Innovation is the **combination** of the six flagships + engine + attestation + audit chain + boundary graph + M365 cross-cloud.
