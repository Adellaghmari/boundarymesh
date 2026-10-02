import { ROUTE_NAMES } from '@/router'

export interface NavDestination {
  name: string
  to: string
  label: string
  description?: string
}

export const PRIMARY_NAV: readonly NavDestination[] = [
  { name: ROUTE_NAMES.overview, to: '/', label: 'Overview' },
  { name: ROUTE_NAMES.boundaryEvents, to: '/boundary-events', label: 'Boundary Events' },
  { name: ROUTE_NAMES.connections, to: '/connections', label: 'Connections' },
  { name: ROUTE_NAMES.architecture, to: '/architecture', label: 'Architecture' },
  { name: ROUTE_NAMES.boundaryGraph, to: '/boundary-graph', label: 'Boundary Graph' },
  { name: ROUTE_NAMES.crossing, to: '/crossing', label: 'Full Crossing' },
]

export const LAB_NAV: readonly NavDestination[] = [
  {
    name: ROUTE_NAMES.identity,
    to: '/identity',
    label: 'Identity Lab',
    description: 'JWT AuthN vs AuthZ',
  },
  {
    name: ROUTE_NAMES.webhooks,
    to: '/webhooks',
    label: 'Webhook Lab',
    description: 'HMAC, freshness, replay',
  },
  {
    name: ROUTE_NAMES.contracts,
    to: '/contracts',
    label: 'Contract Lab',
    description: 'Versioned contract drift',
  },
  {
    name: ROUTE_NAMES.dataBoundary,
    to: '/data-boundary',
    label: 'Data Boundary',
    description: 'Destination egress policy',
  },
]

export const OPERATIONS_NAV: readonly NavDestination[] = [
  { name: ROUTE_NAMES.delivery, to: '/delivery', label: 'Delivery', description: 'Camel delivery lab' },
  {
    name: ROUTE_NAMES.credentials,
    to: '/credentials',
    label: 'Credentials',
    description: 'References and consent',
  },
  {
    name: ROUTE_NAMES.quarantine,
    to: '/quarantine',
    label: 'Quarantine',
    description: 'Hold and authorized reprocess',
  },
]

export const EVIDENCE_NAV: readonly NavDestination[] = [
  { name: ROUTE_NAMES.audit, to: '/audit', label: 'Audit', description: 'Tamper evident chain' },
  {
    name: ROUTE_NAMES.attestations,
    to: '/attestations',
    label: 'Attestations',
    description: 'Signed crossing artifacts',
  },
]

export const STATUS_NAV: NavDestination = {
  name: ROUTE_NAMES.status,
  to: '/status',
  label: 'Status',
}

export const MOBILE_GROUPS = [
  { id: 'core', label: 'Core', items: PRIMARY_NAV },
  { id: 'labs', label: 'Labs', items: LAB_NAV },
  { id: 'operations', label: 'Operations', items: OPERATIONS_NAV },
  { id: 'evidence', label: 'Evidence', items: EVIDENCE_NAV },
  { id: 'project', label: 'Project', items: [STATUS_NAV] },
] as const
