import { createPinia } from 'pinia'
import { describe, expect, it } from 'vitest'
import { createMemoryHistory } from 'vue-router'

import { createAppRouter } from '@/router'
import { useAuthStore } from '@/stores/auth'

describe('router auth guard', () => {
  it('redirects to login when unauthenticated', async () => {
    const pinia = createPinia()
    const router = createAppRouter(pinia, createMemoryHistory())

    await router.push('/')
    await router.isReady()

    expect(router.currentRoute.value.name).toBe('login')
    expect(router.currentRoute.value.query.redirect).toBe('/')
  })

  it('allows access to protected routes when authenticated', async () => {
    const pinia = createPinia()
    const auth = useAuthStore(pinia)
    auth.setTokenPair({
      accessToken: 'access',
      refreshToken: 'refresh',
      accessExpiresAt: new Date(Date.now() + 60_000).toISOString(),
      refreshExpiresAt: new Date(Date.now() + 120_000).toISOString(),
    })

    const router = createAppRouter(pinia, createMemoryHistory())

    await router.push('/')
    await router.isReady()

    expect(router.currentRoute.value.name).toBe('dashboard')
  })

  it('does not require auth for public routes', async () => {
    const pinia = createPinia()
    const router = createAppRouter(pinia, createMemoryHistory())

    await router.push('/dev/health')
    await router.isReady()

    expect(router.currentRoute.value.name).toBe('dev-health')
  })
})
