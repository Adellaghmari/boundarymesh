# Identity and access

Four identities exist. They are not interchangeable.

## Hosted operator

The operator console uses Firebase Google sign-in. The browser receives a Firebase identity token, posts it to the web edge, and the web edge sets an HttpOnly session cookie. Firebase is then signed out in the browser. Later calls use the session, not a stored Firebase token.

The web edge checks the operator allowlist and CSRF before it calls the private API. This path is live on the promoted Hosting release.

The public portfolio page does not use this session. It is static.

## Private API

Cloud Run invocation on `boundarymesh-api` requires a Google identity token. The web edge service account is the invoker. Callers on the internet cannot invoke the API directly.

Workload service accounts are keyless. They are not user accounts and they are not CI accounts.

## Local Entra

A developer can configure Microsoft Entra for the local process. The flow is an authorization-code login through the Spring BFF, with an HttpOnly session and CSRF on `/api/v1/admin/**`.

That setup is optional. Blank local configuration keeps the lab on synthetic identity. Production Entra and production OIDC are disabled. Do not describe the hosted Google session as Entra.

Delegated `Calendars.Read` was verified locally against a personal Microsoft account. Application `Calendars.Read` is not claimed. The mailbox is not an enterprise Exchange Online tenant.

## Graph integration

One local pull reads Outlook calendar events through `MicrosoftGraphCalendarAdapter`. The verified event passed `m365-calendar/v1`, then `m365-calendar-egress/v1`, and the engine returned `ALLOW_WITH_TRANSFORMATION` because egress required redaction. Delivery to an external system was not attempted.

Graph change notifications and subscription webhooks are not implemented.

## Local API callers

Labs and tests use a JWT and JWKS resource server. HMAC callers prove authenticity of a webhook body. Entra client-credentials tokens for machine callers are not a hosted feature.

## What is disabled in production

- Production Entra and OIDC
- Graph change notifications
- External connector calls from the worker
- Anonymous operator actions
