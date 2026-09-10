# Phase 9 — Credential & Consent Boundary

**Status: COMPLETE (local security model / TESTED)**

Product name may remain **Credential & Consent Vault**. This phase is the domain/application boundary plus a local provider. Google Secret Manager is **not** implemented.

## What exists

- Strong types: `CredentialId`, `CredentialVersion`, `CredentialStatus`, `CredentialReference`, `ConnectorIdentity`
- Consent types: `ConsentGrant`, `ConsentStatus`, `Permission`, `ConnectorOperation`, `GrantKind` (`APPLICATION` / `DELEGATED` for future Entra)
- Port: `SecretValueProvider` — resolve material by reference/version only when a connector is about to call
- Local adapter in the **worker** only. Test secrets are unmistakably non-production (`TEST-SECRET-…-NOT-FOR-PRODUCTION`)
- Public API never depends on the secret provider
- ControlVector `credentialResolvability` is a real evaluator

## Rotation

CoveCRM v1 RETIRED, v2 ACTIVE. Local rotate activates a new version and retires the previous. New deliveries use the active version. Retired versions are not a silent fallback.

## Credential vs consent

| Case | Result |
| --- | --- |
| Credential missing / revoked / unavailable | QUARANTINE (`CREDENTIAL_*`) — no Camel call |
| Consent missing / revoked | QUARANTINE (`CONSENT_*`) — no Camel call even if the reference is active |
| Both valid | Crossing may produce a delivery instruction |

Has a secret ≠ is authorized.

## Honest limits

- No Secret Manager
- No encrypted local vault database
- No Entra / Graph consent
- Secret values must not appear in API JSON, ControlVector, logs, or Vue
