# Phase 15J final freeze — hosted production verification

Status: **LIVE VERIFIED / PASS.** Phase 15J-C3A and Phase 15J-C3B both passed. This
document is the authoritative Phase 15J freeze record and supersedes pending-language in the
individual historical gate records.

## Hosted architecture

The verified production path is:

```text
Firebase-hosted Vue SPA
  → Firebase Google authentication
  → Secure, HttpOnly, SameSite=Lax, host-only __session
  → framework and application CSRF
  → boundarymesh-web BFF
  → keyless Google workload identity
  → IAM-private boundarymesh-api
  → Firestore / Cloud KMS / audit / outbox
  → Pub/Sub
  → boundarymesh-worker
  → durable terminal state
  → ACK
```

Firebase Hosting has three releases. HTML shell responses use `no-cache`; hashed assets use
long-lived immutable caching. Google sign-in, `POST /session/login` 200, `GET /session/me` 200,
Firebase session verification, verified-email and recent-sign-in checks, operator authorization,
CSRF, and the strict BFF route allowlist are live verified. A retained-evidence read completed
through the BFF in 17.284 seconds and the private core in 16.561 seconds. The BFF therefore keeps
a bounded 30-second request timeout, a five-second connection timeout, and no retries. This is
functional timeout correctness, not a performance claim.

The live BFF is `boundarymesh-web-00004-k48` on immutable digest
`sha256:90570617750bd39613482fd75d8434f1a47299ae8d35908ae0c81717374ee30a`.
The BFF alone has public Cloud Run transport. Application authentication remains mandatory.
The core has no `allUsers`, no `allAuthenticatedUsers`, and no project-wide Cloud Run Invoker
grant. The BFF service account has `roles/run.invoker` on the core service only.

## Exactly one controlled browser transaction

One manual hosted browser action produced exactly one BFF transaction POST and exactly one
private-core transaction POST:

| Evidence | Value |
| --- | --- |
| Transaction | `d27ea454-941f-4193-b94c-f33637309bf1` |
| Instruction | `dlv-4eea6f0b-38b9-4598-bf0f-d931451ac9bf` |
| Attestation | `cc01172f-ae91-4ab5-95b6-bcac274e72e7` |
| Integration / contract | `quayforge-orders` / `v1` |
| Source / destination | `QUAYFORGE` / `RIVERTIDE` |
| Decision | `ALLOW` |
| Submission | `ACCEPTED_FOR_ASYNCHRONOUS_DELIVERY` |

The request contained only synthetic BoundaryMesh demo data. It created one delivery instruction,
one attestation, four append-only audit entries, one outbox publish, one Worker receipt, one durable
idempotency result, and one terminal result. No duplicate business processing occurred.

## Cryptographic and audit evidence

The preserved attestation is `SIGNED` with `ES256` using Cloud KMS key version 1. The backing key
uses `EC_SIGN_P256_SHA256`, remains `ENABLED`, and never exposes its private key. Production
verification after cleanup returned `VALID` for the exact transaction and `ALLOW` outcome.

The `quayforge-orders` audit chain grew from sequences 1–4 to contiguous sequences 1–8:

- 5 — `BOUNDARY_DECISION`
- 6 — `DELIVERY_INSTRUCTION_CREATED`
- 7 — `DELIVERY_RESULT`
- 8 — `ATTESTATION_CREATED`

Entries 1–4 were not rewritten. Production verification checked all eight entries and returned
`VALID` with no break sequence. Sequence 7 records the core-stage pre-worker result
`NOT_ATTEMPTED`; the later Worker result is independently `QUARANTINED`. They are different
pipeline stages.

## Outbox, Pub/Sub, and terminal custody

Before cleanup, the exact outbox record reached `PUBLISHED` with zero failed publish attempts.
Telemetry showed one relevant topic publish, one Worker receipt, one ACK, zero NACKs, zero expired
ACK deadlines, zero redelivery, zero DLQ publication, and zero pending/oldest-unacknowledged
backlog.

The Worker persisted:

| Field | Value |
| --- | --- |
| Terminal state | `QUARANTINED` |
| Provider category | `CREDENTIAL_OR_CONSENT` |
| Reason | `CONNECTOR_UNAVAILABLE` |
| Connector / route | `rivertide` / `sink.rivertide.instruction` |
| `provider_called` | `false` |
| Attempt count | `0` |

No Rivertide endpoint was configured. The Worker checked endpoint availability before credential
resolution, so no external HTTP call, connector-secret retrieval, or provider credential exposure
occurred. The handler synchronously persisted terminal/idempotency state before returning the ACK
classification; the Pub/Sub consumer acknowledged only after that return.

## Cleanup and retained evidence

The cleanup gate removed only these disposable run-scoped documents:

- `deliveryOutbox/dlv-4eea6f0b-38b9-4598-bf0f-d931451ac9bf`
- `deliveryIdempotency/dlv-4eea6f0b-38b9-4598-bf0f-d931451ac9bf`

It preserved:

- `attestations/cc01172f-ae91-4ab5-95b6-bcac274e72e7`
- `auditChains/quayforge-orders/entries/00000000000000000001` through
  `00000000000000000008`
- shared materialization `observedCrossings/RIVERTIDE`

No standalone transaction or quarantine document exists. No pending Pub/Sub message or DLQ copy
remains. Post-cleanup production verification kept the attestation and audit chain `VALID`.

## Frozen runtime and infrastructure truth

- `boundarymesh-web-00004-k48` is Ready on
  `sha256:90570617750bd39613482fd75d8434f1a47299ae8d35908ae0c81717374ee30a`.
- `boundarymesh-api-00004-9hw` is Ready on
  `sha256:df4ca576227b1311194af1b3c821962a228bc7222d144eb4b4fbfb41590e072d`.
- `boundarymesh-worker-00002-v6j` is Ready on
  `sha256:a031fd689c80faef122b2eb7a35c35c6cb04035f333c493e5d7decea1d08b76a`.
- API, Worker, and Web workload service accounts have zero user-managed keys.
- Firestore, Pub/Sub, KMS, Secret Manager, Firebase Auth, Firebase Hosting, IAM, and the frozen
  Microsoft/Entra integration remain healthy and unchanged by the transaction.
- Compute Engine API remains disabled.
- Final Pulumi preview: **39 unchanged; zero creates, updates, replacements, or deletes; exit 0**.
- Firebase Hosting releases are intentionally outside Pulumi ownership.

Reviewed browser and application evidence contained no Cloud Run workload token, service-account
credential, Authorization bearer credential, private key, Firebase ID token, Google OAuth token,
session-cookie value, CSRF value, OAuth client secret, password-hash material, or Pulumi
passphrase.

## Claims and limits

Phase 15J proves the hosted authenticated operator console, secure same-origin BFF, keyless access
to an IAM-private core, one browser-origin governed transaction, KMS-backed attestation, audit
continuity, transactional outbox, Pub/Sub delivery, Worker idempotency, durable quarantine before
ACK, and no observed redelivery.

It does **not** prove real external-provider delivery, high throughput, low latency, multi-user
production scale, WIF/CI-CD, or a whole-project security freeze.

Two accepted credential-hygiene follow-ups remain, and their values must not be retrieved or
repeated:

1. The current Pulumi passphrase was exposed in chat. The user explicitly chose not to rotate it.
2. Firebase Auth password-hash signing material appeared in local command output. It was not
   committed or stored in the repository, and the email/password provider is disabled.

The earlier Phase 15H Pulumi passphrase incident was separately remediated; it is distinct from the
current accepted-risk passphrase.

The next roadmap phase is **Phase 16 — remaining Pulumi / GCP infrastructure**. Do not begin it,
WIF/CI-CD, or the final security-hygiene work without a separate gate.
