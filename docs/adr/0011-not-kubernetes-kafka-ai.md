# ADR 0011 — Why not Kubernetes, Kafka, or AI

## Status

Accepted.

## Why not Kubernetes / GKE?

Two Cloud Run services with scale-to-zero already match the trust split. GKE adds idle node cost, cluster upgrades, and a DevOps story Project B already covers. **NOT APPROPRIATE.**

## Why not Kafka?

One delivery topic with DLQ does not need a log cluster. Pub/Sub is the GCP-native bus. Kafka would be a keyword. **NOT APPROPRIATE.**

## Why not AI / LLM / RAG?

The product question is a deterministic policy crossing, not whether information is true or whether a model should be trusted. AI would collapse C into A. **NOT APPROPRIATE.**

## Also rejected

Redis, RabbitMQ, service mesh, Spring Cloud Gateway, Quarkus, Micronaut, Next.js, ASP.NET, extra OLTP databases.
