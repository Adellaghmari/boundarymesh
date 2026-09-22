# Phase 15J-C3A.1 — Hosting HTML cache-policy fix

Status: **PASS.** Firebase Hosting serves the cloud SPA at `https://boundarymesh-dev.web.app`. A first Hosting release revealed that root and SPA fallback responses inherited Firebase Hosting's default one-hour cache despite `/index.html` being non-cacheable.

## Correction

Firebase Hosting evaluates matching header rules in declaration order. The catch-all shell rule now applies `Cache-Control: no-cache` before the `/assets/**` rule overrides only hashed static assets with `public,max-age=31536000,immutable`. The BFF rewrites, SPA fallback, and security headers are unchanged. A focused infrastructure test locks that order.

## Live verification

The second Hosting release verified all of the following:

- `/`, `/transactions/demo`, and `/index.html` return the SPA shell with `Cache-Control: no-cache`.
- A hashed JavaScript asset returns `Cache-Control: public, max-age=31536000, immutable`.
- The live HTML responses include the configured content-security, content-type, referrer, and permissions policies.
- API, BFF, and Worker workloads remain Ready; the core API remains IAM-private; no Cloud Run or IAM resource changed during this Hosting-only release.
- Final Pulumi preview: **39 unchanged**, zero operations.

## Deliberate deferral

No Firebase Google sign-in was started, no authenticated BFF-to-core request was made, and no BoundaryMesh transaction was submitted. Those checks remain the next controlled C3A gate.
