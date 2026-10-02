# Phase 16C final evidence

> **Historical freeze record (Phase 16C.5).** Resource counts and WIF context counts in this file
> are true as of that freeze (54 managed, two contexts). Current truth:
> [PROJECT_STATUS.md](../PROJECT_STATUS.md).

Status: **CLOSED / PASS / LIVE VERIFIED** after the Phase 16C.5 documentation freeze.

## Shared Pulumi state

- project/stack: `boundarymesh/dev`
- CLI baseline: `3.262.0`
- backend: `gs://bm-pulumi-state-eu4-21cad20b325f`
- secrets provider: `gcpkms://projects/boundarymesh-dev/locations/europe-west4/keyRings/boundarymesh-pulumi/cryptoKeys/state-secrets`
- current state: 54 managed resources / 55 records including one stack root
- accepted zero-drift preview: 54 unchanged / zero operations / exit 0

The bucket is versioned, uses uniform bucket-level access and public-access prevention, and is
protected from ordinary Pulumi deletion. The secrets key is separate from the application
attestation key. Firebase Hosting releases remain outside Pulumi ownership.

## Keyless trust boundary

Workload Identity Pool `boundarymesh-github` and provider `github-actions` are active in project
`boundarymesh-dev` (`891977254468`). The provider admits exactly two contexts. Both require private
repository ID `1385415640`, owner ID `179922674`, and `refs/heads/main`.

- `wif-verify.yml`, environment `boundarymesh-dev`, event `workflow_dispatch`
- `infra-preview.yml`, environment `boundarymesh-ci-preview`, event `workflow_dispatch`

The verification identity and preview identity use separate environment-scoped
`roles/iam.workloadIdentityUser` bindings. Both have zero user-managed keys, no direct project-level
roles, and no Token Creator role.

The preview identity has only `storage.objects.get` and `storage.objects.list` on the state bucket,
plus `roles/cloudkms.cryptoKeyDecrypter` on the dedicated state key. It cannot write/delete state,
encrypt with KMS, deploy workloads, publish artifacts, or access application data.

## Live evidence

GitHub run `36337930890` at commit `f19498bd06d484e3295d98de63cdd2211ada2999`
authenticated keylessly as `boundarymesh-ci-preview` and produced 54 unchanged / zero operations.

Bounded negative tests proved cross-service-account impersonation denial in both directions,
provider rejection for both workflow/environment swaps, and rejection when the verification job had
no environment claim. Audit review found no unexpected administrative activity.

Compute Engine API remained intentionally disabled throughout. Its Pulumi regions warning is an
accepted non-fatal consequence and is not a reason to enable the API.

## Capability boundary

Phase 16C proves shared-state access and non-mutating infrastructure preview. It does not prove or
implement artifact publication, deployment, SBOM/provenance, software signing, or general PR CI.

The required next order is:

1. Phase 16D — immutable artifact publication.
2. Phase 16E — controlled CD.
3. Phase 16F — live verification and CI/CD freeze.

Historical credential-contamination and final security-hygiene items remain unresolved. Phase 16C
must not be used to claim that the whole project is security-frozen.
