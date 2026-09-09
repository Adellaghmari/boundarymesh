import type { CapabilityClaim } from '@/domain/types'

/**
 * Manual frontend mirror of CV_CLAIMS_MATRIX.md for Phase 7.
 * Not fetched from Git. Do not invent progress.
 */
export const CAPABILITY_CLAIMS: readonly CapabilityClaim[] = [
  {
    id: 'java-backend',
    label: 'Java backend',
    status: 'TESTED',
    notes: 'Java 25 / Spring Boot 4.1.1 local API. Maven verify.',
  },
  {
    id: 'enforcement-engine',
    label: 'Boundary Enforcement Engine',
    status: 'TESTED',
    notes: 'Deterministic ControlVector decisions including real contract and egress controls.',
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
      'Local JWT/JWKS resource server and AuthN vs AuthZ evidence. Microsoft Entra remains PLANNED.',
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
    status: 'PLANNED',
    notes: 'No Secret Manager and no stored credentials.',
  },
  {
    id: 'quarantine-orchestrator',
    label: 'Quarantine & Recovery',
    status: 'PLANNED',
    notes: 'QUARANTINE is an engine outcome only. No queue or reprocess.',
  },
  {
    id: 'apache-camel',
    label: 'Apache Camel',
    status: 'PLANNED',
    notes: 'No Camel dependency or routes.',
  },
  {
    id: 'm365-graph',
    label: 'M365 / Graph',
    status: 'PLANNED',
    notes: 'Helix 365 is fictional demo context. Graph is not integrated.',
  },
  {
    id: 'gcp',
    label: 'GCP',
    status: 'PLANNED',
    notes: 'No Cloud Run, Pub/Sub, Firestore, Secret Manager, or KMS.',
  },
]
