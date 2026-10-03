import type { CapabilityClaim } from '@/domain/types'

/**
 * Manual frontend summary of reviewed project evidence.
 * Static capability copy for the lab. Not a live cloud feed.
 */
export const CAPABILITY_CLAIMS: readonly CapabilityClaim[] = [
  {
    id: 'java-backend',
    label: 'Java backend',
    status: 'LIVE VERIFIED',
    notes: 'Java 25 and Spring Boot 4.1.1 on a private Cloud Run API. Production transaction ingress is verified.',
  },
  {
    id: 'enforcement-engine',
    label: 'Boundary Enforcement Engine',
    status: 'LIVE VERIFIED',
    notes: 'The deployed API evaluated one synthetic production transaction through the real control pipeline.',
  },
  {
    id: 'vue-frontend',
    label: 'Vue frontend',
    status: 'LIVE VERIFIED',
    notes:
      'Hosted operator SPA on Firebase Hosting. Sign in is required. This local page is the lab console, not that hosted bundle.',
  },
  {
    id: 'identity-federation',
    label: 'Identity Federation Gateway',
    status: 'TESTED',
    notes:
      'Local JWT/JWKS plus Entra OIDC BFF (local). Hosted operators use Firebase Google + BFF session. Production Entra is disabled.',
  },
  {
    id: 'webhook-defense',
    label: 'Webhook & Subscription Defense',
    status: 'TESTED',
    notes:
      'QuayForge HMAC, freshness, replay, and idempotency. Microsoft Graph subscriptions are not implemented.',
  },
  {
    id: 'contract-firewall',
    label: 'Contract Drift Firewall',
    status: 'TESTED',
    notes: 'Versioned OpenAPI/AsyncAPI/JSON Schema artifacts. Runtime validation and drift quarantine.',
  },
  {
    id: 'data-firewall',
    label: 'Data Boundary Firewall',
    status: 'TESTED',
    notes: 'Each destination uses ALLOW, REDACT, or DENY, and the outbound payload is actually transformed.',
  },
  {
    id: 'credential-vault',
    label: 'Credential & Consent Vault',
    status: 'TESTED',
    notes:
      'Local reference/version/consent model. Secret Manager containers are live; hosted connector secret read is not exercised.',
  },
  {
    id: 'quarantine-orchestrator',
    label: 'Quarantine & Recovery',
    status: 'LIVE VERIFIED',
    notes:
      'Hosted Worker recorded terminal quarantine and ACK. Local admin reprocess is tested. Not a public demo workflow.',
  },
  {
    id: 'audit-chain',
    label: 'Tamper evident audit chain',
    status: 'LIVE VERIFIED',
    notes: 'The deployed transaction produced a valid SHA256 audit chain stored in Firestore. This local page is the lab console.',
  },
  {
    id: 'exchange-attestation',
    label: 'Cryptographic Exchange Attestation',
    status: 'LIVE VERIFIED',
    notes: 'The deployed API signed a valid ES256 attestation with Cloud KMS; this local read view is the lab console.',
  },
  {
    id: 'boundary-graph',
    label: 'Integration Boundary Graph',
    status: 'TESTED',
    notes: 'Vue SVG from the Java graph API. Configured crossings plus observed crossings. Not a blast radius graph.',
  },
  {
    id: 'apache-camel',
    label: 'Apache Camel',
    status: 'TESTED',
    notes: 'Camel 4.22.0 routes run only in the worker. The public API artifact does not depend on Camel.',
  },
  {
    id: 'm365-graph',
    label: 'M365 / Graph',
    status: 'TESTED',
    notes:
      'Outlook Calendar connector through the Graph SDK and the contract harness. One personal account event was verified locally. Graph subscriptions are not implemented.',
  },
  {
    id: 'gcp',
    label: 'GCP',
    status: 'LIVE VERIFIED',
    notes:
      'Private Cloud Run API, public Web BFF, Worker Pool, Firestore, Pub/Sub, KMS, Firebase Hosting, and keyless workload IAM are live.',
  },
]
