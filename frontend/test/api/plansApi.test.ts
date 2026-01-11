import { createPinia } from 'pinia'
import { describe, expect, it } from 'vitest'
import { createRouter, createMemoryHistory } from 'vue-router'
import { http, HttpResponse } from 'msw'

import { initApiClient } from '@/api/client'
import { addPlanItemProgress, listPlans, updatePlanItem } from '@/api/plans'

import { server } from '../mswServer'

describe('plans api', () => {
  it('passes query params when listing plans', async () => {
    let receivedParams: URLSearchParams | null = null

    server.use(
      http.get('/api/plans', ({ request }) => {
        receivedParams = new URL(request.url).searchParams
        return HttpResponse.json({
          content: [],
          totalElements: 0,
          totalPages: 0,
          pageNumber: 1,
          pageSize: 20,
        })
      }),
    )

    const pinia = createPinia()
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/login', name: 'login', component: { template: '<div />' } }],
    })

    initApiClient(pinia, router)

    await listPlans({ page: 1, size: 20, term: '2026-02-23-1' })

    expect(receivedParams?.get('page')).toBe('1')
    expect(receivedParams?.get('size')).toBe('20')
    expect(receivedParams?.get('term')).toBe('2026-02-23-1')
  })

  it('patches title and dueDate', async () => {
    let received: unknown = null

    server.use(
      http.patch('/api/plans/1/items/10', async ({ request }) => {
        received = await request.json()
        return new HttpResponse(null, { status: 204 })
      }),
    )

    const pinia = createPinia()
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/login', name: 'login', component: { template: '<div />' } }],
    })

    initApiClient(pinia, router)

    await updatePlanItem(1, 10, { title: 'T', dueDate: '2026-01-01' })

    expect(received).toEqual({ title: 'T', dueDate: '2026-01-01' })
  })

  it('posts progress body when adding item progress', async () => {
    let received: unknown = null

    server.use(
      http.post('/api/plan-items/10/progress', async ({ request }) => {
        received = await request.json()
        return HttpResponse.json({
          id: 1,
          itemId: 10,
          progress: 40,
          note: 'n',
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

    await addPlanItemProgress(10, { progress: 40, note: 'n' })

    expect(received).toEqual({ progress: 40, note: 'n' })
  })
})
