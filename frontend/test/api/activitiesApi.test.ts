import { createPinia } from 'pinia'
import { describe, expect, it } from 'vitest'
import { createRouter, createMemoryHistory } from 'vue-router'
import { http, HttpResponse } from 'msw'

import { initApiClient } from '@/api/client'
import { listActivities, rejectSignup, signupActivity } from '@/api/activities'

import { server } from '../mswServer'

describe('activities api', () => {
  it('passes query params when listing activities', async () => {
    let receivedParams: URLSearchParams | null = null

    server.use(
      http.get('/api/activities', ({ request }) => {
        receivedParams = new URL(request.url).searchParams
        return HttpResponse.json({
          content: [],
          totalElements: 0,
          totalPages: 0,
          pageNumber: 1,
          pageSize: 10,
        })
      }),
    )

    const pinia = createPinia()
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/login', name: 'login', component: { template: '<div />' } }],
    })

    initApiClient(pinia, router)

    await listActivities({ page: 1, size: 10, term: '2026-02-23-1', status: 'DRAFT' })

    expect(receivedParams?.get('page')).toBe('1')
    expect(receivedParams?.get('size')).toBe('10')
    expect(receivedParams?.get('term')).toBe('2026-02-23-1')
    expect(receivedParams?.get('status')).toBe('DRAFT')
  })

  it('omits body when signing up without note', async () => {
    let receivedText: string | null = null

    server.use(
      http.post('/api/activities/1/signups', async ({ request }) => {
        receivedText = await request.text()
        return HttpResponse.json({
          id: 1,
          activityId: 1,
          userId: 1,
          status: 'PENDING',
          note: null,
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

    await signupActivity(1)

    expect(receivedText).not.toBeNull()
    expect(['', 'null']).toContain(receivedText)
  })

  it('sends reason body when rejecting signup with reason', async () => {
    let received: unknown = null

    server.use(
      http.post('/api/activities/1/signups/2/reject', async ({ request }) => {
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

    await rejectSignup(1, 2, 'no')

    expect(received).toEqual({ reason: 'no' })
  })
})
