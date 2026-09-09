# Phase 4 — Webhook & Subscription Defense

**Status: COMPLETE** for generic QuayForge HMAC defense. Microsoft Graph subscriptions remain **PLANNED**.

## What this phase proves

A signed partner webhook can be admitted or rejected using real machine controls:

1. Required headers
2. HMAC-SHA256 over **raw request bytes**
3. Timestamp freshness (`Clock`, ±300 seconds)
4. Replay nonce check (atomic in-memory store)
5. JSON parse only after security checks
6. Idempotency on provider `event_id` (separate from replay)
7. `ControlVector` authenticity / freshness / replay → Boundary Enforcement Engine

## QuayForge contract

Headers: `X-Velora-Connector-Id`, `X-Velora-Timestamp`, `X-Velora-Nonce`, `X-Velora-Signature`.

Canonical UTF-8 input:

```text
v1:{timestamp}:{nonce}:{sha256hex(rawBody)}
```

Signature header: `v1=<lowercase-hex HMAC-SHA256>`. Compare with `MessageDigest.isEqual`.

Replay ≠ duplicate:

- Same nonce reused → `REPLAY_DETECTED` / **REJECT**
- Same `event_id`, **new** nonce → `duplicate_delivery: true`, prior outcome, not an attack

Secret: local `WebhookSecretProvider`. Never in Git, Vue, logs, or API bodies.

Graph subscription lifecycle is not implemented and must not be labeled live.
