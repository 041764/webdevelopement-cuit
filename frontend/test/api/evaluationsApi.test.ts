import { createPinia } from 'pinia'
import { describe, expect, it } from 'vitest'
import { createRouter, createMemoryHistory } from 'vue-router'
import { http, HttpResponse } from 'msw'

import { initApiClient } from '@/api/client'
import { createEvaluation } from '@/api/evaluations'

import { server } from '../mswServer'

describe('evaluations api', () => {
  it('creates evaluation with details array when provided', async () => {
    let received: unknown = null

    server.use(
      http.post('/api/evaluations', async ({ request }) => {
        received = await request.json()
        return HttpResponse.json({
          id: 1,
          evaluatorUserId: 2,
          evaluateeUserId: 3,
          term: '2026-02-23-1',
          scoreTotal: 10,
          comment: null,
          createdAt: '2026-01-10T12:00:00Z',
        })
      }),
    )

    const pinia = createPinia()
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/login', name: 'login', component: { template: '<div />' } }],
    })

    initApiClient(pinia, router)

    await createEvaluation({
      evaluateeUserId: 3,
      term: '2026-02-23-1',
      scoreTotal: 10,
      details: [{ itemKey: 'attitude', score: 5, comment: 'ok' }],
    })

    expect(received).toEqual({
      evaluateeUserId: 3,
      term: '2026-02-23-1',
      scoreTotal: 10,
      details: [{ itemKey: 'attitude', score: 5, comment: 'ok' }],
    })
  })
})
