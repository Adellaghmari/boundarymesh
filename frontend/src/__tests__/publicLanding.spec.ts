import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'
import { ROUTE_NAMES, routes } from '@/router'

describe('hosted public documents', () => {
  it('keeps the local lab router intact and serves the explorer at the cloud root', () => {
    expect(routes.map((route) => route.name)).toEqual([
      ROUTE_NAMES.overview,
      ROUTE_NAMES.boundaryEvents,
      ROUTE_NAMES.boundaryEventDetail,
      ROUTE_NAMES.connections,
      ROUTE_NAMES.architecture,
      ROUTE_NAMES.identity,
      ROUTE_NAMES.webhooks,
      ROUTE_NAMES.contracts,
      ROUTE_NAMES.dataBoundary,
      ROUTE_NAMES.crossing,
      ROUTE_NAMES.delivery,
      ROUTE_NAMES.credentials,
      ROUTE_NAMES.quarantine,
      ROUTE_NAMES.quarantineDetail,
      ROUTE_NAMES.audit,
      ROUTE_NAMES.auditDetail,
      ROUTE_NAMES.attestations,
      ROUTE_NAMES.attestationDetail,
      ROUTE_NAMES.boundaryGraph,
      ROUTE_NAMES.status,
      ROUTE_NAMES.about,
      ROUTE_NAMES.notFound,
    ])

    const main = readFileSync(resolve('src/main.ts'), 'utf8')
    const operator = readFileSync(resolve('src/operator-main.ts'), 'utf8')
    const firebase = JSON.parse(readFileSync(resolve('../firebase.json'), 'utf8'))

    expect(main).toContain("import.meta.env.VITE_APP_MODE === 'cloud'")
    expect(main).toContain('seedPublicDemo')
    expect(main).toContain('App.vue')
    expect(main).not.toContain('PublicExplorerView')
    expect(main).not.toContain('PublicLandingView')
    expect(main).toContain('selectPublicEntry')
    expect(main).toContain('App.vue')
    expect(main).not.toContain('CloudConsoleView')
    expect(operator).toContain('CloudConsoleView')
    expect(operator).not.toContain('App.vue')
    expect(operator).not.toContain('PublicExplorerView')
    expect(firebase.hosting.redirects).toEqual([
      { source: '/explore', destination: '/', type: 301 },
      { source: '/explore/**', destination: '/', type: 301 },
    ])
    expect(firebase.hosting.rewrites.map((rule: { source: string }) => rule.source)).toEqual([
      '/session/**',
      '/api/**',
      '/operator',
      '/operator/**',
      '**',
    ])
    expect(firebase.hosting.rewrites[0].run.serviceId).toBe('boundarymesh-web')
    expect(firebase.hosting.rewrites[1].run.serviceId).toBe('boundarymesh-web')
    expect(firebase.hosting.rewrites[2].destination).toBe('/operator.html')
    expect(firebase.hosting.rewrites.at(-1).destination).toBe('/index.html')
  })
})
