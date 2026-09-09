# Phase 1 implementation

Phase 1 is the **local Java/Spring domain foundation**. It is not production IAM, Graph, Camel, or GCP.

## Verified toolchain

Recorded 2026-09-09 on this machine:

| Tool | Actual |
| --- | --- |
| Java | `openjdk 25.0.4.1 2026-08-18 LTS` (Eclipse Temurin `25.0.4.1+1`) |
| Maven Wrapper | **3.9.11** (`backend/mvnw.cmd`) |
| Spring Boot | **4.1.1** (parent POM) |
| Spring Security | **7.1.1** — resolved from Boot BOM, not pinned in our POM |
| Tests | `mvnw.cmd verify` — **38** tests, 0 failures |

Commands:

```text
java --version
# openjdk 25.0.4.1 2026-08-18 LTS
# OpenJDK Runtime Environment Temurin-25.0.4.1+1 (build 25.0.4.1+1-LTS)

cd backend
mvnw.cmd --version
# Apache Maven 3.9.11
# Java version: 25.0.4.1, vendor: Eclipse Adoptium
```

## Package namespace

`dev.adellaghmari.boundarymesh`

Portfolio-stable, not `com.example`, not a fake corporation. Subpackages: `domain`, `application`, `api`.

## Modules (Phase 1)

Fewer modules than the full Phase 0 sketch, on purpose. No empty placeholders.

```text
backend/
  pom.xml                 aggregator, Boot 4.1.1, Java 25, Spotless, Enforcer
  domain/                 pure Java types
  application/            BoundaryEnforcementEngine (no Spring)
  api/                    Spring Boot app
```

**Planned later (not created empty):** `security`, `integration`, `connectors`, `camel`, `persistence`, `audit`, `delivery`, `worker`.

## Domain

Identifiers: `TransactionId` (UUID), `IntegrationId` (slug).  
Enums: `SourceSystem`, `DestinationSystem`, `TransactionState`, `ControlKind`, `ControlStatus`, `BoundaryOutcome`.  
Records: `BoundaryTransaction`, `ControlResult`, `ControlVector`, `ReasonCode`, `BoundaryDecision`, `PolicyVersion`.

### ControlStatus

| Status | Meaning |
| --- | --- |
| `PASS` | Control evaluated; policy condition satisfied |
| `FAIL` | Control evaluated; policy condition not satisfied |
| `NOT_APPLICABLE` | Control does not apply; does not drive the outcome (except required identity/authorization, which fail closed) |
| `UNAVAILABLE` | Control could not be evaluated |

No confidence scores.

### BoundaryOutcome

| Outcome | Meaning |
| --- | --- |
| `REJECT` | Must not cross (security fail/unavailable, schema invalid, egress deny) |
| `QUARANTINE` | Withheld for a recoverable operational reason |
| `ALLOW_WITH_TRANSFORMATION` | May cross only after deterministic transformation |
| `ALLOW` | May continue unchanged |

### Precedence

`REJECT > QUARANTINE > ALLOW_WITH_TRANSFORMATION > ALLOW`

A security rejection cannot be overridden by redaction. A hold cannot be overridden by a transform that would otherwise allow the crossing. Required security `UNAVAILABLE` fails closed to `REJECT`. Credential/connector `UNAVAILABLE` quarantines.

### Clock

`BoundaryEnforcementEngine` takes `java.time.Clock`. Tests use `Clock.fixed`.

## API

| Method | Path | Notes |
| --- | --- | --- |
| GET | `/api/v1/health` | process up |
| GET | `/api/v1/ready` | Phase 1 has no downstream deps |
| POST | `/api/v1/dev/evaluate` | **local/test only**, `synthetic: true` required |
| GET | `/v3/api-docs` | OpenAPI **3.1** generation — not runtime contract enforcement |

Actuator: only `health` (+ probes). Not exposed: env, beans, configprops, heapdump, metrics.

Unlisted routes: unauthenticated `401` sanitized JSON (`UNAUTHORIZED`). No stack traces.

## Security truth

Spring Security **is on the classpath** with a **local-only** filter chain. This is **not** Entra, OAuth2, OIDC, or JWT/JWKS validation.

## Quality

- Spotless (Palantir Java Format) on `verify`
- Compiler `-Xlint:all` (minus serial/processing)
- Enforcer: Java `[25,26)`, Maven `>= 3.9`
- ArchUnit: domain and application must not depend on Spring, GCP, Microsoft, or Camel

## How to run

Requires JDK 25 on `JAVA_HOME`.

```powershell
cd backend
.\mvnw.cmd verify
.\mvnw.cmd -pl api -am spring-boot:run
```

Then:

```powershell
curl http://localhost:8080/api/v1/health
curl http://localhost:8080/api/v1/ready
```

## Synthetic evaluate example

```json
{
  "synthetic": true,
  "transaction_id": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
  "integration_id": "helix-calendar-crm",
  "source_system": "HELIX_365",
  "destination_system": "COVECRM",
  "controls": {
    "identity": { "status": "PASS", "reason_codes": [], "safe_explanation": "Synthetic." },
    "authorization": { "status": "PASS", "reason_codes": [], "safe_explanation": "Synthetic." },
    "authenticity": { "status": "PASS", "reason_codes": [], "safe_explanation": "Synthetic." },
    "freshness": { "status": "PASS", "reason_codes": [], "safe_explanation": "Synthetic." },
    "replay": { "status": "PASS", "reason_codes": [], "safe_explanation": "Synthetic." },
    "contract": { "status": "PASS", "reason_codes": [], "safe_explanation": "Synthetic." },
    "egress": { "status": "PASS", "reason_codes": [], "safe_explanation": "Synthetic." },
    "credential_resolvability": { "status": "PASS", "reason_codes": [], "safe_explanation": "Synthetic." },
    "connector_safety": { "status": "PASS", "reason_codes": [], "safe_explanation": "Synthetic." }
  }
}
```
