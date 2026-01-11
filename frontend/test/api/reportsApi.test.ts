import { createPinia } from 'pinia'
import { describe, expect, it } from 'vitest'
import { createRouter, createMemoryHistory } from 'vue-router'
import { http, HttpResponse } from 'msw'

import { initApiClient } from '@/api/client'
import { getReportActivityStats } from '@/api/reports'

import { server } from '../mswServer'

describe('reports api', () => {
  it('requests activity-stats with term query', async () => {
    server.use(
      http.get('/api/reports/activity-stats', ({ request }) => {
        const url = new URL(request.url)
        expect(url.searchParams.get('term')).toBe('2026-02-23-1')
        return HttpResponse.json({ term: '2026-02-23-1', items: [] })
      }),
    )

    const pinia = createPinia()
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/login', name: 'login', component: { template: '<div />' } }],
    })

    initApiClient(pinia, router)

    const res = await getReportActivityStats({ term: '2026-02-23-1' })
    expect(res.term).toBe('2026-02-23-1')
  })
})
