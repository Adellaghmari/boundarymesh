# Phase 10 — Quarantine & Recovery Orchestrator

**Status: COMPLETE (local / TESTED)**

QUARANTINE is now an operational workflow, not only an engine outcome.

## Domain

`QuarantineItem` with status `OPEN` → `REPROCESSING` → `RESOLVED` or `TERMINAL`.

Invalid transitions are rejected. Maximum **3** recovery cycles. After exhaustion the item is `TERMINAL`.

## Reasons that may quarantine

Contract drift/unavailable, egress policy unavailable, credential unavailable, consent invalid, connector unavailable, delivery retries exhausted, provider rate limited / temporary failure.

## Reasons that must not quarantine

Invalid JWT, invalid HMAC, malicious replay. Those remain REJECT.

## Reprocess

Requires `boundary.admin`. Re-evaluates **current** conditions (`transformed-delivery` + active credential + valid consent + closed circuit + SUCCESS provider). It does not blindly mark RESOLVED.

Public recruiter UI is read-only. The Vue console mints a local admin token from the Java issuer. The token is not embedded in frontend source.

## Persistence truth

In-memory repository. Does not survive restart. Firestore remains PLANNED.
