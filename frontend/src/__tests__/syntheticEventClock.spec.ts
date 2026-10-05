import { describe, expect, it } from 'vitest'
import { SYNTHETIC_DECISION_INSTANT } from '@/api/syntheticPortfolio'
import { presentBoundaryEventTime, syntheticEventDate } from '@/domain/syntheticEventClock'

const SEEDED = [
  'valid-crossing',
  'transformation-required',
  'destination-unavailable',
  'invalid-identity',
] as const

function local(year: number, month: number, day: number, hour: number, minute: number, second: number): Date {
  return new Date(year, month - 1, day, hour, minute, second)
}

describe('synthetic event clock', () => {
  const afternoon = local(2026, 10, 5, 15, 4, 0)

  it('places the four seeded crossings on consecutive local days with different clocks', () => {
    const dates = SEEDED.map((id) => syntheticEventDate(id, SYNTHETIC_DECISION_INSTANT, afternoon))
    expect(dates.every((date) => date instanceof Date)).toBe(true)
    expect(dates.map((date) => date!.getDate())).toEqual([5, 4, 3, 2])
    const clocks = dates.map((date) => `${date!.getHours()}:${date!.getMinutes()}:${date!.getSeconds()}`)
    expect(new Set(clocks).size).toBe(4)
    expect(clocks[0]).toBe('9:6:18')
    expect(dates[0]!.getTime()).toBeLessThanOrEqual(afternoon.getTime())
  })

  it('crosses a month boundary from the visitor local date', () => {
    const oldest = syntheticEventDate('invalid-identity', SYNTHETIC_DECISION_INSTANT, local(2026, 10, 1, 12, 0, 0))
    expect(oldest?.getMonth()).toBe(8)
    expect(oldest?.getDate()).toBe(28)
  })

  it('never shows a future timestamp at local midnight or one second later', () => {
    const midnight = local(2026, 10, 5, 0, 0, 0)
    expect(syntheticEventDate('valid-crossing', SYNTHETIC_DECISION_INSTANT, midnight)?.getTime()).toBe(midnight.getTime())

    const justAfter = local(2026, 10, 5, 0, 0, 1)
    const today = syntheticEventDate('valid-crossing', SYNTHETIC_DECISION_INSTANT, justAfter)
    expect(today!.getTime()).toBeLessThanOrEqual(justAfter.getTime())
    expect(today!.getDate()).toBe(5)
    expect(today!.getHours()).toBe(0)
    expect(today!.getSeconds()).toBe(1)
  })

  it('steps off a reserved earlier clock when the preferred morning time is still ahead', () => {
    const now = local(2026, 10, 5, 8, 17, 41)
    expect(syntheticEventDate('valid-crossing', SYNTHETIC_DECISION_INSTANT, now)?.getTime()).toBe(
      local(2026, 10, 5, 8, 17, 40).getTime(),
    )
  })

  it('leaves real engine timestamps and unknown scenarios unchanged', () => {
    const real = '2026-10-05T07:01:02Z'
    expect(presentBoundaryEventTime('valid-crossing', real, afternoon)).toBe(real)
    expect(presentBoundaryEventTime('other-scenario', SYNTHETIC_DECISION_INSTANT, afternoon)).toBe(
      SYNTHETIC_DECISION_INSTANT,
    )
  })

  it('formats seeded times as a local calendar string', () => {
    const text = presentBoundaryEventTime('valid-crossing', SYNTHETIC_DECISION_INSTANT, afternoon)
    expect(text).toContain('2026')
    expect(text).not.toMatch(/T\d{2}:\d{2}:\d{2}Z/)
    expect(text).not.toMatch(/[-—–]/)
  })
})
