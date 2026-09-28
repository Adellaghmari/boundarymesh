# GitHub Actions and GCP Workload Identity Federation

This document separates the **live-verified Phase 16C preview foundation**, the **live-verified Phase
16D publication boundary**, and deployment capabilities reserved for Phase 16E. BoundaryMesh uses no
long-lived GCP service-account JSON key for CI or publication.

## Live federation boundary

GitHub Actions requests an OIDC token. GCP Security Token Service accepts it only through Workload
Identity Pool `boundarymesh-github` and provider `github-actions`, then service-account IAM decides
whether that admitted principal may impersonate one specific account.

The provider requires these immutable claims for all admitted contexts:

- repository ID `1385415640`
- repository owner ID `179922674`
- private repository visibility
- ref `refs/heads/main`

The live provider admits exactly these three context branches:

1. Environment `boundarymesh-dev`, workflow
   `Adellaghmari/boundarymesh/.github/workflows/wif-verify.yml@refs/heads/main`, event
   `workflow_dispatch`.
2. Environment `boundarymesh-ci-preview`, workflow
   `Adellaghmari/boundarymesh/.github/workflows/infra-preview.yml@refs/heads/main`, event
   `workflow_dispatch`.
3. Environment `boundarymesh-artifact-publish`, workflow
   `Adellaghmari/boundarymesh/.github/workflows/artifact-publish.yml@refs/heads/main`, event
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

### Artifact-publication identity

`boundarymesh-artifact-publish@boundarymesh-dev.iam.gserviceaccount.com` is impersonable only by
the `attribute.environment/boundarymesh-artifact-publish` principal set after the provider admits
the exact publication context. It has zero user-managed keys, zero direct project roles, and only:

- `roles/artifactregistry.writer` on Docker repository `boundarymesh`; and
- `roles/artifactregistry.writer` on Generic repository `boundarymesh-releases`.

It has no deployment, runtime, application-data, Pulumi-state, state-KMS, Token Creator, or Service
Account User role.

The three service accounts have separate environment-scoped `roles/iam.workloadIdentityUser`
bindings. Their workflows and environments cannot be interchanged under the provider condition.

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

### `artifact-publish.yml`

- manual `workflow_dispatch` only from protected `main`
- environment `boundarymesh-artifact-publish`, restricted to `main`, with zero secrets and variables
- `contents: read` and `id-token: write`
- all six actions pinned to full commit SHAs
- exact-commit OCI publication with BuildKit SBOM/provenance
- deterministic frontend archive, CycloneDX SBOM, and unsigned in-toto/SLSA provenance
- canonical release manifest and external SHA-256 sidecars
- collision checks before the first remote write
- no Pulumi operation, deployment, IAM/WIF mutation, Firebase deployment, or application-data access

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

Audit review found no unexpected administrative activity. The verification and preview accounts
remain keyless and without project-level or Token Creator roles. Compute Engine API remains
intentionally disabled.

## Immutable publication evidence

Publication run `36492495801` succeeded from source SHA
`a876a7bfa5fa86f88e8387efb557773281daa6e6`. Independent read-back verified all three OCI digests,
attached OCI evidence, the four-file frontend Generic package, the two-file release package, both
published sidecars, and the actual downloaded manifest against its schema. The manifest SHA-256 is
`ecdbaf88710ff73fd694c5c4e13c0a8ce300fdacf2a3761064b7511bd627300d`.

Exactly four SBOM and provenance subjects exist: API, Worker, Web Edge, and frontend. Frontend
provenance is unsigned; BoundaryMesh makes no software-signing claim. Docker tags are immutable and
Generic versions use the exact source SHA. Audit evidence verified two access-token events for the
publisher identity; Artifact Registry Data Access events were not observable in the queried stream.
See [PHASE_16D_FINAL.md](PHASE_16D_FINAL.md).

Historical run `36490169553` failed closed on a frontend checksum-sidecar working-directory defect
before any remote write. It left no artifact residue and was repaired through protected PR #15.

## Remaining capabilities

Phase 16C did **not** implement general pull-request CI or deployment. Phase 16D implemented
publication, but publication remains separate from deployment.

- **Phase 16E — controlled CD:** explicit approval, immutable manifest/digest inputs, narrowly scoped
  deployment identity, post-deployment verification, and rollback. It must consume the frozen Phase
  16D release rather than rebuilding or republishing it.
- **Phase 16F — live verification and CI/CD freeze:** positive and negative deployment-boundary
  evidence, traceability, zero-key reconfirmation, and documentation freeze.
- Application-security automation such as Semgrep, Gitleaks, Dependency-Check, Dependabot, and ZAP
  remains planned and is not part of the live preview workflow.

No speculative deployment service account is represented as existing. Final security hygiene,
including historical credential-contamination review, remains unresolved until a later explicit gate.
