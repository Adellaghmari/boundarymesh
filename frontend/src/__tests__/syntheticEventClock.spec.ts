import { createPinia, setActivePinia } from 'pinia'
import { describe, expect, it } from 'vitest'
import { presentBoundaryEventTime } from '@/domain/syntheticEventClock'
import { formatTimestamp, SYNTHETIC_HISTORY_MINUTES, syntheticHistoryInstant } from '@/domain/timestamp'
import { useEventsStore } from '@/stores/events'
import { sessionEvent } from './fixtures'

function local(year: number, month: number, day: number, hour: number, minute: number, second = 0): Date {
  return new Date(year, month - 1, day, hour, minute, second)
}

describe('timestamp presentation', () => {
  const stored = '2026-10-09T20:52:00.000Z'

  it('treats a Java instant a few seconds ahead of the display clock as just now', () => {
    const now = local(2026, 10, 10, 1, 15, 10)
    const instant = new Date(now.getTime() + 3_000).toISOString().replace('.000Z', '.311870100Z')
    const view = formatTimestamp(instant, now)
    expect(view?.primary).toBe('Today · 01:15')
    expect(view?.relative).toBe('Just now')
    expect(view?.primary).not.toMatch(/AM|PM/)
  })

  it('formats the same local day with a 24 hour clock', () => {
    const now = local(2026, 10, 9, 22, 52)
    const instant = now.toISOString()
    const view = formatTimestamp(instant, now)
    expect(view?.primary).toBe('Today · 22:52')
    expect(view?.primary).not.toMatch(/AM|PM/)
    expect(view?.relative).toBe('Just now')
  })

  it('formats the previous local calendar day as yesterday', () => {
    const event = local(2026, 10, 8, 23, 48)
    const now = local(2026, 10, 9, 0, 10)
    expect(formatTimestamp(event.toISOString(), now)?.primary).toBe('Yesterday · 23:48')
  })

  it('formats an older date with the calendar day', () => {
    const event = local(2026, 10, 8, 16, 28)
    const now = local(2026, 10, 10, 12, 0)
    expect(formatTimestamp(event.toISOString(), now)?.primary).toBe('8 Oct 2026 · 16:28')
  })

  it('keeps the year when the calendar year changes', () => {
    const event = local(2026, 12, 31, 18, 5)
    const now = local(2027, 3, 17, 14, 30)
    expect(formatTimestamp(event.toISOString(), now)?.primary).toBe('31 Dec 2026 · 18:05')
  })

  it('keeps the month when the calendar month changes', () => {
    const event = local(2027, 3, 1, 9, 15)
    const now = local(2027, 4, 2, 10, 0)
    expect(formatTimestamp(event.toISOString(), now)?.primary).toBe('1 Mar 2027 · 09:15')
  })

  it('rolls a late evening event to yesterday after local midnight', () => {
    const event = local(2026, 10, 9, 23, 58)
    const instant = event.toISOString()
    expect(formatTimestamp(instant, local(2026, 10, 9, 23, 59))?.primary).toBe('Today · 23:58')
    expect(formatTimestamp(instant, local(2026, 10, 10, 0, 1))?.primary).toBe('Yesterday · 23:58')
    expect(instant).toBe(event.toISOString())
  })

  it('does not rewrite a stored real timestamp while labels move forward', () => {
    const now = local(2026, 10, 9, 22, 52)
    const later = local(2026, 10, 10, 9, 0)
    expect(presentBoundaryEventTime('valid-crossing', stored, now)).not.toBe(stored)
    expect(presentBoundaryEventTime('valid-crossing', stored, later)).not.toBe(
      presentBoundaryEventTime('valid-crossing', stored, now),
    )
    expect(stored).toBe('2026-10-09T20:52:00.000Z')
  })

  it('creates distinct synthetic history instants from one origin', () => {
    const origin = new Date('2027-03-17T12:30:00.000Z')
    const instants = SYNTHETIC_HISTORY_MINUTES.map((minutes) => syntheticHistoryInstant(minutes, origin))
    expect(new Set(instants).size).toBe(SYNTHETIC_HISTORY_MINUTES.length)
    const millis = instants.map((value) => Date.parse(value))
    expect(millis[0]! - millis[1]!).toBe(6 * 60_000)
    expect(millis.every((value) => value <= origin.getTime())).toBe(true)
    expect(millis[0]).toBe(origin.getTime() - 2 * 60_000)
    expect(millis[millis.length - 1]).toBe(origin.getTime() - 41 * 60_000)
  })

  it('stamps only the synthetic session row and leaves a real event unchanged', () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const store = useEventsStore()
    const real = { ...sessionEvent, transactionId: 'real-crossing', decidedAt: '2026-01-01T00:00:00.000Z' }
    const seeded = { ...sessionEvent, transactionId: 'seeded-crossing', decidedAt: '2026-10-09T20:52:00.000Z' }
    store.events.push(real, seeded)
    const stamped = syntheticHistoryInstant(2, new Date('2027-03-17T12:30:00.000Z'))
    store.stampDecidedAt('seeded-crossing', stamped)
    expect(store.eventById('seeded-crossing')?.decidedAt).toBe(stamped)
    expect(store.eventById('real-crossing')?.decidedAt).toBe('2026-01-01T00:00:00.000Z')
    expect(store.eventById('seeded-crossing')?.decidedAt).toBe(stamped)
  })
})
