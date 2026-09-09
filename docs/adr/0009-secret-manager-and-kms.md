# ADR 0009 — Secret Manager and Cloud KMS

## Status

Accepted.

## Context

Connector secrets and a real signing purpose are required. Private keys must not live in Git or Firestore.

## Decision

- **Secret Manager**: HMAC, Entra client secret (if any), Graph `clientState`. Firestore stores references + versions.
- **Cloud KMS**: `EC_SIGN_P256_SHA256` for Cryptographic Exchange Attestation. SOFTWARE protection.

## Why Secret Manager?

GCP-native secret IAM at secret level; rotation versions; worker-only accessor. API SA denied.

## Why KMS?

Asymmetric sign where the private key never leaves KMS is the correct primitive for attestations. Not custom ECDSA. Not “encrypt the database and call it cryptography.”

## Consequences

~$0.06/month/key version. Signing failure → UNSIGNED attestation, never a fake signature.
