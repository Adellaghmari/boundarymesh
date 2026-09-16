# ADR 0004 — GCP Cloud Run Service and Worker Pool

## Status

Accepted.

## Context

A=Azure, B=AWS. C needs GCP. Serverless/pay-per-use. No GKE.

## Decision

Primary cloud **GCP `europe-west4`**. Workloads:

1. `boundarymesh-api` — Cloud Run Service, synchronous enforcement
2. `boundarymesh-worker` — Cloud Run Worker Pool, continuous pull from Pub/Sub, no endpoint

The API uses request-based billing and **min instances = 0**. Worker Pools do not have
request ingress or native autoscaling, so the worker is designed with one manually scaled
instance for the later deployment gate.

## Why GCP?

Portfolio cloud breadth and a real cross-cloud story (M365 → GCP). Not “AWS with renamed services.”

## Why Cloud Run?

Managed containers, dedicated IAM service accounts, enough CPU for JVM, and no GKE/VMs. The
Worker Pool matches the long-running pull consumer without inventing a public worker URL.

## Consequences

The API stays IAM-authenticated in the Phase 15I-A preview. Browser ingress and application
authentication require a later explicit design decision before deployment. The worker's fixed
instance has an idle cost when deployed. Java image size: use a JRE 25 distroless-style image in
the deployment phase.
