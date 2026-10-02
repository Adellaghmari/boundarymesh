# Observability

Current state is **platform logs + health endpoints + local Micrometer counters**.
There is **no** OpenTelemetry dependency, **no** OTLP exporter, **no** Cloud Trace pipeline,
and **no** BoundaryMesh-managed alert policies.

## What exists now

- Cloud Run request and application logs (platform-automatic)
- Spring Boot default logging
- Health: API `/api/v1/health` and `/api/v1/ready` (IAM-private); Web `/actuator/health`
- Local Micrometer counters on delivery/quarantine paths and Resilience4j tagged meters
- GCP console metrics for Cloud Run and Pub/Sub (platform-automatic; not BM dashboards)

`/metrics` is not a public product surface.

## What does not exist

- OpenTelemetry Java agent or Spring OTel starter
- Configured Cloud Trace export
- Production alert policies verified in this project
- BoundaryMesh-managed Pub/Sub or DLQ alerts
- A `projectIdentity` self-dogfood document

## Future / optional

OpenTelemetry, tracing, and alerting may be added later. They are not implemented and are not
required for current CV readiness. See [ROADMAP.md](ROADMAP.md).

Safe log/trace attributes, **if tracing is ever added**, must still exclude secrets, tokens,
cookies, HMAC material, emails, and PAN. Until then, do not claim trace context propagation.
