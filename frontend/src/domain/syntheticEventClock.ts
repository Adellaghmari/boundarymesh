import { SYNTHETIC_DECISION_INSTANT } from '@/api/syntheticPortfolio'

/** Newest seeded crossing first. Each entry is a local calendar day before today. */
const SEEDED_DAYS_AGO: Readonly<Record<string, number>> = {
  'valid-crossing': 0,
  'transformation-required': 1,
  'destination-unavailable': 2,
  'invalid-identity': 3,
}

const PAST_CLOCKS: readonly { hour: number; minute: number; second: number }[] = [
  { hour: 16, minute: 28, second: 53 },
  { hour: 11, minute: 42, second: 6 },
  { hour: 8, minute: 17, second: 41 },
]

const TODAY_CLOCK = { hour: 9, minute: 6, second: 18 }

function startOfLocalDay(now: Date): Date {
  return new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0, 0)
}

function atLocalClock(day: Date, hour: number, minute: number, second: number): Date {
  return new Date(day.getFullYear(), day.getMonth(), day.getDate(), hour, minute, second, 0)
}

function clockKey(value: Date): string {
  return [value.getHours(), value.getMinutes(), value.getSeconds()].join(':')
}

function todayInstant(now: Date): Date {
  const start = startOfLocalDay(now)
  const preferred = atLocalClock(start, TODAY_CLOCK.hour, TODAY_CLOCK.minute, TODAY_CLOCK.second)
  if (preferred.getTime() <= now.getTime()) {
    return preferred
  }
  const reserved = new Set(PAST_CLOCKS.map((clock) => `${clock.hour}:${clock.minute}:${clock.second}`))
  let cursor = new Date(now)
  while (cursor.getTime() > start.getTime() && reserved.has(clockKey(cursor))) {
    cursor = new Date(cursor.getTime() - 1000)
  }
  return cursor.getTime() < start.getTime() ? start : cursor
}

/**
 * Local instant for one of the four seeded public crossings.
 * Real engine timestamps are left unchanged.
 */
export function syntheticEventDate(scenarioId: string, decidedAt: string, now: Date): Date | null {
  if (decidedAt !== SYNTHETIC_DECISION_INSTANT) {
    return null
  }
  const daysAgo = SEEDED_DAYS_AGO[scenarioId]
  if (daysAgo === undefined) {
    return null
  }
  if (daysAgo === 0) {
    return todayInstant(now)
  }
  const clock = PAST_CLOCKS[daysAgo - 1]
  if (!clock) {
    return null
  }
  const day = startOfLocalDay(now)
  day.setDate(day.getDate() - daysAgo)
  return atLocalClock(day, clock.hour, clock.minute, clock.second)
}

/** Visible decision time. Seeded synthetic crossings follow the visitor's local calendar. */
export function presentBoundaryEventTime(scenarioId: string, decidedAt: string, now = new Date()): string {
  const instant = syntheticEventDate(scenarioId, decidedAt, now)
  if (!instant) {
    return decidedAt
  }
  return new Intl.DateTimeFormat('en-GB', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(instant)
}
