# Product

BoundaryMesh decides whether one system may exchange data or an action with another. A successful HTTP status code is not that decision.

A crossing is an untrusted payload leaving one trust domain and entering another. The engine checks identity, authorization, authenticity, freshness, and replay. It validates the integration contract and decides which fields may leave. The outcome is `ALLOW`, `ALLOW_WITH_TRANSFORMATION`, `QUARANTINE`, or `REJECT`. Allowed work becomes a delivery instruction. The system records the controls it applied.

This is not an API dashboard, an integration platform, an AI assistant, or a release board.

## What a reader can see

The public page explains the system. `/explore` shows a synthetic crossing without sign in. Neither page submits a real transaction.

The operator console can submit one synthetic transaction after Google sign-in. That path is the hosted proof that the web edge, private API, outbox, Pub/Sub, and worker agree. It does not call a partner API.

The local lab is the full walkthrough: HMAC webhooks, contract and egress checks, quarantine, audit, attestation, and one bounded Microsoft Graph calendar pull against a personal account.

## Differentiation that is in the software

- The untrusted request stops at the boundary. Destinations see a normalized instruction.
- Controls are typed Java results, not a prompt and not a gateway plugin.
- Transformation is an explicit outcome. Redacted fields are part of the decision, not a silent filter.
- Quarantine is a durable hold with a reason. It is not a log line.
- Exchange attestations are signed in Cloud KMS. Release artifacts are not signed by that key.
- Publication, staging, promotion, rollback, and evidence recovery are separate human-gated operations.

## Honest limits

The Google Cloud project is a portfolio and development environment. It is not enterprise production and it does not carry paid traffic.

Production Microsoft Entra is disabled. Graph change notifications are not implemented. External connector delivery is disabled. Dead-letter forwarding is not configured. Rollback is implemented and has not been executed against live traffic. SBOM and provenance are unsigned. There is no OpenTelemetry pipeline and no application pull-request workflow.

## Not in this repository

Later work could add dead-letter forwarding, tracing, or a secret scanner in CI. None of that is required to understand or run the system as it stands. Do not describe those items as present.
