# Credential and consent model

See also [SECRET_MODEL.md](SECRET_MODEL.md) for the future Secret Manager shape.

## Current vs future

| Layer | Now | Later |
| --- | --- | --- |
| Product name | Credential & Consent Vault | same |
| Storage | Local catalog + ephemeral worker secrets | Google Secret Manager |
| Metadata | In-memory references/versions/status | Firestore |
| Consent | Generic grant / permission / operation | Entra admin-consent + Graph scopes |

Do not describe the local catalog as a managed vault.

## Just-in-time resolution

The public API evaluates **resolvability** from metadata. The worker calls `SecretValueProvider.resolve(reference)` only after security, contract, egress, and engine approval, immediately before the connector HTTP call.

Secrets are not loaded at worker startup for every connector. They are not placed on `BoundaryDecision`, `ControlVector`, or quarantine records.

## Consent metadata (Entra-ready, not Entra)

`GrantKind.APPLICATION` and `GrantKind.DELEGATED` exist so a later Graph adapter can represent app roles vs delegated scopes, plus tenant/admin-consent fields. The generic core does not hard-code M365.

## Recruiter evidence

Allowed: connector, credential id, version, status, rotation timestamp, consent status, permission names.

Forbidden: client secret, API token, refresh token, access token, HMAC secret, private key.
