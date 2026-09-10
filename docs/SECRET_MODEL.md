# Credential and consent vault

Flagship capability #5.

**Current (Phase 9):** local credential references, versions, and a worker-only `SecretValueProvider`. This is not Secret Manager.

**Future:** secrets live in **Google Secret Manager**. Application persistence stores **references**, never values.

## What is stored where

| Item | Secret Manager | Firestore |
| --- | --- | --- |
| Entra client secret (if used) | value | `credential_ref`, `version`, `status`, `rotated_at` |
| HMAC partner secret | value | same |
| Graph `clientState` | value | same |
| KMS private key | **no** — KMS holds it | key name + version |
| Tenant ID, app ID | no (not secrets) | connector metadata |
| Consent (admin consent timestamp, permissions) | no | `consent` metadata |
| Allowed scopes / app roles | no | connector metadata |

Never: plaintext in Git, frontend, logs, attestations, or audit payloads.

## Firestore credential document (shape)

```text
credentials/{credentialId}
  connector_id
  kind            ENTRA_CLIENT_SECRET | HMAC | GRAPH_CLIENT_STATE
  secret_resource projects/.../secrets/.../versions/N   (or "latest" resolved at runtime to pin)
  pinned_version  integer
  status          ACTIVE | ROTATING | DISABLED | FAILED
  rotation        { started_at, from_version, to_version }
  updated_at
```

No `secret_value` field. Code review must fail if one is added.

## Resolution

Only `sa-delivery-worker` holds `secretmanager.secretAccessor` on named secrets.

`boundary-api` may read **metadata** (version number, status) via Firestore. It must not call `accessSecretVersion`.

If the worker cannot access the secret: `CREDENTIAL_UNAVAILABLE` → quarantine. Fail closed for delivery, not fail open with empty auth.

## Rotation design

1. SECURITY_ADMIN starts rotation.
2. New Secret Manager version added (value entered in admin UI **once**, sent to API, API writes to Secret Manager, **does not persist body**).
3. Status `ROTATING`; worker dual-read optional for HMAC only if partners need overlap — **default: pin to new version after health check**.
4. Prove: requests signed with **old** HMAC fail; new succeeds.
5. Disable old version; status `ACTIVE`.
6. Old version destroy after idle period.

Entra client-secret rotation: new secret in Entra, stored as new SM version, Graph SDK uses new version; old Graph calls 401 — quarantine until complete. Prefer **federated credentials** (WIF-like for Entra) in a later hardening if the tenant supports it, to avoid client secrets entirely. Phase 0 does not require that; it is a Phase 19 candidate.

## Consent metadata (Entra / Graph)

```text
consent:
  admin_consented: true | false | unknown
  permissions: ["Calendars.Read"]
  permission_type: APPLICATION
  recorded_at
  source: MANUAL | GRAPH_SP
```

This is **not** a secret. It is product evidence of least privilege.

## Proof later (Phase 9 / 18)

- Worker logs contain `secret_version=3` never the value
- Firestore emulator tests assert documents have no value keys
- Gitleaks on the repo
- Rotation demo: old version rejected, new active

## IAM

Separate secrets:

- `hmac-quayforge`
- `entra-helix-client`
- `graph-client-state`

Bindings: worker SA only, one secret at a time. CI SA: none.
