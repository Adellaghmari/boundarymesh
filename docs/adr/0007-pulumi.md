# ADR 0007 — Pulumi TypeScript

## Status

Accepted.

## Context

B uses Terraform. C must not.

## Decision

Pulumi 3.x + TypeScript + `@pulumi/gcp`. GCS DIY state with versioning. KMS as secrets provider. Gated `pulumi up`.

## Why Pulumi?

Real IaC with TypeScript (shared language with Vue/infra typing), distinct from Terraform, still preview/update/state. Not because YAML is unfashionable.

## Consequences

Bootstrap GCS bucket once. No duplicate console ownership of Cloud Run.
