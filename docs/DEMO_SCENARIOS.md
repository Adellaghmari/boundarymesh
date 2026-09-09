# Demo scenarios

Every scenario is **deterministic and reproducible** via seed id. Label **SYNTHETIC DEMO** unless a later live Graph row is proven.

| ID | Scenario | Expected outcome |
| --- | --- | --- |
| S01 | Valid Entra JWT, all controls pass | ALLOW |
| S02 | Expired JWT | REJECT AUTHN_EXPIRED |
| S03 | Wrong issuer | REJECT AUTHN_WRONG_ISSUER |
| S04 | Wrong audience | REJECT AUTHN_WRONG_AUDIENCE |
| S05 | Missing app role / scope | REJECT AUTHZ_MISSING_* |
| S06 | Valid HMAC webhook | ALLOW or ALLOW_WITH_TRANSFORMATION |
| S07 | Invalid HMAC | REJECT AUTHENTICITY_INVALID_SIGNATURE |
| S08 | Expired webhook timestamp | REJECT FRESHNESS_EXPIRED |
| S09 | Malicious replay (nonce reuse) | REJECT REPLAY_DETECTED |
| S10 | Legitimate duplicate (`event_id` reuse, new nonce) | DUPLICATE_DELIVERY / prior result |
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
