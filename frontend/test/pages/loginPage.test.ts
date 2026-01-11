import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import userEvent from '@testing-library/user-event'
import { waitFor } from '@testing-library/vue'

import LoginPage from '@/pages/LoginPage.vue'
import { useAuthStore } from '@/stores/auth'
import { useMeStore } from '@/stores/me'

import { server } from '../mswServer'
import { renderWithProviders } from '../utils/renderWithProviders'

describe('LoginPage', () => {
  it('logs in and loads me', async () => {
    server.use(
      http.post('/api/auth/login', async ({ request }) => {
        const body = (await request.json()) as Record<string, unknown>
        expect(body.userType).toBe('STUDENT')
        expect(body.id).toBe('2020123456')
        expect(typeof body.clientSalt).toBe('string')
        expect(typeof body.clientHash).toBe('string')

        return HttpResponse.json({
          accessToken: 'access',
          refreshToken: 'refresh',
          accessExpiresAt: new Date(Date.now() + 60_000).toISOString(),
          refreshExpiresAt: new Date(Date.now() + 120_000).toISOString(),
        })
      }),
      http.get('/api/auth/me', () => {
        return HttpResponse.json({
          userId: 1,
          userType: 'STUDENT',
          id: '2020123456',
          name: 'S1',
          status: 'ACTIVE',
          collegeId: null,
          roles: [],
        })
      }),
    )

    const user = userEvent.setup()
    const { pinia, router, getByPlaceholderText, getByRole } = await renderWithProviders(LoginPage, {
      routes: [
        { path: '/login', name: 'login', component: LoginPage, meta: { public: true } },
        { path: '/', name: 'dashboard', component: { template: '<div />' }, meta: { requiresAuth: true } },
      ],
      initialPath: '/login',
    })

    await user.type(getByPlaceholderText('例如：2020123456'), '2020123456')
    await user.type(getByPlaceholderText('请输入密码'), 'pw')
    await user.click(getByRole('button', { name: '进入系统' }))

    const auth = useAuthStore(pinia)
    const me = useMeStore(pinia)

    await waitFor(() => {
      expect(auth.isAuthenticated).toBe(true)
      expect(me.me?.name).toBe('S1')
      expect(router.currentRoute.value.name).toBe('dashboard')
    })
  })
})
