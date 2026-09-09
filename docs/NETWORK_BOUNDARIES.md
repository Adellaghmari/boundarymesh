# Network and trust boundaries

```text
MICROSOFT 365 / PARTNER INTERNET
        │  TLS
        ▼
PUBLIC Cloud Run  boundary-api   (HTTPS, unauthenticated network, application AuthN)
        │
        ├─ Google APIs: Firestore, KMS, Pub/Sub  (IAM, private Google APIs)
        │     Secret Manager: DENIED
        │
        ▼
Pub/Sub  delivery-instructions   (IAM publish/subscribe)
        │
        ▼
PRIVATE Cloud Run  delivery-worker  (no public ingress, pull)
        │
        ├─ Secret Manager, Firestore, KMS, DLQ
        │
        ▼
Apache Camel connector  →  Graph / CoveCRM / Rivertide / Lumenway (TLS egress)
```

| Boundary | Identity | Authorization | Credentials | Exposure | Data | Failure |
| --- | --- | --- | --- | --- | --- | --- |
| Internet → API | JWT / HMAC / Graph / none for GET | engine + Spring Security | none on GET | public HTTPS | classified per payload | FC on security |
| API → Firestore | `sa-boundary-api` | IAM RW | ADC | Google backplane | operational + hashes | retry |
| API → KMS | same SA | signer | no local key | Google | digests | UNSIGNED |
| API → Pub/Sub | same SA | publish topic | — | Google | delivery instruction | outbox |
| API → Secret Manager | — | **none** | — | — | — | n/a |
| Pub/Sub → worker | `sa-delivery-worker` | subscribe | — | no public URL | instruction | redelivery / DLQ |
| Worker → SM | worker SA | secret-level accessor | ADC | Google | secret bytes in memory only | Q |
| Worker → internet dest | connector creds | dest ACL | SM | public egress | minimized payload | BR / Q |

No VPC, no GKE, no service mesh. Egress from Cloud Run is the internet. Destinations must be HTTPS.

Firebase Hosting → API: browser CORS allowlist of the Hosting origin only.

Admin session cookies: `Secure` only on HTTPS.
