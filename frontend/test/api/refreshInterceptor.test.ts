import { createPinia } from 'pinia'
import { describe, expect, it, vi } from 'vitest'
import { createMemoryHistory } from 'vue-router'
import { http, HttpResponse } from 'msw'

import { initApiClient } from '@/api/client'
import { useAuthStore } from '@/stores/auth'
import { createAppRouter } from '@/router'

import { server } from '../mswServer'

describe('api refresh interceptor', () => {
  it('refreshes once for concurrent 401s and retries requests', async () => {
    let refreshCount = 0

    server.use(
      http.post('/api/auth/refresh', async ({ request }) => {
        refreshCount += 1
        const body = (await request.json()) as { refreshToken?: string }
        expect(body.refreshToken).toBe('refresh-old')

        return HttpResponse.json({
          accessToken: 'access-new',
          refreshToken: 'refresh-new',
          accessExpiresAt: new Date(Date.now() + 60_000).toISOString(),
          refreshExpiresAt: new Date(Date.now() + 120_000).toISOString(),
        })
      }),
      http.get('/api/protected', ({ request }) => {
        const auth = request.headers.get('authorization')
        if (auth === 'Bearer access-old' || !auth) {
          return HttpResponse.json({ code: 'AUTH_TOKEN_EXPIRED', message: 'expired' }, { status: 401 })
        }
        return HttpResponse.json({ ok: true })
      }),
    )

    const pinia = createPinia()
    const router = createAppRouter(pinia, createMemoryHistory())
    const api = initApiClient(pinia, router)

    const auth = useAuthStore(pinia)
    auth.setTokenPair({
      accessToken: 'access-old',
      refreshToken: 'refresh-old',
      accessExpiresAt: new Date(Date.now() - 1_000).toISOString(),
      refreshExpiresAt: new Date(Date.now() + 120_000).toISOString(),
    })

    const [a, b] = await Promise.all([api.get('/protected'), api.get('/protected')])

    expect(a.data).toEqual({ ok: true })
    expect(b.data).toEqual({ ok: true })
    expect(refreshCount).toBe(1)
    expect(auth.tokenPair?.accessToken).toBe('access-new')
  })

  it('clears tokens and navigates to login when refresh fails', async () => {
    const pinia = createPinia()
    const router = createAppRouter(pinia, createMemoryHistory())
    const pushSpy = vi.spyOn(router, 'push')
    const api = initApiClient(pinia, router)

    const auth = useAuthStore(pinia)
    auth.setTokenPair({
      accessToken: 'access-old',
      refreshToken: 'refresh-old',
      accessExpiresAt: new Date(Date.now() - 1_000).toISOString(),
      refreshExpiresAt: new Date(Date.now() + 120_000).toISOString(),
    })

    server.use(
      http.post('/api/auth/refresh', () => {
        return HttpResponse.json({ code: 'AUTH_TOKEN_REVOKED', message: 'revoked' }, { status: 401 })
      }),
      http.get('/api/protected', () => {
        return HttpResponse.json({ code: 'AUTH_TOKEN_EXPIRED', message: 'expired' }, { status: 401 })
      }),
    )

    await expect(api.get('/protected')).rejects.toBeTruthy()

    expect(auth.isAuthenticated).toBe(false)
    expect(pushSpy).toHaveBeenCalledWith({ name: 'login' })
  })
})
