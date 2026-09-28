# Phase 16D final evidence

Status: **CLOSED / PASS / LIVE VERIFIED / SOURCE-FROZEN** after the Phase 16D.6 documentation freeze.

## Scope and chronology

Phase 16D separated immutable artifact publication from deployment. The work proceeded through:

1. publication architecture, manifest schema, deterministic frontend packaging, and dormant workflow design;
2. a protected `boundarymesh-artifact-publish` environment and dedicated keyless publisher foundation;
3. protected activation of `.github/workflows/artifact-publish.yml`;
4. four-subject publication integration and checksum-sidecar repairs through protected PRs;
5. one successful manual publication from protected `main`; and
6. independent read-back verification of every published artifact, checksum, manifest field, and evidence reference.

The publication foundation is **LIVE VERIFIED / ZERO DRIFT**. The workflow is **LIVE / EXECUTED
SUCCESSFULLY**. The release is **PUBLISHED / IMMUTABLE / LIVE VERIFIED**. No deployment occurred.

## Frozen release identity

The release source identity is permanent and is distinct from any later documentation commit:

- source SHA: `a876a7bfa5fa86f88e8387efb557773281daa6e6`
- source ref: `refs/heads/main`
- workflow: `Publish immutable BoundaryMesh artifacts`
- workflow ID: `368645556`
- run ID: `36492495801`
- event: `workflow_dispatch`
- environment: `boundarymesh-artifact-publish`
- result: `success`

### OCI artifacts

- API: `europe-west4-docker.pkg.dev/boundarymesh-dev/boundarymesh/boundarymesh-api@sha256:bcfd9b01a3189228f583fa3f5d1ed8797f73d214af85bea4d84caeedfa0db8d7`
- Worker: `europe-west4-docker.pkg.dev/boundarymesh-dev/boundarymesh/boundarymesh-worker@sha256:7b1841487a2d1b3751e8bc63ee5323965e76ebeae9ae581c95b672f50154bb2d`
- Web Edge: `europe-west4-docker.pkg.dev/boundarymesh-dev/boundarymesh/boundarymesh-web-edge@sha256:85f98f90450b7807f55649f58807f952010a6278793b808ae1f4df78a22c0b04`

The `boundarymesh` Docker repository has `immutableTags=true`. Each exact source-SHA tag resolved to
one digest during live read-back verification. No destructive overwrite test was performed.

### Frontend package

Coordinate: `boundarymesh-releases / boundarymesh-frontend / a876a7bfa5fa86f88e8387efb557773281daa6e6`

Published files:

- `frontend-dist.tar.gz`
- `frontend-dist.tar.gz.sha256`
- `frontend-sbom.cdx.json`
- `frontend-dist.tar.gz.intoto.json`

Hashes:

- frontend archive: `80d4cfa4e88cac297c7a130ac5c83d68b468b06b60e04537c5060cb275943d84`
- frontend SBOM: `8f63892fb2c0f8d1623b9889ff70220c2324e58822831acd54c0ce008f06d175`
- frontend provenance: `ea38564bcf23e8be9f6b8149b575576504ef99d9c54de5c4b5a090db0d37b277`

The downloaded archive matched its published SHA-256 sidecar. The SBOM is normalized CycloneDX 1.5.
The provenance is an unsigned in-toto Statement v1 with a SLSA provenance v1 predicate whose subject
hash matches the archive. This release makes no software-signing claim.

### Release manifest

Coordinate: `boundarymesh-releases / boundarymesh-release / a876a7bfa5fa86f88e8387efb557773281daa6e6`

Published files:

- `release-manifest.json`
- `release-manifest.json.sha256`

Evidence:

- manifest SHA-256: `ecdbaf88710ff73fd694c5c4e13c0a8ce300fdacf2a3761064b7511bd627300d`
- contract tree: `877cee5fdd507be66d09227c625d75402fbf84cc`
- schema: `contracts/release-manifest/v1.schema.json`
- downloaded manifest sidecar verification: **PASS**
- downloaded manifest schema validation: **PASS**

## Four-subject evidence model

The manifest contains exactly four SBOM subjects and exactly four provenance subjects: `api`,
`worker`, `web`, and `frontend`.

- API SBOM: `293cbd1327ddb318b53ff244f422e0b971af61e6be39d283a1a790dd2375d37f`
- API provenance: `13ee67693f079803bf17c8ab0f4406f17094046cf9d80ed0e2ee258bf1df69c3`
- Worker SBOM: `4153092dcc4cb82577ceebbfdb66bdd2c535f4f9de48161d7956ad24e9d49d33`
- Worker provenance: `a80fca7185dbd0bb7eb0679a3a352772576a7961b3aed9300b6d75790bed0f8f`
- Web Edge SBOM: `19a9cbfb8fee4827ac8828179ac07b108f02a7f05321dad2db15d1e3ccb9f55d`
- Web Edge provenance: `75a1f3d585e7303c759ed1a8e81339eea9788c4eb6a45778a5dbfd030de48db4`
- Frontend SBOM: `8f63892fb2c0f8d1623b9889ff70220c2324e58822831acd54c0ce008f06d175`
- Frontend provenance: `ea38564bcf23e8be9f6b8149b575576504ef99d9c54de5c4b5a090db0d37b277`

API, Worker, and Web Edge use attached BuildKit OCI evidence. Frontend evidence is stored with its
Generic package and is not represented as a fake OCI attestation. No software-signing key exists for
this release.

## Publication security boundary

The publisher is `boundarymesh-artifact-publish@boundarymesh-dev.iam.gserviceaccount.com`. It has:

- zero user-managed keys;
- zero direct project roles;
- one environment-scoped `roles/iam.workloadIdentityUser` grant;
- `roles/artifactregistry.writer` on only the `boundarymesh` Docker repository; and
- `roles/artifactregistry.writer` on only the `boundarymesh-releases` Generic repository.

It has no Token Creator, Service Account User, project-wide writer, deployment, runtime,
application-data, Pulumi-state, or state-KMS role. The active WIF provider admits exactly three
private-repository, protected-main, exact-workflow, exact-environment, manual-dispatch contexts. The
publication context is bound to `.github/workflows/artifact-publish.yml` and environment
`boundarymesh-artifact-publish`. That environment allows `main` only and contains zero secrets and
zero variables.

Read-only audit review observed two `GenerateAccessToken` events for the publisher service account.
Artifact Registry Data Access events were not observable in the queried audit stream, so their
absence is not claimed. Published bytes, repository metadata, timestamps, and workflow evidence
provide the artifact write/read-back proof.

## Historical fail-closed attempt

Run `36490169553` at source SHA `04f3a1ff4c44709465acf6bdbc70d085db3e830f` failed on the frontend
checksum-sidecar working-directory defect before the first remote write. It left no artifact residue
and required no cleanup. Protected PR #15 repaired the integration. This was a fail-closed publication
defect, not a security incident.

## Verification freeze

- focused publication integration: 10 passing before the final merge sequence
- infrastructure suite: 34 passing after the sidecar repair
- downloaded frontend archive and manifest sidecars: PASS
- downloaded release manifest schema validation: PASS
- post-publication Pulumi preview: 59 unchanged / zero operations / exit 0
- Compute Engine API: intentionally disabled

## Deployment boundary and Phase 16E handoff

Artifact publication is not deployment. Phase 16D changed no Cloud Run service, Firebase Hosting
release, traffic allocation, IAM/WIF boundary during publication, or application data. Existing
runtime services remained on their prior Phase 15J revisions.

Phase 16E must consume the frozen release manifest with SHA-256
`ecdbaf88710ff73fd694c5c4e13c0a8ce300fdacf2a3761064b7511bd627300d` and deploy or promote the exact
artifact identities contained in it. Phase 16E must not rebuild API, Worker, Web Edge, or frontend;
generate replacement release artifacts; retag release images; or silently publish another version.
Any repository SHA created by this documentation freeze records documentation only and does not
replace the immutable release source SHA.

Phase 16E is **NOT STARTED**. Phase 16F is **NOT STARTED**. CI/CD is not complete.
