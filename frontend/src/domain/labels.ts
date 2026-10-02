import type {
  BoundaryOutcome,
  CapabilityStatus,
  ControlKind,
  ControlStatus,
  DestinationSystem,
  SourceSystem,
} from '@/domain/types'

export interface ControlCatalogEntry {
  kind: ControlKind
  label: string
  summary: string
}

export const CONTROL_CATALOG: readonly ControlCatalogEntry[] = [
  {
    kind: 'IDENTITY',
    label: 'Identity',
    summary: 'Whether the calling identity is authentic for this crossing.',
  },
  {
    kind: 'AUTHORIZATION',
    label: 'Authorization',
    summary: 'Whether this specific action is permitted at the boundary.',
  },
  {
    kind: 'AUTHENTICITY',
    label: 'Authenticity',
    summary: 'Whether the message integrity and signature checks hold.',
  },
  {
    kind: 'FRESHNESS',
    label: 'Freshness',
    summary: 'Whether the transaction is inside the accepted time window.',
  },
  {
    kind: 'REPLAY',
    label: 'Replay Defense',
    summary: 'Whether a prohibited replay was detected inside the protected window.',
  },
  {
    kind: 'CONTRACT',
    label: 'Contract',
    summary: 'Whether the payload matches the expected integration contract.',
  },
  {
    kind: 'EGRESS',
    label: 'Egress',
    summary: 'Whether the rules for this destination allow the fields to leave.',
  },
  {
    kind: 'CREDENTIAL_RESOLVABILITY',
    label: 'Credential resolvability',
    summary: 'Whether required connector credentials can be resolved safely.',
  },
  {
    kind: 'CONNECTOR_SAFETY',
    label: 'Connector safety',
    summary: 'Whether the destination connector is safe to use right now.',
  },
]

export const OUTCOME_COPY: Record<BoundaryOutcome, { label: string; meaning: string }> = {
  ALLOW: {
    label: 'ALLOW',
    meaning: 'The transaction may cross this boundary unchanged.',
  },
  ALLOW_WITH_TRANSFORMATION: {
    label: 'ALLOW WITH TRANSFORMATION',
    meaning: 'The transaction may cross only after deterministic transformation.',
  },
  QUARANTINE: {
    label: 'QUARANTINE',
    meaning: 'The crossing is held for a recoverable operational reason.',
  },
  REJECT: {
    label: 'REJECT',
    meaning: 'The transaction must not cross this security boundary.',
  },
}

export const CONTROL_STATUS_COPY: Record<ControlStatus, string> = {
  PASS: 'PASS',
  FAIL: 'FAIL',
  NOT_APPLICABLE: 'NOT APPLICABLE',
  UNAVAILABLE: 'UNAVAILABLE',
}

export const SOURCE_LABELS: Record<SourceSystem, string> = {
  HELIX_365: 'Helix 365',
  QUAYFORGE: 'QuayForge Orders',
  SYNTHETIC: 'Synthetic source',
}

export const DESTINATION_LABELS: Record<DestinationSystem, string> = {
  COVECRM: 'CoveCRM',
  RIVERTIDE: 'Rivertide Settlements',
  LUMENWAY: 'Lumenway Fulfillment',
  NONE: 'No destination',
}

export const CAPABILITY_STATUS_COPY: Record<CapabilityStatus, string> = {
  PLANNED: 'PLANNED',
  IMPLEMENTED: 'IMPLEMENTED',
  TESTED: 'TESTED',
  'LIVE VERIFIED': 'LIVE VERIFIED',
}

export function crossingLabel(source: SourceSystem, destination: DestinationSystem): string {
  return `${SOURCE_LABELS[source]} → ${DESTINATION_LABELS[destination]}`
}
