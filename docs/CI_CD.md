# GitHub Actions and GCP Workload Identity Federation

BoundaryMesh uses no long-lived GCP service-account JSON key for CI, publication, or deployment.
GitHub repository and environment secrets for GCP credentials are unused. Operator workstation
ADC is a separate human path and is not “keyless CI/CD.”

Application tests (Maven, Vitest, Playwright) run locally. There is no pull-request application CI
workflow. That is a qualifier, not a claim of fully automated CI/CD.

## Live federation boundary

GitHub Actions requests an OIDC token. GCP STS accepts it only through pool `boundarymesh-github`
and provider `github-actions`. Service-account IAM then decides whether that principal may
impersonate **one** account.

Immutable claims for every admitted context:

- repository ID `1385415640`
- repository owner ID `179922674`
- private repository visibility
- ref `refs/heads/main`

The live provider admits **exactly seven** contexts (all `workflow_dispatch`):

| # | Environment | Workflow |
| --- | --- | --- |
| 1 | `boundarymesh-dev` | `wif-verify.yml` |
| 2 | `boundarymesh-ci-preview` | `infra-preview.yml` |
| 3 | `boundarymesh-artifact-publish` | `artifact-publish.yml` |
| 4 | `boundarymesh-deploy` | `deploy-release.yml` |
| 5 | `boundarymesh-hosting-deploy` | `deploy-release.yml` |
| 6 | `boundarymesh-deploy` | `recover-promotion-evidence.yml` |
| 7 | `boundarymesh-hosting-deploy` | `recover-promotion-evidence.yml` |

No pull-request or push event is privileged by this provider.

## Identities

All listed accounts have **zero user-managed keys** and no `roles/iam.serviceAccountTokenCreator`
on the CI/CD or workload identities. A Firebase-managed identity in the project holds Token
Creator; that is outside the keyless CI/CD claim. See [SECURITY.md](SECURITY.md).

| Account | May do | May not do |
| --- | --- | --- |
| `boundarymesh-wif-verify` | Prove federation | Anything else |
| `boundarymesh-ci-preview` | Read Pulumi state objects; decrypt `state-secrets` | Write state, apply, deploy, publish |
| `boundarymesh-artifact-publish` | Write Docker `boundarymesh` and Generic `boundarymesh-releases` | Deploy, IAM, Pulumi, Hosting |
| `boundarymesh-deploy` | Update existing Cloud Run API/Web/Worker; write deployment evidence | Hosting mutate, IAM/WIF, rebuild images |
| `boundarymesh-hosting-deploy` | Bounded Hosting preview/promote/restore; read Web service | Cloud Run mutate, IAM/WIF |

Workload identities `boundarymesh-api`, `boundarymesh-worker`, and `boundarymesh-web` are also
keyless. The deploy account has `serviceAccountUser` on those three so Cloud Run revisions can
run as them. That is not Token Creator.

## Happy path

Human dispatch only. No automatic promote, no automatic rollback, no application rebuild.

```text
publish (once per source SHA)
    → stage  (zero-allocation Cloud Run candidates + Hosting preview)
    → promote (shift API/Web traffic and Worker instanceSplits to 100%; Hosting live version)
    → [optional later] rollback
```

Publication, stage, and promote are separate protected workflows or operations. Promotion consumes the frozen release manifest and parent OCI digests. Cloud Run may show a
resolved linux/amd64 child digest at runtime. Identity continuity compares parent and resolved
images without treating that as drift.

### `wif-verify.yml`

Manual federation proof and bounded negative modes. No Pulumi, no deploy.

### `infra-preview.yml`

Manual `pulumi preview --stack dev --diff --non-interactive --suppress-permalink`. No refresh,
no apply. Saved-plan apply is **not** a live CI capability; infrastructure apply remains a
reviewed, non-default operation outside these workflows.

### `artifact-publish.yml`

One immutable four-subject release per source SHA. Collision-fail. Unsigned SBOM/provenance.
Does not deploy.

### `deploy-release.yml`

One dispatch selects `stage`, `promote`, or `rollback`. Runtime jobs use `boundarymesh-deploy`.
Hosting jobs use `boundarymesh-hosting-deploy`. Evidence is written only after a complete
operation. Stage evidence and promotion evidence are separate immutable Generic versions.

Rollback is implemented and tested. Rollback identities remain live-readable. **Rollback has
not been live-executed.** A human must dispatch it with the exact stage and promotion run IDs.

## Break glass recovery (not the normal path)

`recover-promotion-evidence.yml` exists because an otherwise successful promotion can fail while
recording immutable evidence. It does **not** promote, roll back, or change runtime/Hosting.

- Preconditions: original stage succeeded; original promotion succeeded; production still matches
  that promotion; the promotion-evidence version is absent.
- Dual principal: Hosting read under `boundarymesh-hosting-deploy`, then evidence publish under
  `boundarymesh-deploy`.
- The recovered document binds the **original promotion run**, not the recovery run.
- Publish is once; collisions fail closed.
- It remains available because Hosting inventory is not readable under the runtime deploy
  identity, and a missing evidence version would otherwise be unrecoverable without mutation.

Do not describe recovery as part of every promotion.

## Current deployed release

The deploy lock points at published source `f307c9009f39f60f3fe4c036846ba1c0c3c42a7f`, publication run `37238013523`. Its Hosting config is vendored at `scripts/deploy/release/frozen/firebase.f307c9009f39f60f3fe4c036846ba1c0c3c42a7f.json`. Earlier Hosting configs remain vendored for `a876a7bfa5fa86f88e8387efb557773281daa6e6`, `22019c72ab7f4435c9dc588bb331a5c97d45b1c4`, and `2ad8ae1e7b49351c4ad1abea0acc595922911ac3`. Deployment reads the lock's vendored Hosting bytes and checks the recorded SHA256. It does not read a historical commit from Git.

| Surface | Identity |
| --- | --- |
| API | `boundarymesh-api-c-36941014905` at 100% |
| Web edge | `boundarymesh-web-c-36941014905` at 100% |
| Worker | `boundarymesh-worker-c-36941014905` at 100% |
| Firebase Hosting live | version `c9def09b61688b16` |
| Rollback API | `00004-9hw` |
| Rollback web | `00004-k48` |
| Rollback worker | `00002-v6j` |
| Rollback Hosting | `1bce16129fb1a0d4` |

Rollback identities are readable. They have not been dispatched. Promotion evidence is an immutable generic package version bound to the original promotion run.

Parent image digests are listed in [INFRASTRUCTURE.md](INFRASTRUCTURE.md). The deploy lock is `scripts/deploy/frozen-release.json`. Do not edit that lock to match a newer commit. A newer release requires a new publication.

## What CI/CD is not

- Fully automated CI/CD
- Pull-request Maven/Vitest gates
- Signed attestations / Cosign
- Automatic rollback on failure
