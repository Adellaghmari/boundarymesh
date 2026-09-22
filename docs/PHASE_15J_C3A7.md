# Phase 15J-C3A.7 — bounded private-core request timeout

Phase 15J-C3A.6 live-verified the hosted Firebase session, operator authorization,
CSRF enforcement, and keyless BFF invocation of the IAM-private core. The retained
attestation read reached `boundarymesh-api` and returned HTTP 200 after approximately
16 seconds, but the BFF returned `CORE_UNAVAILABLE` because its per-request timeout was
12 seconds.

C3A.7 changes only that bounded request timeout to 30 seconds. The separate five-second
connection timeout, no-redirect policy, response-size bound, keyless ID-token flow,
route allowlist, and fail-closed `CORE_UNAVAILABLE` behavior remain unchanged. No retry,
cache, minimum instance, or core-performance change was added.

Focused tests cover the 30-second production bound, a delayed response that completes
within a shorter injected test bound, a genuine timeout, a non-timeout I/O failure,
unchanged 200/4xx/5xx forwarding, bounded response handling, and the existing Google
ID-token path. The full Maven and infrastructure verification gates pass.

The immutable BFF image tag
`phase15j-c3a7-20260922t233356z` resolves to
`sha256:90570617750bd39613482fd75d8434f1a47299ae8d35908ae0c81717374ee30a`.
Pulumi is pinned to that digest. The image is built and pushed but **not deployed**.

The current live BFF remains `boundarymesh-web-00003-xlj` on the C3A.5 digest until a
separately approved apply. The hosted end-to-end evidence read has not been reverified
after this fix. No browser transaction has been run, and Phase 15J remains in progress.
