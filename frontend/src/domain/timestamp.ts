/** Stored instants stay ISO 8601 UTC. This module only formats them. */

const ISO_TIME = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d+)?(?:Z|[+-]\d{2}:\d{2})$/

/** Newest seeded crossing first. Offsets are fixed once the session is created. */
export const SYNTHETIC_HISTORY_MINUTES = [2, 8, 19, 41] as const

export interface TimestampView {
  primary: string
  relative: string | null
  exact: string
  instant: string
}

export function isTimestamp(value: string): boolean {
  return ISO_TIME.test(value)
}

export function syntheticHistoryInstant(minutesAgo: number, origin: Date): string {
  return new Date(origin.getTime() - minutesAgo * 60_000).toISOString()
}

function clock(date: Date): string {
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${hours}:${minutes}`
}

function exactClock(date: Date): string {
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${clock(date)}:${seconds}`
}

function startOfLocalDay(date: Date): number {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime()
}

function calendarLabel(date: Date): string {
  return new Intl.DateTimeFormat('en-GB', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  }).format(date)
}

function relativeLabel(instant: Date, now: Date): string | null {
  const delta = now.getTime() - instant.getTime()
  if (delta < -5_000 || delta >= 60 * 60_000) {
    return null
  }
  if (delta < 60_000) {
    return 'Just now'
  }
  const minutes = Math.floor(delta / 60_000)
  return minutes === 1 ? '1 min ago' : `${minutes} min ago`
}

/** Local presentation of a stored UTC instant. The stored value is not modified. */
export function formatTimestamp(value: string, now: Date): TimestampView | null {
  if (!isTimestamp(value)) {
    return null
  }
  const instant = new Date(value)
  if (Number.isNaN(instant.getTime())) {
    return null
  }
  const day = startOfLocalDay(now) - startOfLocalDay(instant)
  const time = clock(instant)
  let primary: string
  if (day === 0) {
    primary = `Today · ${time}`
  } else if (day === 86_400_000) {
    primary = `Yesterday · ${time}`
  } else {
    primary = `${calendarLabel(instant)} · ${time}`
  }
  return {
    primary,
    relative: relativeLabel(instant, now),
    exact: `${calendarLabel(instant)} · ${exactClock(instant)}`,
    instant: value,
  }
}
