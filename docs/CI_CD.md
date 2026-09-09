# GitHub Actions and GCP Workload Identity Federation

No long-lived GCP service-account JSON keys. Ever.

## Google federation model (not a copy of AWS OIDC)

GitHub is configured as an **OIDC identity provider** on a GCP **Workload Identity Pool**.

1. GitHub Actions requests an OIDC token (`aud` bound to the pool).
2. GCP Security Token Service exchanges it for a federated token.
3. The token **impersonates** a specific service account via IAM `roles/iam.workloadIdentityUser` on that SA, constrained by **attribute conditions** (`assertion.repository == 'Adellaghmari/boundarymesh'`, ref, workflow).

Unlike AWS IAM roles’ trust policy, GCP splits **pool + provider + SA impersonation**. Document this in the recruiter Architecture page.

## Identities

| SA | Allowed to | Bound to |
| --- | --- | --- |
| `sa-github-ci` | Artifact Registry write (optional), no prod mutate | PRs: test, Semgrep, Gitleaks, Dependency-Check |
| `sa-github-deploy` | Cloud Run deploy, Pulumi up (gated), Firebase | `workflow_dispatch` + environment `prod` |

CI does **not** access Secret Manager or KMS sign.

## Workflows (Phase 17)

- `ci.yml` — Maven test, Vue test, Semgrep, Gitleaks, Dependency-Check on PR
- `deploy.yml` — build images, push Artifact Registry, Cloud Run deploy, **Pulumi up only if flags set**
- `zap.yml` — baseline after public URL exists

## Proof later

- Action log shows WIF impersonation, not `GOOGLE_APPLICATION_CREDENTIALS` file
- Repo has **zero** `*-sa.json`
- Gitleaks clean
