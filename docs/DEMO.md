# Demo

Use the local lab for a full walkthrough against the Java API. The hosted root is the same console, filled with synthetic data. It does not sign in and does not call the API. About the project is a page in that navigation. Use the operator console only after Google sign-in, and only for the synthetic transaction it offers.

## Local lab

Start the stack with [LOCAL_DEVELOPMENT.md](LOCAL_DEVELOPMENT.md). Open `http://127.0.0.1:5173/`.

Useful paths:

- Overview and architecture explain the boundary.
- Boundary events and the boundary graph show enforcement state. The graph is application state, not a blast-radius map.
- Webhooks demonstrate HMAC for the synthetic QuayForge partner.
- Contracts and data boundary show schema failure and field redaction.
- Quarantine, audit, and attestations show a hold, the hash chain, and signature verification.
- Connections includes synthetic systems and the optional Microsoft 365 calendar pull.

Labs mint tokens on the server. Do not paste tokens or HMAC secrets into the browser.

If Entra is not configured, the calendar pull stays on the contract harness. A configured personal account can pull one calendar. That result is local. It is not the hosted production path. Helix 365 stays a synthetic demo.

## Hosted pages

The public root does not call `/session` or `/api`. It is a simulated read only application. It is not anonymous access to operator actions. `/explore` redirects to `/`.

The operator page asks for Google sign-in before any action. After a session exists, the console can submit one synthetic crossing. That crossing is the hosted proof. It does not call CoveCRM, Lumenway, Rivertide, or Graph.

The live Hosting channel may still be the earlier operator-only bundle until a later release is published. Read the repository if the live page and the source differ.

## What not to do

Do not dispatch publish, stage, promote, or rollback as part of a demo. Do not point scanners at the live project. Do not treat a local Graph pull as an enterprise mailbox integration.
