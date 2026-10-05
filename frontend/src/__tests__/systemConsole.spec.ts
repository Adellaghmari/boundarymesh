import { describe, expect, it } from 'vitest'
import { buildSystemConsole } from '@/domain/systemConsole'
import { sessionEvent } from './fixtures'

describe('system console', () => {
  it('derives a synthetic runtime view from the selected crossing', () => {
    const model = buildSystemConsole({
      routeName: 'overview',
      events: [sessionEvent],
      selected: sessionEvent,
    })
    expect(model.runtimeMode).toBe('Synthetic demo')
    expect(model.routeLabel).toBe('Overview')
    expect(model.crossing).toBe('Valid crossing')
    expect(model.decision).toBe('ALLOW')
    expect(model.worker).toBe('Processed')
    expect(model.delivery).toBe('Completed')
    expect(model.attestation).toBe('Signed')
    expect(model.pipeline.map((stage) => stage.label)).toEqual([
      'Identity',
      'Authorization',
      'Authenticity',
      'Freshness',
      'Replay',
      'Contract',
      'Policy',
      'Transformation',
      'Attestation',
      'Delivery',
    ])
    expect(model.selectedLog.map((line) => line.message)).toContain('Identity verified')
    expect(model.log.map((line) => line.message)).toContain('Identity verified')
    expect(model.log.map((line) => line.message)).toContain('Policy result ALLOW')
    expect(model.log.map((line) => line.message)).toContain('Delivery completed')
    expect(model.stages.map((stage) => stage.label)).toEqual([
      'Authenticate',
      'Authorize',
      'Verify authenticity',
      'Check replay / freshness',
      'Validate contract',
      'Govern data',
      'Enforce boundary',
      'Resolve credential / consent',
      'Deliver or quarantine',
      'Record audit chain',
      'Sign exchange attestation',
    ])
    expect(model.groups.map((group) => group.id)).toEqual([
      'runtime',
      'events',
      'crossing',
      'policy',
      'delivery',
      'attestation',
      'route',
    ])
    expect(model.state.map((fact) => fact.value).join(' ')).not.toMatch(/[-—–]/)
    expect(model.log.map((line) => `${line.clock} ${line.message}`).join(' ')).not.toMatch(/[-—–]/)
    expect(model.stages.map((stage) => Object.values(stage).join(' ')).join(' ')).not.toMatch(/[-—–]/)
    expect(model.groups.flatMap((group) => group.fields.map((field) => `${field.key} ${field.value}`)).join(' ')).not.toMatch(/[-—–]/)
  })
})
