# Failure model

Failures were designed **before** implementation. Security verification **fails closed**. Availability problems **quarantine**. Nothing retries forever.

Legend: **FC** fail closed, **FO** fail open (documented), **Q** quarantine, **R** reject, **NR** no retry, **BR** bounded retry.

| Failure | Posture | Retry | Outcome | Quarantine | Evidence | UI |
| --- | --- | --- | --- | --- | --- | --- |
| Invalid JWT | FC | NR | HTTP 401 (not admitted) | no | AUTHN / UNAUTHORIZED | Authentication could not be established |
| Expired JWT | FC | NR | HTTP 401 | no | UNAUTHORIZED | |
| Wrong issuer | FC | NR | HTTP 401 | no | UNAUTHORIZED | |
| Wrong audience | FC | NR | HTTP 401 | no | UNAUTHORIZED | |
| Missing scope on HTTP-gated route | FC | NR | HTTP 403 | no | FORBIDDEN | Authenticated, not authorized |
| Missing scope on identity evaluate | FC | NR | REJECT | no | AUTHZ_MISSING_SCOPE | AuthN PASS, AuthZ FAIL |
| JWKS unavailable | FC | NR | REJECT | no | AUTHN_JWKS_UNAVAILABLE | |
| JWKS rotation unknown kid (refresh fails) | FC | NR | REJECT | no | | |
| Invalid webhook HMAC | FC | NR | REJECT | no | AUTHENTICITY_INVALID_SIGNATURE | |
| Expired webhook timestamp | FC | NR | REJECT | no | FRESHNESS_EXPIRED | |
| Malicious replay | FC | NR | REJECT | no | REPLAY_DETECTED | |
| Legitimate duplicate | n/a | NR extra sink | prior result | no | DUPLICATE_DELIVERY | Delivery already succeeded |
| Schema invalid | FC | NR | REJECT | no | CONTRACT_SCHEMA_INVALID | pointers |
| Unsupported version / drift | FC on accept | NR auto | QUARANTINE | yes | CONTRACT_DRIFT / UNSUPPORTED | operational |
| Egress DENY | FC | NR | REJECT | no | EGRESS_FIELD_DENIED | |
| Secret Manager unavailable | Q | BR on worker | QUARANTINE | yes | CREDENTIAL_UNAVAILABLE | |
| Secret version invalid | Q | NR | QUARANTINE | yes | | |
| Graph 401/403 | Q | NR | QUARANTINE | yes | | |
| Graph 429 | BR then Q | honor Retry-After | QUARANTINE if exhausted | maybe | DELIVERY_HTTP_429 | |
| Graph 5xx | BR then Q | BR | QUARANTINE | yes | | |
| Downstream timeout | BR then Q | BR | QUARANTINE | yes | DELIVERY_TIMEOUT | |
| Pub/Sub duplicate | idempotent | n/a | same delivery | no | | |
| Worker crash mid-send | Pub/Sub redelivery | bounded | idempotent | if exhausted | | |
| Circuit open | Q | NR | QUARANTINE | yes | CONNECTOR_CIRCUIT_OPEN | |
| KMS signing failure | FO on delivery if already ALLOW; FC on claiming signature | BR sign 2× | UNSIGNED attestation | no unless policy | ATTESTATION_SIGNING_FAILED | Attestation unsigned |
| Firestore transient | retry SDK | BR | 503 ingest | no | | |
| Audit append failure | detective | BR | security event | no | AUDIT_CHAIN_APPEND_FAILED | Posture warning |
| Graph validation timeout | FC handshake | NR | Graph unsubscribes | | | Harness simulates |

## Combined retry budget

In-route Resilience4j: max 3.  
Pub/Sub: max 5 deliveries.  
Worst case outbound attempts ≤ 15 only if misconfigured — **lock: disable in-route retry when Pub/Sub will retry**, **or** in-route retry=3 and Pub/Sub maxAttempts=3. **Lock: Pub/Sub maxAttempts=5, in-route retry=2 for 429/5xx, circuit breaker counts all.** Document the product on the quarantine item as `attempt_budget`.

## Demo-reproducible failures

Every row in [DEMO_SCENARIOS.md](DEMO_SCENARIOS.md) has a seed id. Local and public demo can trigger them without waiting for real outages (synthetic injectors on **protected** admin in live; public demo uses pre-seeded transactions).
