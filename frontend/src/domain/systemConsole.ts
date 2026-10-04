import { ABOUT_NAV, EVIDENCE_NAV, LAB_NAV, OPERATIONS_NAV, PRIMARY_NAV, STATUS_NAV } from '@/config/navigation'
import { show } from '@/domain/displayCopy'
import { OUTCOME_COPY } from '@/domain/labels'
import type { BoundaryEventRecord, BoundaryOutcome, ControlKind } from '@/domain/types'

export type SystemConsoleTab = 'overview' | 'events' | 'pipeline' | 'state' | 'system'

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
  const match = /T(\d{2}):(\d{2}):(\d{2})/.exec(decidedAt)
  const base = match
    ? Number(match[1]) * 3600 + Number(match[2]) * 60 + Number(match[3])
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
      ? pipelineFor(selected).map((stage) => ({ label: present(stage.label), status: present(stage.status) }))
      : [],
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
