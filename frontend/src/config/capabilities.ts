import type { CapabilityClaim } from '@/domain/types'

/**
 * Manual frontend summary of reviewed project evidence through Phase 15I.
 * Not fetched from Git. Do not invent progress.
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
    status: 'TESTED',
    notes: 'Vue 3 recruiter console against the local backend. No public deploy.',
  },
  {
    id: 'identity-federation',
    label: 'Identity Federation Gateway',
    status: 'TESTED',
    notes:
      'Local JWT/JWKS resource server plus Entra OIDC BFF architecture. Live tenant login is not claimed until configured.',
  },
  {
    id: 'webhook-defense',
    label: 'Webhook & Subscription Defense',
    status: 'TESTED',
    notes:
      'QuayForge HMAC, freshness, replay, and idempotency. Microsoft Graph subscriptions remain PLANNED.',
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
      'Local reference/version/consent model. Secret Manager containers and adapters are live; D1 did not exercise a deployed workload secret read.',
  },
  {
    id: 'quarantine-orchestrator',
    label: 'Quarantine & Recovery',
    status: 'TESTED',
    notes: 'Local reprocess is tested. The deployed worker durably recorded terminal quarantine and ACKed; browser recovery is not live.',
  },
  {
    id: 'audit-chain',
    label: 'Tamper-Evident Audit Chain',
    status: 'LIVE VERIFIED',
    notes: 'The deployed transaction produced a valid Firestore-backed SHA-256 audit chain; this browser read view remains local.',
  },
  {
    id: 'exchange-attestation',
    label: 'Cryptographic Exchange Attestation',
    status: 'LIVE VERIFIED',
    notes: 'The deployed API signed a valid ES256 attestation with Cloud KMS; this browser read view remains local.',
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
      'Outlook Calendar connector through Graph SDK + contract harness. Live tenant not claimed. Graph subscriptions remain PLANNED.',
  },
  {
    id: 'gcp',
    label: 'GCP',
    status: 'LIVE VERIFIED',
    notes: 'Private Cloud Run API, Worker Pool, Firestore, Pub/Sub, KMS, and keyless workload IAM are live. Firebase Hosting is not deployed.',
  },
]
