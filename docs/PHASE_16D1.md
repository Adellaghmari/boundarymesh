# Phase 16D.1 — immutable artifact publication architecture

Historical gate status: **IMPLEMENTED IN SOURCE / TESTED LOCALLY / PREVIEWED**. Phase 16D later
completed live publication and independent verification; current truth is recorded in
[PHASE_16D_FINAL.md](PHASE_16D_FINAL.md).

Phase 16D separates publication from deployment. The planned publisher identity is
`boundarymesh-artifact-publish@boundarymesh-dev.iam.gserviceaccount.com` (the nearest valid form
within GCP's 30-character account-ID limit). It is keyless and may
receive `roles/artifactregistry.writer` only on the existing `boundarymesh` Docker repository and
the new `boundarymesh-releases` generic repository. It has no project role, runtime deployment,
Pulumi-state, state-KMS, application-data, signing-key, Secret Manager, Token Creator, or Service
Account User access.

The WIF provider source preserves the two live Phase 16C contexts and adds exactly one complete
publication alternative: private numeric repository and owner identity, `refs/heads/main`, manual
dispatch, environment `boundarymesh-artifact-publish`, and the exact
`.github/workflows/artifact-publish.yml@refs/heads/main` workflow reference. This source is not live
until a later reviewed apply.

The executable workflow is intentionally absent. GitHub documents that referencing a nonexistent
environment from a workflow can create it, so the reviewed contract remains at
`docs/templates/artifact-publish.yml` until the protected publication environment exists. This
keeps the ordering fail-closed.

The publication set is three exact-commit, digest-resolved OCI images; deterministic
`frontend-dist.tar.gz`; a versioned release manifest; and SBOM/provenance metadata. Production
Docker stages pin validated linux/amd64 Temurin manifests. The frontend archive normalizes order,
paths, timestamp, mode, UID/GID, and gzip metadata. The release manifest is canonical JSON and uses
an external SHA-256 sidecar, avoiding a circular self-hash. Phase 16E must consume these immutable
identities and produce separate deployment evidence; it must not rewrite the publication manifest.

No artifact has been published and no runtime has been deployed by this gate. The publication
environment, publisher identity, generic repository, IAM grants, immutable-tag setting, and third
WIF context are not live until separately reviewed gates apply and verify them.

The final read-only preview reports **5 creates / 2 updates / 52 unchanged / 0 replacements /
0 deletes**. Creates are the publisher account, its environment-scoped WIF binding, the generic
repository, and two repository-scoped writer members. Updates are only immutable tags on the
existing Docker repository and the exact third provider context.
