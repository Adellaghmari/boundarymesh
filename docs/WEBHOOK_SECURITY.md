# Webhook and subscription defense

Flagship capability #2.

Two providers, two designs. Do not pretend they are the same.

## A. Generic signed webhook (QuayForge Orders)

Used for partner HTTP callbacks.

### Required headers

| Header | Purpose |
| --- | --- |
| `X-Velora-Connector-Id` | Integration identity (not a secret) |
| `X-Velora-Timestamp` | Unix seconds |
| `X-Velora-Nonce` | Unique per attempt |
| `X-Velora-Signature` | `v1=<hex>` HMAC-SHA256 |

### Canonical string

```text
v1:{timestamp}:{nonce}:{sha256(raw_body)}
```

HMAC-SHA256 over that UTF-8 string with the secret from Secret Manager.

### Verification steps

1. Load secret **bytes** via reference; never log.
2. Compute HMAC.
3. Compare with `MessageDigest.isEqual` (constant-time).
4. Reject if timestamp outside **±300 seconds** (configurable per integration, max 600).
5. Reject if nonce seen in the replay window (Firestore `replayNonces/{integrationId}/{nonce}` with TTL = window).
6. Then run AuthZ ACL, contract, egress, engine.

### Malicious replay vs legitimate duplicate

| | Replay attack | Legitimate duplicate delivery |
| --- | --- | --- |
| What | Previously valid signed request resent **outside** freshness/nonce rules, or nonce reused | Provider retries the **same** event with a **new** HTTP attempt |
| Identity of event | Same signature, same nonce/timestamp | Same **event id** in body (`event_id`) |
| Security result | `REJECT` / `REPLAY_DETECTED` | Not a security reject |
| Reliability result | n/a | Idempotent consumer returns prior result (`DUPLICATE_DELIVERY`) |

If a retry reuses the **same nonce** inside the window, it looks like replay. QuayForge contract **requires a new nonce per HTTP attempt** and a stable `event_id`. Tests cover both.

## B. Microsoft Graph change notifications

See also [M365_GRAPH_INTEGRATION.md](M365_GRAPH_INTEGRATION.md).

| Concern | Graph mechanism | BoundaryMesh |
| --- | --- | --- |
| Subscription proof | Validation token echo | Dedicated handler, plain text response |
| Shared secret | `clientState` | Secret Manager; constant-time compare |
| Resource identity | `subscriptionId`, tenant, resource path | Must match registered integration |
| Lifetime | expiration + renewal | Worker renews; no fake permanence |
| Lifecycle | `subscriptionRemoved`, `missed` | Quarantine + operator/system renew |
| Duplicates | possible | Idempotency on notification id |
| HMAC-SHA256 of body | **not** Graph’s model | **do not implement fake HMAC** for Graph |

If Graph payload signatures (e.g. additional validation tokens / certificates) apply to the chosen notification flavor, implement those APIs as documented in Phase 14.

## Replay window store

Access pattern: point get/put on nonce or notification id.

- Key: `{integrationId}/{nonce}` or `{integrationId}/graph/{notificationId}`
- Fields: `seenAt`, `transactionId`
- TTL: freshness window
- Contention: create-if-absent; if exists → replay

Firestore TTL is eventually consistent. Engine also checks `seenAt` in-document. For portfolio scale this is sufficient. Do not add Redis.

## Clock skew

API and partners use UTC. Leeway 60s inside the 300s window. Graph validation handshake has a **tight** timeout — do not run the full engine on handshake.

## Never retry as delivery failure

Invalid signature, expired timestamp, and replay are **not** downstream errors. The API responds 401/403/409 as designed and does **not** publish Pub/Sub.

HTTP codes (ingress):

| Case | Status |
| --- | --- |
| Bad HMAC | 401 |
| Expired timestamp | 401 |
| Replay | 409 with `REPLAY_DETECTED` |
| Graph validation | 200 + token body |
| Duplicate event_id after ALLOW | 200 with prior transaction id |

## Tests (Phase 4)

- valid signature
- tampered body
- wrong secret version
- expired timestamp
- nonce replay
- new nonce, same `event_id` (duplicate, not replay)
- Graph validation echo
- Graph wrong `clientState`
- constant-time path covered by a focused unit test (document the API used)
