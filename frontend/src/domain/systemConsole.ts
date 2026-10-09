import { ABOUT_NAV, EVIDENCE_NAV, LAB_NAV, OPERATIONS_NAV, PRIMARY_NAV, STATUS_NAV } from '@/config/navigation'
import { show } from '@/domain/displayCopy'
import { DESTINATION_LABELS, OUTCOME_COPY, SOURCE_LABELS } from '@/domain/labels'
import type { BoundaryEventRecord, BoundaryOutcome, ControlKind } from '@/domain/types'

export type SystemConsoleTab = 'overview' | 'state' | 'timeline' | 'pipeline' | 'routes' | 'system' | 'about'

export interface InspectorStage {
  label: string
  status: string
  input: string
  result: string
  impact: string
  boundary: string
}

export interface InspectorField {
  key: string
  value: string
}

export interface InspectorGroup {
  id: string
  label: string
  fields: InspectorField[]
}

export interface InspectorRoute {
  name: string
  label: string
  to: string
  path: string
}

export interface ConsoleLogLine {
  clock: string
  message: string
}

export interface ConsolePipelineStage {
  label: string
  status: string
}

export interface ConsoleFact {
  label: string
  value: string
}

export interface SystemConsoleModel {
  runtimeMode: string
  routeLabel: string
  crossing: string
  decision: string
  worker: string
  delivery: string
  attestation: string
  pipeline: ConsolePipelineStage[]
  stages: InspectorStage[]
  groups: InspectorGroup[]
  log: ConsoleLogLine[]
  selectedLog: ConsoleLogLine[]
  state: ConsoleFact[]
}

const NAV = [...PRIMARY_NAV, ...LAB_NAV, ...OPERATIONS_NAV, ...EVIDENCE_NAV, STATUS_NAV, ABOUT_NAV]

export function routeLabel(name: unknown): string {
  const key = typeof name === 'string' ? name : ''
  return NAV.find((item) => item.name === key)?.label ?? 'Public page'
}

function clock(decidedAt: string, offsetSeconds: number): string {
  const parsed = Date.parse(decidedAt)
  const baseDate = Number.isNaN(parsed) ? null : new Date(parsed)
  const base = baseDate
    ? baseDate.getHours() * 3600 + baseDate.getMinutes() * 60 + baseDate.getSeconds()
    : 12 * 3600 + 41 * 60 + 3
  const total = (base + offsetSeconds) % 86400
  const hours = Math.floor(total / 3600)
  const minutes = Math.floor(total / 60) % 60
  const seconds = total % 60
  return [hours, minutes, seconds].map((part) => String(part).padStart(2, '0')).join(':')
}

function controlFailed(event: BoundaryEventRecord, kind: ControlKind): boolean {
  return event.appliedControls[kind] === 'FAIL'
}

function deliveryLabel(outcome: BoundaryOutcome): string {
  if (outcome === 'ALLOW') return 'Completed'
  if (outcome === 'ALLOW_WITH_TRANSFORMATION') return 'Completed after transformation'
  if (outcome === 'QUARANTINE') return 'Held'
  return 'Not attempted'
}

function workerLabel(outcome: BoundaryOutcome): string {
  if (outcome === 'ALLOW' || outcome === 'ALLOW_WITH_TRANSFORMATION') return 'Processed'
  return 'Idle'
}

function attestationLabel(outcome: BoundaryOutcome): string {
  if (outcome === 'ALLOW' || outcome === 'ALLOW_WITH_TRANSFORMATION') return 'Signed'
  if (outcome === 'QUARANTINE') return 'Signed for the hold'
  return 'Signed for the rejection'
}

function linesFor(event: BoundaryEventRecord): ConsoleLogLine[] {
  const messages = [
    'Crossing received',
    controlFailed(event, 'IDENTITY') ? 'Identity rejected' : 'Identity verified',
    'Authorization evaluated',
    'Authenticity verified',
    'Freshness accepted',
    controlFailed(event, 'REPLAY') ? 'Replay check failed' : 'Replay check passed',
    controlFailed(event, 'CONTRACT') ? 'Contract held' : 'Contract validated',
    'Policy evaluated',
    event.outcome === 'ALLOW_WITH_TRANSFORMATION' ? 'Transformation applied' : 'Transformation not required',
    `Policy result ${OUTCOME_COPY[event.outcome].label}`,
    'Attestation created',
    event.outcome === 'REJECT' || event.outcome === 'QUARANTINE' ? 'Delivery not queued' : 'Delivery queued',
    workerLabel(event.outcome) === 'Processed' ? 'Worker processed' : 'Worker idle',
    event.outcome === 'QUARANTINE'
      ? 'Delivery held'
      : event.outcome === 'REJECT'
        ? 'Delivery not attempted'
        : 'Delivery completed',
  ]
  return messages.map((message, index) => ({
    clock: clock(event.decidedAt, index),
    message,
  }))
}

function pipelineFor(event: BoundaryEventRecord): ConsolePipelineStage[] {
  const status = (kind: ControlKind, pass: string, fail: string) =>
    controlFailed(event, kind) ? fail : pass
  return [
    { label: 'Identity', status: status('IDENTITY', 'Verified', 'Rejected') },
    { label: 'Authorization', status: 'Evaluated' },
    { label: 'Authenticity', status: 'Verified' },
    { label: 'Freshness', status: 'Accepted' },
    { label: 'Replay', status: status('REPLAY', 'Passed', 'Failed') },
    { label: 'Contract', status: status('CONTRACT', 'Validated', 'Held') },
    { label: 'Policy', status: OUTCOME_COPY[event.outcome].label },
    {
      label: 'Transformation',
      status: event.outcome === 'ALLOW_WITH_TRANSFORMATION' ? 'Applied' : 'Not required',
    },
    { label: 'Attestation', status: attestationLabel(event.outcome) },
    { label: 'Delivery', status: deliveryLabel(event.outcome) },
  ]
}

function stage(label: string, status: string, input: string, result: string, impact: string): InspectorStage {
  return {
    label: show(label),
    status: show(status),
    input: show(input),
    result: show(result),
    impact: show(impact),
    boundary: show('Synthetic public boundary. No private API and no operator authority.'),
  }
}

function stagesFor(event: BoundaryEventRecord | null): InspectorStage[] {
  if (!event) {
    return [
      stage('Authenticate', 'Unavailable', 'No crossing selected', 'No synthetic result', 'None'),
      stage('Authorize', 'Unavailable', 'No crossing selected', 'No synthetic result', 'None'),
      stage('Verify authenticity', 'Unavailable', 'No crossing selected', 'No synthetic result', 'None'),
      stage('Check replay / freshness', 'Unavailable', 'No crossing selected', 'No synthetic result', 'None'),
      stage('Validate contract', 'Unavailable', 'No crossing selected', 'No synthetic result', 'None'),
      stage('Govern data', 'Unavailable', 'No crossing selected', 'No synthetic result', 'None'),
      stage('Enforce boundary', 'Unavailable', 'No crossing selected', 'No synthetic result', 'None'),
      stage('Resolve credential / consent', 'Unavailable', 'No crossing selected', 'No synthetic result', 'None'),
      stage('Deliver or quarantine', 'Unavailable', 'No crossing selected', 'No synthetic result', 'None'),
      stage('Record audit chain', 'Unavailable', 'No crossing selected', 'No synthetic result', 'None'),
      stage('Sign exchange attestation', 'Unavailable', 'No crossing selected', 'No synthetic result', 'None'),
    ]
  }
  const decision = OUTCOME_COPY[event.outcome].label
  const stopped = event.outcome === 'REJECT'
  const held = event.outcome === 'QUARANTINE'
  return [
    stage(
      'Authenticate',
      controlFailed(event, 'IDENTITY') ? 'Rejected' : 'Verified',
      'Synthetic caller identity',
      controlFailed(event, 'IDENTITY') ? 'Identity rejected' : 'Identity verified',
      controlFailed(event, 'IDENTITY') ? 'Stops the crossing' : 'Continues',
    ),
    stage('Authorize', 'Evaluated', 'Synthetic action and destination', 'Authorization evaluated', stopped ? 'Recorded before the stop' : 'Continues'),
    stage('Verify authenticity', 'Verified', 'Synthetic message integrity', 'Authenticity verified', 'Continues'),
    stage(
      'Check replay / freshness',
      controlFailed(event, 'REPLAY') || controlFailed(event, 'FRESHNESS') ? 'Failed' : 'Accepted',
      'Synthetic time window and replay key',
      controlFailed(event, 'REPLAY') ? 'Replay check failed' : 'Freshness accepted',
      controlFailed(event, 'REPLAY') ? 'Stops the crossing' : 'Continues',
    ),
    stage(
      'Validate contract',
      controlFailed(event, 'CONTRACT') ? 'Held' : 'Validated',
      'Synthetic payload contract',
      controlFailed(event, 'CONTRACT') ? 'Contract held' : 'Contract validated',
      controlFailed(event, 'CONTRACT') ? 'Holds the crossing' : 'Continues',
    ),
    stage(
      'Govern data',
      controlFailed(event, 'EGRESS') ? 'Restricted' : 'Allowed',
      'Synthetic field set',
      event.outcome === 'ALLOW_WITH_TRANSFORMATION' ? 'Transformation applied' : 'Fields accepted',
      event.outcome === 'ALLOW_WITH_TRANSFORMATION' ? 'Crosses after transformation' : 'Continues',
    ),
    stage(
      'Enforce boundary',
      decision,
      'Synthetic policy',
      `Policy result ${decision}`,
      stopped ? 'Rejects the crossing' : held ? 'Holds the crossing' : 'Allows the crossing',
    ),
    stage(
      'Resolve credential / consent',
      controlFailed(event, 'CREDENTIAL_RESOLVABILITY') ? 'Unresolved' : 'Resolved',
      'Synthetic credential reference',
      controlFailed(event, 'CREDENTIAL_RESOLVABILITY') ? 'Credential unresolved' : 'Credential reference accepted',
      'No secret is exposed',
    ),
    stage(
      'Deliver or quarantine',
      deliveryLabel(event.outcome),
      'Synthetic delivery instruction',
      deliveryLabel(event.outcome),
      held ? 'Held in quarantine' : stopped ? 'Delivery not attempted' : 'Delivery completed',
    ),
    stage('Record audit chain', 'Recorded', 'Synthetic stage order', 'Audit chain extended', 'Public sample only'),
    stage(
      'Sign exchange attestation',
      attestationLabel(event.outcome),
      'Synthetic decision record',
      attestationLabel(event.outcome),
      'Not a production signature',
    ),
  ]
}

function groupsFor(input: {
  routeName: unknown
  events: readonly BoundaryEventRecord[]
  selected: BoundaryEventRecord | null
}): InspectorGroup[] {
  const selected = input.selected
  const present = show
  const lines = selected ? linesFor(selected) : []
  const lastLine = lines.length > 0 ? lines[lines.length - 1] : undefined
  const latest = lastLine ? lastLine.message : 'None yet'
  return [
    {
      id: 'runtime',
      label: 'runtime',
      fields: [
        { key: 'mode', value: present('Synthetic demo') },
        { key: 'readOnly', value: present('true') },
        { key: 'privateApi', value: present('unavailable') },
        { key: 'operatorAuthority', value: present('unavailable') },
      ],
    },
    {
      id: 'events',
      label: 'events',
      fields: [
        { key: 'count', value: present(String(input.events.length)) },
        { key: 'latest', value: present(latest) },
      ],
    },
    {
      id: 'crossing',
      label: 'crossing',
      fields: [
        { key: 'id', value: present(selected?.transactionId ?? 'None yet') },
        { key: 'name', value: present(selected?.scenarioLabel ?? 'None yet') },
        { key: 'source', value: present(selected ? SOURCE_LABELS[selected.sourceSystem] : 'None yet') },
        { key: 'destination', value: present(selected ? DESTINATION_LABELS[selected.destinationSystem] : 'None yet') },
        { key: 'decision', value: present(selected ? OUTCOME_COPY[selected.outcome].label : 'None yet') },
      ],
    },
    {
      id: 'policy',
      label: 'policy',
      fields: [
        { key: 'version', value: present(selected?.policyVersion ?? 'None yet') },
        { key: 'decision', value: present(selected ? OUTCOME_COPY[selected.outcome].label : 'None yet') },
      ],
    },
    {
      id: 'delivery',
      label: 'delivery',
      fields: [
        { key: 'state', value: present(selected ? deliveryLabel(selected.outcome) : 'Not attempted') },
        { key: 'worker', value: present(selected ? workerLabel(selected.outcome) : 'Idle') },
      ],
    },
    {
      id: 'attestation',
      label: 'attestation',
      fields: [
        { key: 'state', value: present(selected ? attestationLabel(selected.outcome) : 'None yet') },
      ],
    },
    {
      id: 'route',
      label: 'route',
      fields: [
        { key: 'page', value: present(routeLabel(input.routeName)) },
        { key: 'publicMode', value: present('Synthetic demo') },
        { key: 'authRequired', value: present('No') },
        { key: 'privateApi', value: present('Unavailable') },
      ],
    },
  ]
}

export function inspectorRoutes(): InspectorRoute[] {
  return NAV.map((item) => ({
    name: item.name,
    label: show(item.label),
    to: item.to,
    path: show(item.to),
  }))
}

export function buildSystemConsole(input: {
  routeName: unknown
  events: readonly BoundaryEventRecord[]
  selected: BoundaryEventRecord | null
}): SystemConsoleModel {
  const selected = input.selected ?? input.events[0] ?? null
  const chronological = [...input.events].sort((left, right) => left.decidedAt.localeCompare(right.decidedAt) || left.transactionId.localeCompare(right.transactionId))
  const log = chronological.flatMap((event) => linesFor(event))
  const present = (value: string) => show(value)
  return {
    runtimeMode: present('Synthetic demo'),
    routeLabel: present(routeLabel(input.routeName)),
    crossing: present(selected?.scenarioLabel ?? 'None yet'),
    decision: present(selected ? OUTCOME_COPY[selected.outcome].label : 'None yet'),
    worker: present(selected ? workerLabel(selected.outcome) : 'Idle'),
    delivery: present(selected ? deliveryLabel(selected.outcome) : 'Not attempted'),
    attestation: present(selected ? attestationLabel(selected.outcome) : 'None yet'),
    pipeline: selected
      ? pipelineFor(selected).map((item) => ({ label: present(item.label), status: present(item.status) }))
      : [],
    stages: stagesFor(selected),
    groups: groupsFor({ ...input, selected }),
    log: log.map((line) => ({ clock: line.clock, message: present(line.message) })),
    selectedLog: (selected ? linesFor(selected) : []).map((line) => ({
      clock: line.clock,
      message: present(line.message),
    })),
    state: [
      { label: 'Runtime', value: present('Synthetic demo') },
      { label: 'Authority', value: present('Read only') },
      { label: 'Private API', value: present('Unavailable') },
      { label: 'Operator authority', value: present('Unavailable') },
      { label: 'Page', value: present(routeLabel(input.routeName)) },
      { label: 'Crossing', value: present(selected?.scenarioLabel ?? 'None yet') },
      { label: 'Decision', value: present(selected ? OUTCOME_COPY[selected.outcome].label : 'None yet') },
      { label: 'Policy', value: present(selected?.policyVersion ?? 'None yet') },
    ],
  }
}
