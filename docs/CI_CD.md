# GitHub Actions and GCP Workload Identity Federation

This document separates the **live-verified Phase 16C foundation** from the publication and
deployment capabilities planned for Phases 16D through 16F. BoundaryMesh uses no long-lived GCP
service-account JSON key for CI.

## Live federation boundary

GitHub Actions requests an OIDC token. GCP Security Token Service accepts it only through Workload
Identity Pool `boundarymesh-github` and provider `github-actions`, then service-account IAM decides
whether that admitted principal may impersonate one specific account.

The provider requires these immutable claims for both admitted contexts:

- repository ID `1385415640`
- repository owner ID `179922674`
- private repository visibility
- ref `refs/heads/main`

It admits exactly these two context branches:

1. Environment `boundarymesh-dev`, workflow
   `Adellaghmari/boundarymesh/.github/workflows/wif-verify.yml@refs/heads/main`, event
   `workflow_dispatch`.
2. Environment `boundarymesh-ci-preview`, workflow
   `Adellaghmari/boundarymesh/.github/workflows/infra-preview.yml@refs/heads/main`, event
   `workflow_dispatch`.

Repository name is mapped for context but immutable numeric repository and owner IDs form the
anti-rename/anti-name-reuse boundary. No pull-request or push event is privileged by this provider.

## Live identities and authorization

### Verification identity

`boundarymesh-wif-verify@boundarymesh-dev.iam.gserviceaccount.com` is impersonable only by the
`attribute.environment/boundarymesh-dev` principal set after the provider admits the exact
verification context. It has:

- zero user-managed keys
- no direct project role
- no deployment or application-data role
- no `roles/iam.serviceAccountTokenCreator`

It proves keyless federation only.

### Infrastructure-preview identity

`boundarymesh-ci-preview@boundarymesh-dev.iam.gserviceaccount.com` is impersonable only by the
`attribute.environment/boundarymesh-ci-preview` principal set after the provider admits the exact
preview context. Its complete resource authorization is:

- custom role `projects/boundarymesh-dev/roles/boundarymeshCiStateReader` on bucket
  `bm-pulumi-state-eu4-21cad20b325f`, containing exactly `storage.objects.get` and
  `storage.objects.list`
- `roles/cloudkms.cryptoKeyDecrypter` on only
  `projects/boundarymesh-dev/locations/europe-west4/keyRings/boundarymesh-pulumi/cryptoKeys/state-secrets`

It has zero user-managed keys, no direct project role, no state write/delete, no KMS encrypt, no
project reader, no deployment permission, no application-data access, and no Token Creator role.

The two service accounts have separate environment-scoped `roles/iam.workloadIdentityUser`
bindings. Neither identity can impersonate the other.

## Live workflows

### `wif-verify.yml`

- manual `workflow_dispatch` only
- protected `main` and environment `boundarymesh-dev`
- `contents: read` and `id-token: write`
- pinned Google authentication action
- positive federation proof plus bounded negative test modes
- no checkout, Pulumi, deployment, resource mutation, or token output

### `infra-preview.yml`

- manual `workflow_dispatch` only
- protected `main` and environment `boundarymesh-ci-preview`
- `contents: read` and `id-token: write`
- commit-pinned checkout/auth/setup actions and Pulumi CLI `3.262.0`
- logs into `gs://bm-pulumi-state-eu4-21cad20b325f`
- runs only `pulumi preview --stack dev --diff --non-interactive --suppress-permalink`
- no refresh, update/apply, state write, deployment, publication, or Secret Manager/application-secret read

## Live verification evidence

Positive preview run `36337930890` used commit
`f19498bd06d484e3295d98de63cdd2211ada2999`, authenticated as the preview service account, and
returned **54 unchanged / zero operations**.

Negative tests proved:

- preview context cannot impersonate the verification service account
- verification context cannot impersonate the preview service account
- `infra-preview.yml` in `boundarymesh-dev` is rejected by provider condition
- `wif-verify.yml` in `boundarymesh-ci-preview` is rejected by provider condition
- `wif-verify.yml` without an environment is rejected before IAM Credentials

Audit review found no unexpected administrative activity. Both accounts remain keyless and without
project-level or Token Creator roles. Compute Engine API remains intentionally disabled.

## Planned capabilities

Phase 16C does **not** implement general pull-request CI, artifact publication, or deployment.

- **Phase 16D — immutable artifact publication:** exact-commit builds, digest-pinned publication,
  release manifest, and a separately reviewed publication identity/workflow. No identity name or IAM
  grant is live until that phase proves it.
- **Phase 16E — controlled CD:** explicit approval, immutable inputs, narrowly scoped deployment
  identity, post-deployment verification, and rollback.
- **Phase 16F — live verification and CI/CD freeze:** positive and negative boundary evidence,
  traceability, zero-key reconfirmation, and documentation freeze.
- Application-security automation such as Semgrep, Gitleaks, Dependency-Check, Dependabot, and ZAP
  remains planned and is not part of the live preview workflow.

No speculative publication or deployment service account is represented as existing. Final
security hygiene, including historical credential-contamination review, remains unresolved until a
later explicit gate.
