# Network and trust boundaries

```text
HOSTED OPERATOR BROWSER
        │  TLS + Firebase Google authentication
        ▼
Firebase Hosting  boundarymesh-dev.web.app
        │  same-origin rewrite + Secure/HttpOnly __session
        ▼
Cloud Run Service  boundarymesh-web   (public transport, mandatory application auth)
        │  Google-signed ID token + service-level roles/run.invoker
        ▼
Cloud Run Service  boundarymesh-api   (IAM-private)
        │
        ├─ Google APIs: Firestore, KMS, Pub/Sub  (IAM, private Google APIs)
        │     Secret Manager: hmac-quayforge only
        │
        ▼
Pub/Sub  boundarymesh-deliveries   (IAM publish/subscribe)
        │
        ▼
Cloud Run Worker Pool  boundarymesh-worker  (no endpoint, pull)
        │
        ├─ named connector secrets + Firestore
        │
        ▼
Apache Camel connector  →  Graph / CoveCRM / Rivertide / Lumenway (TLS egress, **hosted destinations unconfigured**)
```

| Boundary | Identity | Authorization | Credentials | Exposure | Data | Failure |
| --- | --- | --- | --- | --- | --- | --- |
| Browser → Hosting/BFF | Firebase Google identity + `__session` | verified operator UID, CSRF, strict route allowlist | Secure HttpOnly host-only cookie | public transport edge | bounded application request | fail closed before core |
| BFF → API | `boundarymesh-web` workload identity | service-level `roles/run.invoker` | short-lived Google-signed ID token | IAM-private core | allowlisted request only | bounded timeout / `CORE_UNAVAILABLE` |
| API → Firestore | `boundarymesh-api` | `roles/datastore.user` | ADC | Google backplane | operational + hashes | retry |
| API → KMS | same SA | signer | no local key | Google | digests | UNSIGNED |
| API → Pub/Sub | same SA | publish topic | — | Google | delivery instruction | outbox |
| API → Secret Manager | API SA | accessor on `hmac-quayforge` only | ADC | Google | HMAC bytes in memory only | reject |
| Pub/Sub → worker | `boundarymesh-worker` | subscriber on one subscription | — | no public URL | instruction | redelivery |
| Worker → SM | worker SA | accessor on three named connector secrets | ADC | Google | secret bytes in memory only | Q |
| Worker → internet dest | connector creds | dest ACL | SM | public egress | minimized payload | BR / Q |

No VPC, no GKE, no service mesh. Egress from Cloud Run is the internet. Destinations must be HTTPS.

Firebase Hosting rewrites only the reviewed session and application routes to `boundarymesh-web`; the browser never calls the private core URL. The BFF uses a 30-second bounded per-request timeout and a separate five-second connection timeout with no retries.

The Firebase session cookie is `__session`, `Secure`, `HttpOnly`, `SameSite=Lax`, Path `/`, host-only, and limited to one hour.

Phase 15J live-verified this complete route with exactly one synthetic hosted browser transaction.
The BFF reached the IAM-private core keylessly; the core published one delivery instruction; the Worker
persisted `QUARANTINED` before ACK. With no Rivertide endpoint configured, no external HTTP call or
connector-secret retrieval occurred. See [PHASE_15J_FINAL.md](PHASE_15J_FINAL.md).
