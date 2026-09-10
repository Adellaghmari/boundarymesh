import { describe, expect, it } from 'vitest'
import { normalizeMicrosoftMode } from '@/api/microsoft'

describe('normalizeMicrosoftMode', () => {
  it('does not treat LIVE_CONFIGURED as LIVE_VERIFIED', () => {
    expect(normalizeMicrosoftMode('LIVE_CONFIGURED')).toBe('LIVE_CONFIGURED')
    expect(normalizeMicrosoftMode('LIVE_VERIFIED')).toBe('LIVE_VERIFIED')
    expect(normalizeMicrosoftMode('CONTRACT_HARNESS')).toBe('CONTRACT_HARNESS')
  })
})
