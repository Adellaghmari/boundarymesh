# Demo scenarios

Every scenario is **deterministic and reproducible** via seed id. Label **SYNTHETIC DEMO** unless a later live Graph row is proven.

| ID | Scenario | Expected outcome |
| --- | --- | --- |
| S01 | Valid **local** machine JWT, all controls pass | ALLOW — Identity Lab |
| S02 | Expired JWT | HTTP 401 — authentication cannot be established |
| S03 | Wrong issuer | HTTP 401 |
| S04 | Wrong audience | HTTP 401 |
| S05 | Missing evaluate permission | AuthN PASS, AuthZ FAIL, REJECT |
| S06 | Valid HMAC webhook | ALLOW — Webhook Lab |
| S07 | Invalid HMAC | REJECT AUTHENTICITY_INVALID_SIGNATURE |
| S08 | Expired webhook timestamp | REJECT FRESHNESS_EXPIRED |
| S09 | Malicious replay (nonce reuse) | REJECT REPLAY_DETECTED |
| S10 | Legitimate duplicate (`event_id` reuse, new nonce) | `duplicate_delivery: true`, prior result |
| S11 | Graph subscription validation handshake | 200 + token echo |
| S12 | Graph notification (harness) | ALLOW_* after engine |
| S13 | Schema valid | contract VALID |
| S14 | Schema mismatch | REJECT CONTRACT_SCHEMA_INVALID |
| S15 | Contract version drift | QUARANTINE CONTRACT_DRIFT |
| S16 | Forbidden outgoing field | REJECT EGRESS_FIELD_DENIED |
| S17 | Redaction | ALLOW_WITH_TRANSFORMATION |
| S18 | Credential version change (old HMAC) | REJECT authenticity / then new ALLOW |
| S19 | Secret unavailable | QUARANTINE CREDENTIAL_UNAVAILABLE |
| S20 | Downstream timeout | QUARANTINE DELIVERY_TIMEOUT |
| S21 | Downstream 429 | retry then success or Q |
| S22 | Downstream 500 | bounded retry then Q |
| S23 | Bounded retry exhaust | QUARANTINE |
| S24 | Circuit breaker open | QUARANTINE CONNECTOR_CIRCUIT_OPEN |
| S25 | Quarantine inspect | UI shows reason, no secrets |
| S26 | Safe reprocess after policy fix | new transaction SUCCESS |
| S27 | KMS signing success | attestation SIGNED, verify true |
| S28 | KMS signing failure | UNSIGNED, verify false |
| S29 | Attestation verification | valid/invalid |
| S30 | Audit chain valid | VALID |
| S31 | Audit chain tamper | HASH_MISMATCH or GAP |

Wow path: S01/S17 then S04 then S09 then S15.
