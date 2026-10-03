# Delivery and recovery

Enforcement finishes before delivery starts. The worker never sees a rejected payload as an instruction.

## Lifecycle

1. The API builds a control vector: identity, authorization, authenticity, freshness, replay, contract, egress, credential metadata, and connector safety.
2. The engine returns allow, allow with transformation, quarantine, or reject.
3. Allow outcomes persist a delivery instruction containing the approved payload only.
4. The hosted API writes that instruction with a transactional outbox, then publishes to Pub/Sub topic `boundarymesh-deliveries`.
5. The worker pulls subscription `boundarymesh-delivery-worker`.
6. Camel translates and routes. Resilience4j wraps outbound HTTP.
7. The worker records delivered, duplicate suppressed, or quarantined, then acknowledges the message.

Reject writes evidence and stops. Quarantine at the engine does not start Camel.

## Outbox and Pub/Sub

The outbox and the publish are the hosted durability path. The worker is a pull consumer, so a slow destination does not hold the API request open.

There is one internal bus. There is no second broker. A dead-letter topic exists in infrastructure. Dead-letter forwarding is not configured, so a poison message is not automatically moved.

## Worker behavior

The worker resolves a connector credential from Secret Manager at use time. On the hosted path the connector endpoint is empty, so that retrieval does not run. Camel idempotent consumption suppresses duplicates. The dead-letter channel inside Camel is an application route. It is not Google Cloud dead-letter forwarding.

The worker has no HTTP server in production. Health for the local worker is `GET /api/v1/health` on its local port. That port is not a public Cloud Run URL.

## Failure handling

Security failures close. Availability failures quarantine. Nothing retries forever.

Invalid identity, a bad HMAC, an expired timestamp, replay, and an egress deny reject the crossing. They do not enter quarantine and they do not publish. An unsupported contract can quarantine so an operator can see the drift. A circuit that is open does not call the provider.

Outbound HTTP allows three attempts in total, counting the first call. Camel does not add its own redeliveries. Pub/Sub may redeliver. The worker treats a repeated instruction as a duplicate. There is no dead-letter policy, so redelivery must not be described as an unbounded second retry budget. HTTP 400, 401, and 403 are not retried. HTTP 429, 500, 503, and timeouts are retried inside that budget, then quarantined.

A signing failure returns an unsigned attestation and keeps the crossing outcome.

## Quarantine states

An item moves from `OPEN` to `REPROCESSING` when an operator starts recovery. Success resolves it. Another failure returns it to `OPEN` until the recovery cap, then `TERMINAL`. Only an open item can start reprocess. Security rejects never enter this workflow.

Local reprocess is `POST /api/v1/quarantine/{id}/reprocess` and requires `boundary.admin`. The hosted page does not offer anonymous reprocess. The hosted synthetic transaction reached worker quarantine and acknowledgement without an external call.

Deployment failure is separate from message failure. A failed stage does not promote. A failed promotion does not leave a promotion-evidence document unless the promotion itself completed.

## Recovery of promotion evidence

If promotion succeeded and the immutable evidence object is missing, `recover-promotion-evidence.yml` can publish that object. Preconditions are a successful stage, a successful promotion, production still matching that promotion, and an absent evidence version.

The recovered document is bound to the original promotion run, not to the recovery run. A collision fails closed. The workflow cannot promote, roll back, or change runtime and Hosting. It is break-glass, not a second promotion. See [CI_CD.md](CI_CD.md).

## Rollback boundaries

Rollback is a dispatch on `deploy-release.yml`. It can move API and web traffic and worker instance splits back to preserved revisions, and it can restore a preserved Hosting version. Those identities are live-readable. Rollback has not been executed against the live environment. Do not describe a zero-downtime rollback as proven.
