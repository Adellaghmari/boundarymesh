# Threat model (STRIDE)

Scope: BoundaryMesh API, worker, Vue demo, GCP project, Entra/Graph connector, partner webhooks.  
Out of scope: compromising Google or Microsoft as vendors; physical datacenter.

## Phase 8–10 implemented mitigations

| Threat | Implemented now | Still planned |
| --- | --- | --- |
| Information disclosure (secrets) | References only in API/UI/ControlVector; worker JIT resolve; leak tests | Secret Manager IAM, Gitleaks CI |
| Elevation of privilege (reprocess) | `boundary.admin` on quarantine reprocess | Entra app roles |
| Replay (inbound) | HMAC nonce window | Firestore TTL nonce |
| Replay (outbound) | Delivery idempotent consumer | Firestore idempotency |
| Tampering | JWT/HMAC/contracts/egress + audit-chain verification + ECDSA attestation verify; KMS key hosted in GCP, live signing verified, API-only CryptoKey IAM applied | Cloud Run workload verification, Firestore versioning |
| Denial / dependency failure | Bounded R4j retry, circuit breaker, quarantine | Pub/Sub redelivery budget |

## Phase 11–13 implemented mitigations

| Threat | Implemented now | Still planned |
| --- | --- | --- |
| Repudiation | Structured audit + SHA-256 hash-chain continuity | Firestore + exported checkpoints |
| Tampering (audit entries) | Recalculated hashes and previous-hash links | Object versioning / privileged-admin detection |
| Tampering (attestation) | ECDSA P-256 verification of RFC 8785 bytes; Cloud KMS signing **LIVE VERIFIED LOCALLY**; digest/signature/public-key integrity checks, strict DER → P1363, verification before SIGNED; API-only CryptoKey IAM **LIVE** | Production workload verification (15I-C/D) |
| Information disclosure | Metadata-only audit/attestation/graph | Gitleaks CI |

The chain does **not** prevent deletion by a privileged storage administrator. That remains a storage/governance concern.

Normal/local/test and `gcp-emulator` retain JCA with no live KMS dependency. The explicit
`gcp-live-kms` Maven profile permits real KMS metadata reads, signing, and public-key retrieval
against the existing version 1 with synthetic input. Phase 15H-C is **LIVE VERIFIED LOCALLY**.
Phase 15I-A composes `CloudKmsAttestationSigner`; Phase 15I-B1 applied the API-only,
CryptoKey-scoped signing and public-key-viewer IAM. The Cloud Run API Service and Worker Pool are
deployed and Ready; workload KMS signing remains Phase 15I-D verification work. Controlled failure tests
return UNSIGNED without changing the boundary outcome; they are not live IAM/key failure tests.

## Phase 3–4 implemented mitigations

| Threat | Implemented now | Still planned |
| --- | --- | --- |
| Spoofing (JWT) | Asymmetric RS256 + JWKS, issuer, audience, exp, nbf | Entra issuer/JWKS live |
| Spoofing (webhook) | HMAC-SHA256 over raw body, constant-time compare | Secret Manager |
| Tampering (webhook body) | Signature covers SHA-256 of raw bytes | Graph-specific proofs |
| Replay | Signed timestamp ±300s + atomic nonce store | Firestore TTL **policy** |
| Elevation of privilege | Scope `boundary.evaluate` on HTTP and domain AuthZ | Entra app roles / ACL |
| Information disclosure | Sanitized 401/403, no raw JWT/HMAC in responses or normal logs | Gitleaks CI, cloud log sinks |
| Oversized ingest | 65536-byte webhook limit, 413 | Distributed rate limit |

KMS infrastructure is **LIVE / PROVISIONED**, and opt-in KMS signing is **LIVE VERIFIED LOCALLY**. Secret Manager has one enabled synthetic version in each of the four integration containers; production Java adapters are live-verified locally without payload disclosure. The Cloud Run workloads are deployed and Ready, but their Firestore, Secret Manager, Pub/Sub, and KMS data paths await Phase 15I-D verification.

Each important threat maps to **preventive**, **detective**, and **recovery** controls.

## Spoofing

| Threat | Preventive | Detective | Recovery |
| --- | --- | --- | --- |
| Forged JWT | JWKS signature, iss, aud, exp, tid | AuthN reject metrics, security events | Rotate Entra keys/app; revoke |
| Stolen token replay | short TTL, nbf/exp, replay window where applicable | REPLAY / AUTHN_EXPIRED | Entra revoke, disable integration |
| Fake Graph notifications | clientState, subscription id match | AUTHENTICITY failures | Rotate clientState, delete subscription |
| Fake partner webhook | HMAC + timestamp + nonce | HMAC fail metrics | Rotate HMAC secret |
| Spoofed admin | Entra OIDC + app roles + CSRF | 401/403 logs | Disable user in Entra |

## Tampering

| Threat | Preventive | Detective | Recovery |
| --- | --- | --- | --- |
| Body tamper | HMAC / JWT over request; Graph fetch from source | Invalid signature | Reject |
| Firestore evidence edit | IAM least privilege; no public Firestore | Audit chain verify | Restore from PITR if enabled later; disclose |
| Attestation mutate | KMS signature | Verify endpoint | Trust signature not document |
| Log injection | Structured logging, deny-list | Log review | n/a |

## Repudiation

| Threat | Preventive | Detective | Recovery |
| --- | --- | --- | --- |
| “We never allowed that” | Attestation + audit chain | Chain/checkpoint verify | Re-issue unsigned warning honestly |
| Admin denies rotation | Admin events on platform chain | Entra sign-in logs (live later) | |

## Information disclosure

| Threat | Preventive | Detective | Recovery |
| --- | --- | --- | --- |
| Secret in logs/UI | encoder deny-list, references only | Gitleaks, log queries for `eyJ` JWT prefix | Rotate all possibly leaked |
| PII in audit | field paths + hashes only | Code review, tests | Delete documents, rotate |
| Over-permissioned Graph | `Calendars.Read` only | Posture page lists grants | Admin remove permission |
| Public demo data scrape | synthetic only, rate limit | |

## Denial of service

| Threat | Preventive | Detective | Recovery |
| --- | --- | --- | --- |
| Ingress flood | Cloud Run concurrency, app rate limit | 429 metrics | Scale / Armor later |
| Downstream 429 storm | circuit breaker, rate limiter | circuit state | Quarantine, backoff |
| JWKS outage | cache + fail closed | JWKS_UNAVAILABLE | Wait / Entra status |

## Elevation of privilege

| Threat | Preventive | Detective | Recovery |
| --- | --- | --- | --- |
| Anonymous reprocess | method security | 401 tests | |
| Operator rotates secrets | role split | 403 tests | |
| Worker SA reads all secrets | secret-level IAM | IAM policy review | Tighten bindings |
| API calls Graph | no Graph on API SA | IAM | |

## Additional (integration-specific)

| Threat | Preventive | Detective | Recovery |
| --- | --- | --- | --- |
| Malicious replay | nonce + window | REPLAY_DETECTED | |
| Contract abuse / unexpected fields | implicit deny egress + schema | CONTRACT_* | Quarantine |
| Schema bomb (huge JSON) | size limit on ingest | 413 | |
| Data exfiltration via extra fields | implicit deny | denied_count | |
| Downstream compromise | minimize payload, no secrets to dest | | Disable connector |
| Audit manipulation | hash chain + checkpoints | verify | |
| Credential misuse | worker-only accessor, rotation | 401 from Graph | Rotate, disable |

## Residual risk (accepted)

- Privileged GCP project Owner can mutate Firestore and delete checkpoints. Detective controls + later log sinks. Not claimed solved.
- Recruiter-visible synthetic data can be copied. It is fictional.
- Contract harness can be mistaken for live Graph if UI badge is ignored — badge is mandatory.
