import type { BoundaryOutcome, ControlKind, ControlStatus, TraceStepView } from '@/domain/types'

export type ExplorerSection = 'overview' | 'crossing' | 'policy' | 'evidence' | 'delivery'

export interface ExplorerSectionTab {
  id: ExplorerSection
  label: string
}

export interface ExplorerCheck {
  kind: ControlKind
  status: ControlStatus
  detail: string
}

export interface ExplorerTimelineEntry {
  id: string
  title: string
  detail: string
}

export interface ExplorerCrossing {
  id: string
  name: string
  source: string
  destination: string
  outcome: BoundaryOutcome
  summary: string
  incoming: string
  identity: string
  authorization: string
  authenticity: string
  freshness: string
  replay: string
  contract: string
  policy: string
  transformation: string
  attestation: string
  signing: string
  instruction: string
  worker: string
  terminal: string
  checks: readonly ExplorerCheck[]
  timeline: readonly ExplorerTimelineEntry[]
}

export const EXPLORER_SECTIONS: readonly ExplorerSectionTab[] = [
  { id: 'overview', label: 'Overview' },
  { id: 'crossing', label: 'Crossing' },
  { id: 'policy', label: 'Policy' },
  { id: 'evidence', label: 'Evidence' },
  { id: 'delivery', label: 'Delivery' },
]

export const EXPLORER_LOCKED_ACTIONS = [
  'Acknowledge hold',
  'Retry delivery',
  'Approve release',
  'Reject crossing',
] as const

const pass = (kind: ControlKind, detail: string): ExplorerCheck => ({ kind, status: 'PASS', detail })
const fail = (kind: ControlKind, detail: string): ExplorerCheck => ({ kind, status: 'FAIL', detail })

export const PORTFOLIO_EXPLORER_CROSSINGS: readonly ExplorerCrossing[] = [
  {
    id: '4108',
    name: 'QuayForge order accepted',
    source: 'QuayForge',
    destination: 'Rivertide',
    outcome: 'ALLOW',
    summary: 'A signed partner order passes every check and is delivered unchanged.',
    incoming: 'QuayForge sends order 4108 to Rivertide. The payload is an untrusted settlement request entering another trust domain.',
    identity: 'Source identity quayforge.orders is recognized for this integration.',
    authorization: 'That identity may submit an order to Rivertide. The requested action is permitted.',
    authenticity: 'The partner signature matches the expected key. The payload was not altered in transit.',
    freshness: 'The request timestamp is inside the accepted time window.',
    replay: 'Request identifier 4108 has not been seen before.',
    contract: 'The payload matches the QuayForge orders contract.',
    policy: 'Policy allows the order to leave unchanged.',
    transformation: 'No field was changed. The approved body matches the incoming order.',
    attestation: 'Exchange attestation for crossing 4108 is signed. The recorded verification result is valid.',
    signing: 'Cloud KMS signed that attestation with the exchange key. The browser never holds the key. This page shows the recorded result only.',
    instruction: 'Delivery instruction 4108 was created after the allow decision.',
    worker: 'The worker consumed the instruction and recorded a terminal state. No external connector was called.',
    terminal: 'Delivered. BoundaryMesh finished the instruction without calling Rivertide.',
    checks: [
      pass('IDENTITY', 'Source identity quayforge.orders is recognized for this integration.'),
      pass('AUTHORIZATION', 'That identity may submit an order to Rivertide.'),
      pass('AUTHENTICITY', 'The partner signature matches the expected key.'),
      pass('FRESHNESS', 'The request timestamp is inside the accepted time window.'),
      pass('REPLAY', 'Request identifier 4108 has not been seen before.'),
      pass('CONTRACT', 'The payload matches the QuayForge orders contract.'),
      pass('EGRESS', 'Every field in the order may leave for Rivertide.'),
    ],
    timeline: [
      { id: '4108-received', title: 'Request received', detail: 'QuayForge order 4108 arrived as an untrusted payload.' },
      { id: '4108-identity', title: 'Source identity recognized', detail: 'quayforge.orders is the expected partner identity.' },
      { id: '4108-policy', title: 'Policy allowed the crossing', detail: 'The order may leave unchanged.' },
      { id: '4108-attestation', title: 'Exchange attestation signed', detail: 'Cloud KMS recorded a valid signature for this decision.' },
      { id: '4108-worker', title: 'Worker recorded delivery', detail: 'Terminal state is delivered. No external connector was called.' },
    ],
  },
  {
    id: '4114',
    name: 'Contact field transformed',
    source: 'QuayForge',
    destination: 'Rivertide',
    outcome: 'ALLOW_WITH_TRANSFORMATION',
    summary: 'A Rivertide settlement may leave only after the contact email is removed.',
    incoming: 'QuayForge order 4114 includes a contact email that Rivertide must not receive.',
    identity: 'Source identity quayforge.orders is recognized for this integration.',
    authorization: 'That identity may submit a settlement to Rivertide.',
    authenticity: 'The partner signature matches the expected key.',
    freshness: 'The request timestamp is inside the accepted time window.',
    replay: 'Request identifier 4114 has not been seen before.',
    contract: 'The payload matches the QuayForge orders contract.',
    policy: 'Policy allows the settlement only after the contact email is removed.',
    transformation: 'The contact email was replaced with a redaction marker. The order amount and reference remain.',
    attestation: 'Exchange attestation for crossing 4114 is signed and covers the transformed body.',
    signing: 'Cloud KMS signed the attestation after the transformation. The signing key stays in Cloud KMS.',
    instruction: 'Delivery instruction 4114 carries the transformed body, not the original email.',
    worker: 'The worker consumed the transformed instruction. No external connector was called.',
    terminal: 'Delivered after transformation. The original email did not leave BoundaryMesh.',
    checks: [
      pass('IDENTITY', 'Source identity quayforge.orders is recognized for this integration.'),
      pass('AUTHORIZATION', 'That identity may submit a settlement to Rivertide.'),
      pass('AUTHENTICITY', 'The partner signature matches the expected key.'),
      pass('FRESHNESS', 'The request timestamp is inside the accepted time window.'),
      pass('REPLAY', 'Request identifier 4114 has not been seen before.'),
      pass('CONTRACT', 'The payload matches the QuayForge orders contract.'),
      pass('EGRESS', 'The contact email is removed before the settlement may leave.'),
    ],
    timeline: [
      { id: '4114-received', title: 'Request received', detail: 'Order 4114 included a contact email.' },
      { id: '4114-policy', title: 'Policy required a transformation', detail: 'The email may not leave for Rivertide.' },
      { id: '4114-transform', title: 'Transformation applied', detail: 'The email was replaced with a redaction marker.' },
      { id: '4114-attestation', title: 'Exchange attestation signed', detail: 'The signature covers the transformed body.' },
      { id: '4114-worker', title: 'Worker recorded delivery', detail: 'Terminal state is delivered after transformation.' },
    ],
  },
  {
    id: '4120',
    name: 'Contract drift held',
    source: 'QuayForge',
    destination: 'Rivertide',
    outcome: 'QUARANTINE',
    summary: 'An unexpected field stops the crossing and holds it for an operator.',
    incoming: 'QuayForge order 4120 includes a field the Rivertide contract does not allow.',
    identity: 'Source identity quayforge.orders is recognized for this integration.',
    authorization: 'The identity is permitted to submit an order, but the payload is not acceptable yet.',
    authenticity: 'The partner signature matches the expected key.',
    freshness: 'The request timestamp is inside the accepted time window.',
    replay: 'Request identifier 4120 has not been seen before.',
    contract: 'Contract validation failed. The payload includes a field the contract does not allow.',
    policy: 'Policy quarantines the crossing. It is held instead of delivered or rejected outright.',
    transformation: 'No transformation was applied. The payload remains in BoundaryMesh custody.',
    attestation: 'Exchange attestation for crossing 4120 records the quarantine decision.',
    signing: 'Cloud KMS signed the quarantine attestation. Signing does not release the payload.',
    instruction: 'No delivery instruction was created.',
    worker: 'The worker was not asked to process this crossing.',
    terminal: 'Held. Terminal custody stays with BoundaryMesh until an authenticated operator acts.',
    checks: [
      pass('IDENTITY', 'Source identity quayforge.orders is recognized for this integration.'),
      pass('AUTHORIZATION', 'The identity may submit an order.'),
      pass('AUTHENTICITY', 'The partner signature matches the expected key.'),
      pass('FRESHNESS', 'The request timestamp is inside the accepted time window.'),
      pass('REPLAY', 'Request identifier 4120 has not been seen before.'),
      fail('CONTRACT', 'The payload includes a field the contract does not allow.'),
      fail('EGRESS', 'No field is released while the contract is invalid.'),
    ],
    timeline: [
      { id: '4120-received', title: 'Request received', detail: 'Order 4120 arrived with an unexpected field.' },
      { id: '4120-contract', title: 'Contract validation failed', detail: 'The field is outside the QuayForge orders contract.' },
      { id: '4120-policy', title: 'Policy quarantined the crossing', detail: 'The payload is held for an authenticated operator.' },
      { id: '4120-attestation', title: 'Exchange attestation signed', detail: 'The attestation records the hold. It does not release the payload.' },
      { id: '4120-worker', title: 'Worker not started', detail: 'No delivery instruction exists.' },
    ],
  },
  {
    id: '4126',
    name: 'Replay rejected',
    source: 'QuayForge',
    destination: 'Rivertide',
    outcome: 'REJECT',
    summary: 'A repeated request identifier is rejected before any delivery exists.',
    incoming: 'QuayForge sends order 4126 again with a request identifier that was already accepted.',
    identity: 'Source identity quayforge.orders is recognized for this integration.',
    authorization: 'The identity would be allowed to submit a new order. This request is not new.',
    authenticity: 'The partner signature matches the expected key.',
    freshness: 'The timestamp is inside the window, but freshness does not excuse a replay.',
    replay: 'Replay detection matched request identifier 4126 to an earlier acceptance.',
    contract: 'The payload shape matches the contract. The replay decision still blocks it.',
    policy: 'Policy rejects the crossing. A repeated request must not cross the boundary.',
    transformation: 'No transformation was applied. Nothing was approved to leave.',
    attestation: 'Exchange attestation for crossing 4126 records the rejection.',
    signing: 'Cloud KMS signed the rejection attestation. The signature is evidence, not approval to deliver.',
    instruction: 'No delivery instruction was created.',
    worker: 'The worker was not started.',
    terminal: 'Not attempted. The rejected request never became delivery work.',
    checks: [
      pass('IDENTITY', 'Source identity quayforge.orders is recognized for this integration.'),
      pass('AUTHORIZATION', 'The identity may submit a new order. This request is not new.'),
      pass('AUTHENTICITY', 'The partner signature matches the expected key.'),
      pass('FRESHNESS', 'The timestamp is inside the window.'),
      fail('REPLAY', 'Request identifier 4126 was already accepted.'),
      pass('CONTRACT', 'The payload shape matches the contract.'),
      fail('EGRESS', 'A rejected request releases no fields.'),
    ],
    timeline: [
      { id: '4126-received', title: 'Request received', detail: 'Order 4126 repeated an earlier request identifier.' },
      { id: '4126-replay', title: 'Replay detected', detail: 'The identifier was already accepted.' },
      { id: '4126-policy', title: 'Policy rejected the crossing', detail: 'The request must not leave.' },
      { id: '4126-attestation', title: 'Exchange attestation signed', detail: 'Cloud KMS signed the rejection record.' },
      { id: '4126-worker', title: 'Delivery not attempted', detail: 'No instruction was created and the worker did not start.' },
    ],
  },
]

export function filterPortfolioCrossings(
  crossings: readonly ExplorerCrossing[],
  query: string,
  outcome: BoundaryOutcome | 'ALL',
): ExplorerCrossing[] {
  const needle = query.trim().toLowerCase()
  return crossings.filter((crossing) => {
    if (outcome !== 'ALL' && crossing.outcome !== outcome) return false
    if (!needle) return true
    const haystack = [crossing.name, crossing.source, crossing.destination, crossing.summary, crossing.outcome, crossing.id]
      .join(' ')
      .toLowerCase()
    return haystack.includes(needle)
  })
}

export function explorerTrace(crossing: ExplorerCrossing): TraceStepView[] {
  return crossing.checks.map((check) => ({
    kind: check.kind,
    status: check.status,
    explanation: check.detail,
    reference: crossing.id,
  }))
}
