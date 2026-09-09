# Microsoft 365 / Graph / Entra integration

Microsoft 365 is the **flagship enterprise connector ecosystem**, not the hosting cloud. BoundaryMesh remains GCP-hosted.

```text
MICROSOFT 365 / ENTRA / GRAPH
        →  JAVA / SPRING / CAMEL
        →  BOUNDARY ENFORCEMENT
        →  GOOGLE CLOUD
        →  COVECRM (and other connectors)
```

## Chosen depth: Outlook Calendar

One deep connector beats five shallow Graph wrappers.

**Scenario:** a Helix 365 calendar event (customer meeting created or updated) attempts to become a **CoveCRM activity**.

Why calendar:

- Real change-notification lifecycle (create, validate, expire, renew, lifecycle notifications).
- Attendee emails are a natural **egress** demo (REDACT).
- Application permission can be least-privilege (`Calendars.Read`).
- CRM destination is a believable system crossing.

Not in v1: SharePoint libraries, Teams chats, mail send, `User.Read.All`.

## Permission matrix (least privilege)

| Permission | Type | Why |
| --- | --- | --- |
| `Calendars.Read` | Application | Read events for subscribed mailboxes / users in tenant |
| Admin consent | required | Application permissions always |

Not requested: `Calendars.ReadWrite` (connector does not write back to Graph in v1).

If live tenant cannot grant application calendar read, **do not fake it**. Stay on **CONTRACT-HARNESS MODE** and mark the CV matrix `TESTED`, not `LIVE VERIFIED`.

## Two modes (hard)

### LIVE CONNECTOR MODE

Used only when a legitimate tenant exists and a real Graph call has been executed.

Evidence required to flip the badge:

- tenant ID (non-secret)
- application ID (non-secret)
- permission granted
- one successful Graph GET or notification processed
- timestamp and Git SHA

### CONTRACT-HARNESS MODE (default until proven otherwise)

- Official Graph API shapes (OpenAPI / documented JSON)
- WireMock mappings for:
  - `GET /v1.0/users/{id}/events/{id}`
  - subscription CRUD
  - validation handshake (echo `validationToken`)
  - notification payload
  - 401 / 403 / 429 / 5xx
- Pact consumer tests for `GraphCalendarClient`
- Synthetic Helix 365 mailbox data labeled **SYNTHETIC DEMO**

The core product **must build and demo fully** in harness mode.

## Graph change notifications (real behavior)

Microsoft Graph notifications are **not** HMAC-SHA256 partner webhooks.

| Step | Graph behavior | BoundaryMesh |
| --- | --- | --- |
| Create subscription | POST `/subscriptions` with `notificationUrl`, `resource`, `expirationDateTime`, `clientState` | Worker (authenticated) creates; API does not |
| Validation | Graph sends GET/POST with `validationToken`; endpoint must return token in **plain text** within seconds | `boundary-api` public handler, no engine, no persistence of token |
| Notifications | POST resource notifications; may include `clientState` | Authenticity: `clientState` compared constant-time to Secret Manager reference; tenant/app/subscription ID must match Firestore registration |
| Lifecycle | `subscriptionRemoved`, `missed`, reauthorization | Quarantine + renew path; no fake infinite subscriptions |
| Expiration | max lifetime is limited | Renewal job on worker before expiry |
| Duplicates | Graph may deliver more than once | Idempotency on `notificationId` / resource + change type; **not** classified as replay |

`clientState` is a secret. Store only a Secret Manager reference. Compare in constant time. Never log it.

If Microsoft introduces additional handshake/signature validation for the notification payload in the tenant’s cloud, implement the **documented Graph mechanism**, not a pretend HMAC.

## Connector through the engine

Graph events do **not** bypass BoundaryMesh.

1. Notification hits `boundary-api`.
2. Subscription authenticity + freshness + duplicate/replay rules.
3. Optional fetch of full event (worker) **or** use resource data in notification if rich notifications are enabled — Phase 14 chooses one and documents it. Default: notification is a **pointer**; worker fetches the event **after** ALLOW, so untrusted POST bodies are not treated as the CRM payload.
4. Fetched event is validated against `helix-calendar-event.v1` JSON Schema.
5. Egress policy for CoveCRM applied.
6. Camel `graphToCrm` route delivers.

If we fetch after ALLOW, the **ingress** transaction is the notification envelope; the **delivery** payload is the Graph resource. Both get evidence records. The attestation covers the crossing that was actually delivered.

## Error handling (Graph)

| Graph status | Retry | Outcome |
| --- | --- | --- |
| 401 / 403 | **no** | quarantine `CREDENTIAL` / `AUTHZ` — fail closed |
| 429 | yes, honor `Retry-After` | bounded retry then quarantine |
| 5xx | yes, bounded | then quarantine |
| Timeout | yes, bounded | then quarantine |
| 404 resource | no | reject or quarantine per policy (default quarantine — resource may have been deleted) |

## SDK

Microsoft Graph Java SDK 6.66.x with `azure-identity` **client credentials** in the **worker only**.

API process has no Graph SDK usage except the validation HTTP endpoint.

## Recruiter UI: Microsoft 365 Connector page

- Mode badge: `LIVE VERIFIED` or `CONTRACT HARNESS`
- Permission list actually requested
- Delegated vs application explanation
- Last subscription expiry / renewal (synthetic or live)
- Last crossing outcome
- Explicit statement if no real tenant call has occurred
