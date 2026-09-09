# ADR 0004 — GCP and two Cloud Run services

## Status

Accepted.

## Context

A=Azure, B=AWS. C needs GCP. Serverless/pay-per-use. No GKE.

## Decision

Primary cloud **GCP `europe-west4`**. Workloads:

1. `boundary-api` — public ingress, sync enforcement
2. `delivery-worker` — pull Pub/Sub, no public ingress

Request-based billing, **min instances = 0**.

## Why GCP?

Portfolio cloud breadth and a real cross-cloud story (M365 → GCP). Not “AWS with renamed services.”

## Why Cloud Run?

Scale-to-zero containers, IAM service accounts, enough CPU for JVM, cheaper than GKE/VMs. Two services because **secret and Graph access must not sit on the public ingress identity**.

## Consequences

Cold starts: acceptable for portfolio; document on Architecture page. Java image size: use a JRE 25 distroless-style image in Phase 16.
