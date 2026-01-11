import { createPinia } from 'pinia'
import { describe, expect, it } from 'vitest'
import { createRouter, createMemoryHistory } from 'vue-router'
import { http, HttpResponse } from 'msw'

import { initApiClient } from '@/api/client'
import { importUsersCsv, resetUserPassword } from '@/api/users'

import { server } from '../mswServer'

describe('users api', () => {
  it('imports CSV with query userType and multipart file', async () => {
    let queryUserType: string | null = null
    let fileName: string | null = null

    server.use(
      http.post('/api/users/import', async ({ request }) => {
        const url = new URL(request.url)
        queryUserType = url.searchParams.get('userType')
        const fd = await request.formData()
        const file = fd.get('file')
        if (file instanceof File) {
          fileName = file.name
        }
        return HttpResponse.json({ created: 1, updated: 0, failed: 0, failures: [] })
      }),
    )

    const pinia = createPinia()
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/login', name: 'login', component: { template: '<div />' } }],
    })

    initApiClient(pinia, router)

    const file = new File(['id,name,collegeName\n1,a,c\n'], 'u.csv', { type: 'text/csv' })
    const res = await importUsersCsv({ userType: 'STUDENT' }, file)

    expect(queryUserType).toBe('STUDENT')
    expect(fileName).toBe('u.csv')
    expect(res.created).toBe(1)
  })

  it('resets password with clientSalt/clientHash', async () => {
    let received: unknown = null

    server.use(
      http.post('/api/users/9/password:reset', async ({ request }) => {
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

    await resetUserPassword(9, { clientSalt: 's', clientHash: 'h' })

    expect(received).toEqual({ clientSalt: 's', clientHash: 'h' })
  })
})
