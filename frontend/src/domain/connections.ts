import type { DestinationSystem, SourceSystem } from '@/domain/types'

export interface ConnectionAction {
  label: 'View contract' | 'Inspect policy path' | 'View synthetic flow'
  to: string
}

export interface VeloraSystem {
  id: string
  name: string
  role: string
  character: string
  connectorStatus: 'SYNTHETIC EVALUATE ONLY' | 'PLANNED'
  systemCode?: SourceSystem | DestinationSystem
  mode: string
  source: string
  destination: string
  contract: string
  permissions: string
  policyPath: string
  crossing: string
  syntheticStatus: string
  recentOutcome: string
  actions: readonly ConnectionAction[]
}

const CONTRACT: ConnectionAction = { label: 'View contract', to: '/contracts' }
const POLICY: ConnectionAction = { label: 'Inspect policy path', to: '/data-boundary' }
const FLOW: ConnectionAction = { label: 'View synthetic flow', to: '/crossing' }

export const VELORA_SYSTEMS: readonly VeloraSystem[] = [
  {
    id: 'helix-365',
    name: 'Helix 365',
    role: 'Calendar / collaboration source',
    character:
      'Fictional context similar to Microsoft 365 for Velora Interchange. Not a live Microsoft 365 tenant and not a Microsoft Graph integration.',
    connectorStatus: 'SYNTHETIC EVALUATE ONLY',
    systemCode: 'HELIX_365',
    mode: 'Synthetic evaluate only',
    source: 'Helix 365',
    destination: 'CoveCRM',
    contract: 'Helix calendar to CoveCRM',
    permissions: 'No live permission. This is not a Microsoft Graph tenant.',
    policyPath: 'Destination egress for CoveCRM',
    crossing: 'Synthetic evaluate. No live connector is called.',
    syntheticStatus: 'Synthetic',
    recentOutcome: 'Seeded crossing allows the calendar record into CoveCRM.',
    actions: [CONTRACT, POLICY, FLOW],
  },
  {
    id: 'covecrm',
    name: 'CoveCRM',
    role: 'CRM destination',
    character: 'Inbound activity records with strict destination egress rules.',
    connectorStatus: 'SYNTHETIC EVALUATE ONLY',
    systemCode: 'COVECRM',
    mode: 'Synthetic evaluate only',
    source: 'Helix 365',
    destination: 'CoveCRM',
    contract: 'Inbound activity record',
    permissions: 'No external credential. The public demo evaluates the crossing locally.',
    policyPath: 'Destination egress for CoveCRM',
    crossing: 'Synthetic evaluate of an inbound activity record.',
    syntheticStatus: 'Synthetic',
    recentOutcome: 'Seeded crossing allows the inbound activity.',
    actions: [CONTRACT, POLICY, FLOW],
  },
  {
    id: 'quayforge',
    name: 'QuayForge Orders',
    role: 'Order / commerce source',
    character:
      'Signed partner webhooks are planned. Phase 2 uses synthetic evaluate payloads only.',
    connectorStatus: 'SYNTHETIC EVALUATE ONLY',
    systemCode: 'QUAYFORGE',
    mode: 'Synthetic evaluate only',
    source: 'QuayForge Orders',
    destination: 'Rivertide Settlements',
    contract: 'QuayForge orders v1',
    permissions: 'No live webhook receiver. Signed partner webhooks remain planned.',
    policyPath: 'Destination egress for Rivertide',
    crossing: 'Synthetic evaluate of a signed order payload.',
    syntheticStatus: 'Synthetic',
    recentOutcome: 'Seeded crossing holds the order when Rivertide is unavailable.',
    actions: [CONTRACT, POLICY, FLOW],
  },
  {
    id: 'rivertide',
    name: 'Rivertide Settlements',
    role: 'Settlement destination',
    character: 'High sensitivity fictional destination. Not a real payment processor.',
    connectorStatus: 'SYNTHETIC EVALUATE ONLY',
    systemCode: 'RIVERTIDE',
    mode: 'Synthetic evaluate only',
    source: 'QuayForge Orders',
    destination: 'Rivertide Settlements',
    contract: 'Settlement payload',
    permissions: 'No payment credential. Rivertide is not a live processor.',
    policyPath: 'Destination egress for Rivertide',
    crossing: 'Synthetic evaluate. A sensitive field can be denied before it leaves.',
    syntheticStatus: 'Synthetic',
    recentOutcome: 'Seeded crossing quarantines the settlement when the connector is unavailable.',
    actions: [CONTRACT, POLICY, FLOW],
  },
  {
    id: 'lumenway',
    name: 'Lumenway Fulfillment',
    role: 'Fulfillment destination',
    character: 'Broader destination allowlist in later data boundary work. No live connector yet.',
    connectorStatus: 'PLANNED',
    systemCode: 'LUMENWAY',
    mode: 'Not enabled',
    source: 'QuayForge Orders',
    destination: 'Lumenway Fulfillment',
    contract: 'Fulfillment payload, described in the contract lab',
    permissions: 'No connector credential is configured.',
    policyPath: 'Destination egress for Lumenway, including redaction',
    crossing: 'Not enabled in this public environment. No live connector exists.',
    syntheticStatus: 'Planned',
    recentOutcome: 'No seeded crossing. The connector is not enabled.',
    actions: [CONTRACT, POLICY, { label: 'View synthetic flow', to: '/delivery' }],
  },
]
