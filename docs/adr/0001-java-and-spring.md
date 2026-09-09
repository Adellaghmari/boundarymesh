# ADR 0001 — Java 25, Spring Boot 4.1, Spring Security 7.1

## Status

Accepted.

## Context

Project C must demonstrate a third backend family (not Python/FastAPI, not Go/Gin) with real IAM and API security.

## Decision

- **Java 25 LTS** (Eclipse Temurin).
- **Spring Boot 4.1.1** (OSS line through mid-2027; 4.0 OSS ends Dec 2026).
- **Spring Security 7.1.x** (BOM) for OAuth2 resource server, OIDC BFF, method security.
- Maven multi-module later; two Spring Boot apps.

## Why Java?

Enterprise integration + Spring Security + Camel + Graph Java SDK is a coherent JVM story. Interview value for Java backend / integration roles. Not chosen because “enterprise sounds good.”

## Why Spring Boot?

Application composition, Actuator, testing, Cloud Run fat-jar/image. Not Quarkus/Micronaut (those would split the CV without fitting Camel/Security depth).

## Why Spring Security?

JWT/JWKS, OIDC, CSRF, method security are solved problems. Hand-rolled filters would be a defect. Entra is configured **as an IdP**, not via Azure-hosting starters as the primary stack.

## Consequences

Need Java 25 locally in Phase 1. Camel 4.22.0 supports 17/21/25. Jackson 3 via Boot 4 — attestation still uses RFC 8785, not default Jackson.
