# Cost model

**Phase 0 provisions zero GCP resources.** These are estimates for a low-usage portfolio after Phase 16–18.

Region `europe-west4`. Billing: pay-per-use, scale-to-zero. No GKE, no VMs, no Cloud SQL, no NAT, no global HTTPS load balancer in the default design (Firebase Hosting + Cloud Run URLs).

Currency: USD, 2026 public list / always-free where noted. Recheck at provision time.

## Always-free / low idle (typical)

| Service | Free-ish / idle | Low-usage extra | Trap |
| --- | --- | --- | --- |
| Cloud Run (request-based) | 2M req, 180k vCPU-s, 360k GiB-s / month (N. America quoted; **EU may differ — verify**) | Recruiter traffic usually inside free | Min instances > 0 → idle CPU bill. **Lock min instances = 0** |
| Firestore | 1 GiB, 50k reads / 20k writes / 20k deletes **per day** | Demo well inside | List pages without indexes; chatty UI polling |
| Pub/Sub | 10 GiB / month | Tiny messages | Retries storm |
| Secret Manager | 6 active versions, 10k accesses | Stay within named secrets | Many versions left ACTIVE |
| Cloud KMS | Autokey free tier **does not apply** to manually created keys. Software asymmetric key ≈ **$0.06 / month / version** + $0.03 / 10k operations | One key version: ~$0.06 idle | HSM keys; unused versions |
| Artifact Registry | storage + egress | Small images | Untagged image pile |
| Firebase Hosting | generous hobby | fine | |
| Cloud Logging | 50 GiB/month free | **TRAP** if debug logs on public ingest | Debug on, huge JSON bodies |
| Cloud Monitoring | free metrics allotment | custom metrics explosion | |
| Cloud Trace | sampling | 100% sample in prod | **Lock 1–5% prod, 100% local** |
| GCS Pulumi state | pennies | versioning on | |
| Cloud Armor | **not default** | skip | |

## Monthly envelope (honest)

| Scenario | Estimate |
| --- | --- |
| Idle deployed (min 0, tiny data, 1 KMS key version, few secrets) | **~$0.10–2.00** (KMS + storage + leftover logs) |
| Recruiter demo traffic (hundreds of GETs/day) | still ~free-tier |
| Logging left at DEBUG with payload fields | **$10–50+** easily — primary trap |
| API min-instance > 0 or deployed Worker Pool instance | **tens of dollars** — API stays at 0; worker cost requires deployment approval |
| Load balancer + unused GKE | out of policy |

## Cost traps before implementation

1. **Logging** — never log request bodies; INFO in prod; retention 30 days.
2. **Min instances** — 0.
3. **Firestore polling** — Vue fetches on navigation, not 1s intervals.
4. **KMS HSM** — software protection level.
5. **PITR / backups** — optional; PITR doubles storage-like cost. Default **off** until needed.
6. **Multi-region Firestore** — use regional.
7. **Secret versions** — disable old versions.
8. **Artifact Registry** — cleanup untagged.
9. **Trace 100%** — no.
10. **NAT / VPC connector** — not used (no VPC Cloud Run). Direct egress.

## Budget (Phase 16)

Set a billing budget alert at **$10** and **$25**. No resources until then.

## Comparison intent

Cheaper than always-on VMs and GKE. Comparable spirit to Project B’s serverless AWS bill, different vendor.
