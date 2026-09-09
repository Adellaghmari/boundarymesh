# ADR 0005 — Firestore Native

## Status

Accepted.

## Context

Need a GCP-native store. A already uses PostgreSQL. B already uses DynamoDB.

## Decision

**Firestore Native**, regional `europe-west4`, access-pattern-first collections (see DATA_MODEL.md).

## Why Firestore?

Matches point gets (nonce, idempotency, transaction), serverless, no instance cost. Not modeling SQL tables.

## If it fails

Only then consider another **GCP-native** store with a new ADR. Not Postgres, not DynamoDB, not Redis-as-DB.

## Consequences

Composite indexes kept minimal. Outbox field for Pub/Sub publish-after-commit.
