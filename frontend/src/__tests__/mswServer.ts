import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'
import { allowResponse } from './fixtures'

export const handlers = [
  http.get('/api/v1/health', () =>
    HttpResponse.json({
      status: 'UP',
      service: 'boundarymesh-api',
      truth: 'SYNTHETIC DEMO / LOCAL',
    }),
  ),
  http.get('/api/v1/ready', () =>
    HttpResponse.json({
      status: 'READY',
      detail: 'Phase 1 local foundation has no downstream dependencies.',
    }),
  ),
  http.post('/api/v1/dev/evaluate', () => HttpResponse.json(allowResponse)),
]

export const server = setupServer(...handlers)
