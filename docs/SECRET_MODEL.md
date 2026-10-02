# Credential and consent vault

Flagship capability #5.

**Current:** local credential references remain the default non-prod path. Production adapters
resolve Secret Manager. Four containers exist with one enabled synthetic version each. Hosted
Worker IAM can read the three connector secrets; the live 15J path stopped before that read.
HMAC `hmac-quayforge` is authorized for the API.

Application persistence stores **references**, never values.

## Phase 15I-B2 payload contract

All four values are independent 32-byte CSPRNG outputs encoded as 43-character, unpadded Base64URL
ASCII with no whitespace or newline. `hmac-quayforge` supplies those exact textual bytes to the
QuayForge HMAC verifier. `covecrm-api`, `lumenway-api`, and `rivertide-api` are opaque bearer-token
text used only by the worker connector client. The demo systems and their credentials are synthetic.
No payload is managed by Pulumi or recorded in Git, documentation, chat, test reports, or temp files.
The production composition pins all three worker credential references to version `1`; the local
credential lab retains its separate v1/v2 rotation scenario.

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

The worker receives `secretmanager.secretAccessor` only on the three named provider secrets.
The API receives it only on `hmac-quayforge`, which its webhook authenticity path resolves.

`boundarymesh-api` accesses only `hmac-quayforge`. It must not call `accessSecretVersion` for any
worker connector secret.

If the worker cannot access the secret: `CREDENTIAL_UNAVAILABLE` → quarantine. Fail closed for delivery, not fail open with empty auth.

## Rotation design

1. SECURITY_ADMIN starts rotation and records only the current version metadata.
2. New Secret Manager version is added through a controlled interactive administrator procedure;
   the value never enters Pulumi config, source, chat, or logs.
3. Verify the intended workload with the new version, then pin or promote it.
4. Disable the prior version and observe provider/webhook behavior during the retention period.
5. Destroy the disabled prior version only after an explicit review. Never create, disable, or
   destroy versions as an incidental Pulumi deployment.

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
- a dedicated Entra client secret container only when production OIDC is explicitly enabled
- `graph-client-state`

Bindings: each workload SA receives one additive binding per required secret. CI SA: none.
