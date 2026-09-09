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

HMAC-SHA256 over that UTF-8 string. Phase 4 uses a local `WebhookSecretProvider` (env or generated ephemeral). Secret Manager remains **PLANNED**.

### Verification steps

1. Load secret **bytes** via reference; never log.
2. Compute HMAC.
3. Compare with `MessageDigest.isEqual` (constant-time).
4. Reject if timestamp outside **±300 seconds** (configurable per integration, max 600).
5. Reject if nonce seen in the replay window (Phase 4: in-memory `ReplayNonceStore` with expiry and `recordIfAbsent`. Firestore adapter remains **PLANNED**).
6. Then parse JSON, apply event-id idempotency, and run the engine. Contract Drift Firewall is **not** Phase 4.

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

HTTP codes (Phase 4 local QuayForge path):

| Case | Status |
| --- | --- |
| Missing required headers | 400 |
| Wrong `Content-Type` | 415 |
| Body over 65536 bytes | 413 |
| Signed-path outcomes (valid, bad HMAC, stale/future, replay, duplicate) | **200** + `DecisionResponse` (`ALLOW` / `REJECT`, reason codes) |
| Graph validation | **PLANNED** |

The 200 + bounded decision body is intentional: the response is not a signature oracle (no expected HMAC, no secret, no nonce store dump). Future production ingress may tighten HTTP codes without changing the control model.

Max body: 65536 bytes. Reject before unbounded buffering (`BoundedRawBodyFilter`).

Future distributed rate limiting is **PLANNED**. This is not production DDoS protection.

## Tests (Phase 4)

Implemented:

- valid signature
- invalid / missing signature
- expired and future timestamp
- nonce replay
- new nonce, same `event_id` (duplicate, not replay)
- concurrent nonce race on `recordIfAbsent`
- oversized payload (413)
- wrong content type (415)
- constant-time compare via `MessageDigest.isEqual`
- lab helper does not return the secret or `v1=` signature

Not implemented:

- Graph validation echo
- Graph `clientState`
- Secret Manager versioned secrets
