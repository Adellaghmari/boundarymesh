# Documentation

These documents describe the software in this repository. The root [README](../README.md) is the project entry point.

## Architecture and product

| Document | Topic |
| --- | --- |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Topology, service boundaries, and enforcement flow |
| [PRODUCT.md](PRODUCT.md) | What the product is, and what it is not |
| [API_CONTRACTS.md](API_CONTRACTS.md) | OpenAPI, AsyncAPI, and JSON Schema |
| [DATA_MODEL.md](DATA_MODEL.md) | Firestore collections and delivery records |
| [adr/README.md](adr/README.md) | Architecture decision records |

## Security and identity

| Document | Topic |
| --- | --- |
| [SECURITY.md](SECURITY.md) | Controls, trust boundaries, secrets, and known gaps |
| [THREAT_MODEL.md](THREAT_MODEL.md) | Assets, actors, and accepted threats |
| [IDENTITY_AND_ACCESS.md](IDENTITY_AND_ACCESS.md) | Hosted sessions, local Entra, and Graph |

## Infrastructure and delivery

| Document | Topic |
| --- | --- |
| [INFRASTRUCTURE.md](INFRASTRUCTURE.md) | GCP topology, Pulumi ownership, and cost limits |
| [DELIVERY_AND_RECOVERY.md](DELIVERY_AND_RECOVERY.md) | Outbox, worker, quarantine, and recovery |
| [CI_CD.md](CI_CD.md) | Keyless workflows, the frozen release, and rollback |
| [OBSERVABILITY.md](OBSERVABILITY.md) | Logs, health, and what is not instrumented |

## Testing and development

| Document | Topic |
| --- | --- |
| [TESTING.md](TESTING.md) | Local test suites and opt-in GCP checks |
| [LOCAL_DEVELOPMENT.md](LOCAL_DEVELOPMENT.md) | JDK 25 and Node 22 commands |
| [DEMO.md](DEMO.md) | How to walk through the product |
