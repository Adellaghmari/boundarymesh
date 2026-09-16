# Network and trust boundaries

```text
MICROSOFT 365 / PARTNER INTERNET
        │  TLS
        ▼
Cloud Run Service  boundarymesh-api   (HTTPS, IAM-authenticated in 15I-A design)
        │
        ├─ Google APIs: Firestore, KMS, Pub/Sub  (IAM, private Google APIs)
        │     Secret Manager: hmac-quayforge only
        │
        ▼
Pub/Sub  delivery-instructions   (IAM publish/subscribe)
        │
        ▼
Cloud Run Worker Pool  boundarymesh-worker  (no endpoint, pull)
        │
        ├─ named connector secrets + Firestore
        │
        ▼
Apache Camel connector  →  Graph / CoveCRM / Rivertide / Lumenway (TLS egress)
```

| Boundary | Identity | Authorization | Credentials | Exposure | Data | Failure |
| --- | --- | --- | --- | --- | --- | --- |
| Internet → API | JWT / HMAC / Graph / none for GET | engine + Spring Security | none on GET | public HTTPS | classified per payload | FC on security |
| API → Firestore | `boundarymesh-api` | `roles/datastore.user` | ADC | Google backplane | operational + hashes | retry |
| API → KMS | same SA | signer | no local key | Google | digests | UNSIGNED |
| API → Pub/Sub | same SA | publish topic | — | Google | delivery instruction | outbox |
| API → Secret Manager | API SA | accessor on `hmac-quayforge` only | ADC | Google | HMAC bytes in memory only | reject |
| Pub/Sub → worker | `boundarymesh-worker` | subscriber on one subscription | — | no public URL | instruction | redelivery |
| Worker → SM | worker SA | accessor on three named connector secrets | ADC | Google | secret bytes in memory only | Q |
| Worker → internet dest | connector creds | dest ACL | SM | public egress | minimized payload | BR / Q |

No VPC, no GKE, no service mesh. Egress from Cloud Run is the internet. Destinations must be HTTPS.

Firebase Hosting → API: browser CORS allowlist of the Hosting origin only.

Admin session cookies: `Secure` only on HTTPS.
