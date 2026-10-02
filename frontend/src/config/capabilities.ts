import type { CapabilityClaim } from '@/domain/types'

/**
 * Manual frontend summary of reviewed project evidence.
 * Aligns with PROJECT_STATUS.md / CV_CLAIMS_MATRIX.md. Not a live cloud feed.
 */
export const CAPABILITY_CLAIMS: readonly CapabilityClaim[] = [
  {
    id: 'java-backend',
    label: 'Java backend',
    status: 'LIVE VERIFIED',
    notes: 'Java 25 / Spring Boot 4.1.1 private Cloud Run API. Production transaction ingress verified.',
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
      'Hosted operator SPA on Firebase Hosting (sign-in required). This local page is the lab console, not that hosted bundle.',
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
    notes: 'Destination-specific ALLOW/REDACT/DENY with real outbound transformation.',
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
    label: 'Tamper-Evident Audit Chain',
    status: 'LIVE VERIFIED',
    notes: 'The deployed transaction produced a valid Firestore-backed SHA-256 audit chain; this local read view is the lab console.',
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
    notes: 'Vue SVG from the Java graph API. Configured plus observed crossings. Not a blast-radius graph.',
  },
  {
    id: 'apache-camel',
    label: 'Apache Camel',
    status: 'TESTED',
    notes: 'Worker-only Camel 4.22.0 routes. The public API artifact does not depend on Camel.',
  },
  {
    id: 'm365-graph',
    label: 'M365 / Graph',
    status: 'TESTED',
    notes:
      'Outlook Calendar connector through Graph SDK + contract harness. One personal-account event verified locally. Graph subscriptions are not implemented.',
  },
  {
    id: 'gcp',
    label: 'GCP',
    status: 'LIVE VERIFIED',
    notes:
      'Private Cloud Run API, public Web BFF, Worker Pool, Firestore, Pub/Sub, KMS, Firebase Hosting, and keyless workload IAM are live.',
  },
]
