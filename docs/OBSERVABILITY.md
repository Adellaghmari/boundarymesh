# Observability

OpenTelemetry + Spring Boot Actuator + Micrometer → Cloud Logging, Monitoring, Trace.

## Trace context

Propagate `correlation_id` (W3C `traceparent` when present). Span attributes (safe):

`transaction_id`, `integration_id`, `connector_id`, `boundary_outcome`, `contract_version`, `reason_code`

Never: raw JWT, Authorization, cookies, secret values, HMAC, OAuth client credentials, PEM, `clientState`, emails, PAN.

## Metrics (names stable)

- `boundarymesh.transactions.allowed`
- `boundarymesh.transactions.transformed`
- `boundarymesh.transactions.rejected`
- `boundarymesh.transactions.quarantined`
- `boundarymesh.replay.attempts`
- `boundarymesh.contract.drift`
- `boundarymesh.delivery.attempts`
- `boundarymesh.delivery.success`
- `boundarymesh.delivery.failure`
- `boundarymesh.quarantine.open`
- `boundarymesh.reprocess.total`
- Resilience4j tagged circuit-breaker and retry meters (local Micrometer only)

Cardinality: outcome and connector_id allowed; **no** subject ids as labels.

## Logs

JSON structured. Production INFO. Actuator `/health` `/info` (git SHA) public; `/metrics` **not** public (Cloud Monitoring scrape via sidecar-less: Cloud Run + OTel exporter). Expose Prometheus only internally if at all — **lock: OTLP exporter, no public /metrics**.

## Self-dogfood (later)

`projectIdentity` document: git SHA, Cloud Run revision, Pulumi stack name, last CI run URL, last AppSec summary. Labeled **LIVE PROJECT DATA**.
