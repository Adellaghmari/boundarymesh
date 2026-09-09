# Threat model (STRIDE)

Scope: BoundaryMesh API, worker, Vue demo, GCP project, Entra/Graph connector, partner webhooks.  
Out of scope: compromising Google or Microsoft as vendors; physical datacenter.

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
