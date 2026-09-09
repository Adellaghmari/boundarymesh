import type { DestinationSystem, SourceSystem } from '@/domain/types'

export interface VeloraSystem {
  id: string
  name: string
  role: string
  character: string
  connectorStatus: 'SYNTHETIC EVALUATE ONLY' | 'PLANNED'
  systemCode?: SourceSystem | DestinationSystem
}

export const VELORA_SYSTEMS: readonly VeloraSystem[] = [
  {
    id: 'helix-365',
    name: 'Helix 365',
    role: 'Calendar / collaboration source',
    character:
      'Fictional Microsoft-365-like demo context for Velora Interchange. Not a live Microsoft 365 tenant and not a Microsoft Graph integration.',
    connectorStatus: 'SYNTHETIC EVALUATE ONLY',
    systemCode: 'HELIX_365',
  },
  {
    id: 'covecrm',
    name: 'CoveCRM',
    role: 'CRM destination',
    character: 'Inbound activity records with strict destination egress rules.',
    connectorStatus: 'SYNTHETIC EVALUATE ONLY',
    systemCode: 'COVECRM',
  },
  {
    id: 'quayforge',
    name: 'QuayForge Orders',
    role: 'Order / commerce source',
    character:
      'Signed partner webhooks are planned. Phase 2 uses synthetic evaluate payloads only.',
    connectorStatus: 'SYNTHETIC EVALUATE ONLY',
    systemCode: 'QUAYFORGE',
  },
  {
    id: 'rivertide',
    name: 'Rivertide Settlements',
    role: 'Settlement destination',
    character: 'High-sensitivity fictional destination. Not a real payment processor.',
    connectorStatus: 'SYNTHETIC EVALUATE ONLY',
    systemCode: 'RIVERTIDE',
  },
  {
    id: 'lumenway',
    name: 'Lumenway Fulfillment',
    role: 'Fulfillment destination',
    character: 'Broader destination allowlist in later data-boundary work. No live connector yet.',
    connectorStatus: 'PLANNED',
    systemCode: 'LUMENWAY',
  },
]
